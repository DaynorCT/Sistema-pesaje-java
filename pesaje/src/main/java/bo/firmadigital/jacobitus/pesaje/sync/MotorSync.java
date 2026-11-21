package bo.firmadigital.jacobitus.pesaje.sync;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonNode;

/**
 * Motor de sincronizacion con la web.
 *
 * Hace las dos direcciones:
 * <ul>
 *   <li><b>Push</b>: manda los eventos de la cola local a
 *       {@code POST /api/sync/:recurso}. El {@code client_event_id} hace
 *       que reenviar el mismo evento nunca duplique el pesaje.</li>
 *   <li><b>Pull</b>: baja lo que cambio en la web desde el ultimo cursor
 *       ({@code GET /api/sync/:recurso?since=}) y lo aplica en la base
 *       local.</li>
 * </ul>
 *
 * Los errores no se pierden: los reintentables quedan con un proximo
 * intento mas adelante (30 s, 1 min, 2 min... hasta 5 min) y los
 * permanentes quedan para que el operador los revise.
 */
public final class MotorSync {

    private static final Logger LOGGER = Logger.getLogger(MotorSync.class.getName());

    /** Recursos de catalogo que se descargan de la web. */
    public static final String[] RECURSOS_CATALOGO = {
            "vehiculos", "productos", "origenes", "destinos", "choferes", "proveedores"};

    private final Transporte transporte;
    private final SesionSync sesion;
    private final RepositorioOutbox outbox;
    private final AplicadorDelta aplicador;

    public MotorSync(Transporte transporte, SesionSync sesion, RepositorioOutbox outbox,
            AplicadorDelta aplicador) {
        this.transporte = transporte;
        this.sesion = sesion;
        this.outbox = outbox;
        this.aplicador = aplicador;
    }

    /**
     * Ejecuta una vuelta completa: manda lo pendiente y baja los cambios.
     *
     * @return resultado de la vuelta
     */
    public ResultadoSync sincronizar() {
        ResultadoSync resultado = new ResultadoSync();
        if (!sesion.asegurarToken()) {
            resultado.setMensaje(sesion.getUltimoError());
            return resultado;
        }
        procesarOutbox(resultado);
        descargarCatalogos(resultado);
        descargarPesajes(resultado);
        return resultado;
    }

    /**
     * Publica el peso capturado para la card de la web.
     *
     * @param peso peso en kilogramos
     * @return true si el servidor aceptó el valor
     */
    public boolean enviarPesoVivo(int peso) {
        if (peso < 0) {
            return false;
        }
        if (!sesion.asegurarToken()) {
            return false;
        }
        Map<String, Object> cuerpo = Json.mapa();
        cuerpo.put("peso", peso);
        RespuestaHttp respuesta = transporte.enviar("POST",
                sesion.getUrlBase() + "/api/pesajes/ultimo-peso",
                Json.escribir(cuerpo), sesion.cabeceras());
        if (respuesta.getCodigo() == 401) {
            sesion.invalidar();
            if (sesion.asegurarToken()) {
                respuesta = transporte.enviar("POST",
                        sesion.getUrlBase() + "/api/pesajes/ultimo-peso",
                        Json.escribir(cuerpo), sesion.cabeceras());
            }
        }
        return respuesta.esExito();
    }

    /**
     * Envia los eventos pendientes de la cola local.
     *
     * @param resultado resultado que se va llenando
     */
    public void procesarOutbox(ResultadoSync resultado) {
        for (EventoOutbox evento : outbox.pendientes()) {
            try {
                enviarEvento(evento, resultado);
            } catch (RuntimeException e) {
                // Un evento que revienta no puede impedir que salgan los
                // demas: se marca con error y se sigue con la cola.
                outbox.marcarError(evento.getClientEventId(),
                        "Error inesperado: " + e.getMessage(), false,
                        evento.getIntentos() + 1,
                        SyncPolicy.calcularProximoIntento(evento.getIntentos() + 1,
                                LocalDateTime.now(), 0));
                resultado.sumarError();
                resultado.setMensaje("Un evento quedo con error y se reintentara mas tarde.");
            }
        }
    }

    private void enviarEvento(EventoOutbox evento, ResultadoSync resultado) {
        {
            // Los pesajes van a /api/pesajes, que es el endpoint con
            // idempotencia por client_event_id; los catalogos van al
            // espejo /api/sync/:recurso, que resuelve la version (LWW).
            String ruta = "pesajes".equals(evento.getRecurso())
                    ? "/api/pesajes"
                    : "/api/sync/" + evento.getRecurso();
            RespuestaHttp respuesta = transporte.enviar("POST", sesionUrl() + ruta,
                    evento.getPayload(), sesion.cabeceras());

            if (respuesta.getCodigo() == 401) {
                sesion.invalidar();
                if (sesion.asegurarToken()) {
                    respuesta = transporte.enviar("POST", sesionUrl() + ruta,
                            evento.getPayload(), sesion.cabeceras());
                }
            }

            if (respuesta.esExito()) {
                aplicarConfirmacion(evento, respuesta);
                outbox.marcarEnviado(evento.getClientEventId(), webIdDe(respuesta));
                resultado.sumarEnviado();
                return;
            }

            if (SyncPolicy.esErrorPermanente(respuesta.getCodigo())) {
                outbox.marcarError(evento.getClientEventId(),
                        "HTTP " + respuesta.getCodigo() + " " + respuesta.getCuerpo(),
                        true, evento.getIntentos() + 1, null);
                resultado.sumarError();
                resultado.setMensaje("Hay eventos que el servidor rechazo y necesitan revision.");
                return;
            }

            int intentos = evento.getIntentos() + 1;
            String proximo = SyncPolicy.calcularProximoIntento(intentos, LocalDateTime.now(), 0);
            outbox.marcarError(evento.getClientEventId(),
                    "HTTP " + respuesta.getCodigo() + " " + respuesta.getCuerpo(),
                    false, intentos, proximo);
            resultado.sumarError();
            resultado.setMensaje("No se pudo enviar un evento; se reintentara mas tarde.");

            if (respuesta.esFalloDeRed()) {
                // Sin red no tiene sentido seguir con el resto de la cola:
                // cada evento se comeria el timeout entero.
                resultado.setMensaje("Sin conexion con la web. La cola sigue esperando.");
                return;
            }
        }
    }

    /**
     * Baja los catalogos que cambiaron en la web y los aplica en local.
     *
     * @param resultado resultado que se va llenando
     */
    public void descargarCatalogos(ResultadoSync resultado) {
        for (String recurso : RECURSOS_CATALOGO) {
            descargar(recurso, resultado, false);
        }
    }

    /**
     * Baja los pesajes que cambiaron en la web.
     *
     * @param resultado resultado que se va llenando
     */
    public void descargarPesajes(ResultadoSync resultado) {
        descargar("pesajes", resultado, true);
    }

    private void descargar(String recurso, ResultadoSync resultado, boolean esPesaje) {
        String desde = aplicador.leerCursor(recurso);
        String ruta = "/api/sync/" + recurso + "?since=" + desde;
        RespuestaHttp respuesta = transporte.enviar("GET", sesionUrl() + ruta, null,
                sesion.cabeceras());

        if (respuesta.getCodigo() == 401) {
            sesion.invalidar();
            if (sesion.asegurarToken()) {
                respuesta = transporte.enviar("GET", sesionUrl() + ruta, null, sesion.cabeceras());
            }
        }
        if (!respuesta.esExito()) {
            resultado.setMensaje("No se pudo descargar " + recurso + " (" + respuesta + ").");
            return;
        }

        JsonNode raiz = Json.leer(respuesta.getCuerpo());
        if (raiz == null) {
            return;
        }
        JsonNode datos = raiz.get("data");
        if (datos != null && datos.isArray()) {
            for (JsonNode fila : datos) {
                try {
                    boolean aplicado = esPesaje
                            ? aplicador.aplicarPesaje(fila)
                            : aplicador.aplicarCatalogo(recurso, fila);
                    if (aplicado) {
                        resultado.sumarDescargado();
                    }
                } catch (RuntimeException e) {
                    // Una fila que no se puede aplicar no puede cortar la
                    // vuelta entera: el resto de los cambios tambien
                    // tienen que llegar.
                    resultado.sumarError();
                    resultado.setMensaje("Una fila de " + recurso + " no se pudo aplicar: "
                            + e.getMessage());
                }
            }
        }
        String cursor = Json.texto(raiz, "cursor");
        if (cursor != null && !cursor.isEmpty()) {
            aplicador.guardarCursor(recurso, cursor);
        }
    }

    /**
     * Tras un push aceptado, el servidor devuelve la fila que quedo: si es
     * un pesaje, se adoptan la guia y el id que le asigno el servidor.
     *
     * @param evento    evento enviado
     * @param respuesta respuesta del servidor
     */
    private void aplicarConfirmacion(EventoOutbox evento, RespuestaHttp respuesta) {
        try {
            confirmar(evento, respuesta);
        } catch (RuntimeException e) {
            // El evento ya fue aceptado por el servidor: un problema al
            // leer la confirmacion no lo deja sin marcar.
            LOGGER.log(Level.WARNING, "No se pudo leer la confirmacion de {0}", evento.getClientEventId());
        }
    }

    private void confirmar(EventoOutbox evento, RespuestaHttp respuesta) {
        JsonNode raiz = Json.leer(respuesta.getCuerpo());
        if (raiz == null) {
            return;
        }
        JsonNode datos = raiz.get("data");
        if (datos == null || !datos.isObject()) {
            // /api/pesajes responde con la fila en la raiz; /api/sync/:recurso
            // la envuelve en "data".
            datos = raiz;
        }
        if (!"pesajes".equals(evento.getRecurso())) {
            return;
        }
        Integer id = Json.entero(datos, "id");
        String guia = Json.texto(datos, "guia");
        if (evento.getEntidadId() == null) {
            return;
        }
        if (id != null) {
            aplicador.actualizarPesajeSincronizado(evento.getEntidadId(), id, guia);
        }
    }

    /**
     * URL base del servidor.
     *
     * @return URL del servidor
     */
    public String sesionUrl() {
        return sesion.getUrlBase();
    }

    /**
     * Cuenta los eventos que siguen esperando.
     *
     * @return numero de eventos pendientes
     */
    public long pendientes() {
        return outbox.pendientesCount();
    }

    /**
     * Cuenta los eventos con error permanente.
     *
     * @return numero de eventos con error
     */
    public long errores() {
        return outbox.erroresCount();
    }

    /**
     * Encola un evento para enviarlo a la web.
     *
     * @param clientEventId identificador unico del evento
     * @param recurso       recurso del servidor
     * @param entidadId     id de la fila local
     * @param payload       cuerpo JSON del evento
     */
    public void encolar(String clientEventId, String recurso, Integer entidadId, String payload) {
        outbox.encolar(clientEventId, recurso, entidadId, payload);
    }

    /**
     * Payload de un pesaje listo para enviar.
     *
     * <p>La guia no se envia: la asigna el servidor desde su propia
     * secuencia. Si cada lado generara guias por su cuenta, dos pesajes
     * distintos podrian compartir guia y el servidor descartaria uno en
     * silencio.
     *
     * @param clientEventId identificador unico del evento
     * @param pesaje        pesaje a enviar
     * @return cuerpo JSON
     */
    public static String payloadPesaje(String clientEventId,
            bo.firmadigital.jacobitus.pesaje.modelo.Pesaje pesaje) {
        Map<String, Object> cuerpo = Json.mapa();
        cuerpo.put("client_event_id", clientEventId);
        cuerpo.put("vehiculo_id", pesaje.getVehiculoId());
        cuerpo.put("chofer_id", referencia(pesaje.getChoferId()));
        cuerpo.put("proveedor_id", referencia(pesaje.getProveedorId()));
        cuerpo.put("producto_id", referencia(pesaje.getProductoId()));
        cuerpo.put("id_origen", referencia(pesaje.getOrigenId()));
        cuerpo.put("id_destino", referencia(pesaje.getDestinoId()));
        cuerpo.put("peso_bruto", pesaje.getPesoBruto() != null ? pesaje.getPesoBruto() : 0);
        cuerpo.put("tara", pesaje.getTara() != null ? pesaje.getTara() : 0);
        cuerpo.put("costo_bs", pesaje.getCostoBs() != null ? pesaje.getCostoBs() : 0);
        cuerpo.put("flete_bs_pendiente",
                pesaje.getFleteBsPendiente() != null ? pesaje.getFleteBsPendiente() : 0);
        cuerpo.put("estado_balanza", pesaje.getEstadoBalanza().name());
        cuerpo.put("fecha_creacion", pesaje.getFechaCreacion());
        return Json.escribir(cuerpo);
    }

    /**
     * Una referencia que no existe se manda como null, no como 0: el
     * servidor tiene llaves foraneas activas y 0 no es un id valido.
     *
     * @param id id de referencia
     * @return el id o null
     */
    private static Object referencia(Integer id) {
        return id != null && id > 0 ? id : null;
    }

    private Integer webIdDe(RespuestaHttp respuesta) {
        JsonNode raiz = Json.leer(respuesta.getCuerpo());
        if (raiz == null) {
            return null;
        }
        JsonNode datos = raiz.get("data");
        if (datos != null && datos.isObject()) {
            return Json.entero(datos, "id");
        }
        return Json.entero(raiz, "id");
    }

    /**
     * Lista de los recursos que se descargan, para mostrar en la interfaz.
     *
     * @return nombres de los recursos de catalogo
     */
    public static List<String> recursosCatalogo() {
        return java.util.Arrays.asList(RECURSOS_CATALOGO);
    }
}
