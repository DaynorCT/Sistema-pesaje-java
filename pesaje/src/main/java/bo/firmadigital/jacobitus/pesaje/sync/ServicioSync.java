package bo.firmadigital.jacobitus.pesaje.sync;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import bo.firmadigital.jacobitus.pesaje.config.ConfigSistema;
import bo.firmadigital.jacobitus.pesaje.modelo.Pesaje;

/**
 * Servicio de sincronizacion de la aplicacion.
 *
 * Owns el motor, encola los pesajes para enviar y dispara la vuelta
 * periodica cuando la configuracion pide envio automatico. Si el servidor
 * no esta configurado, el servicio queda disponible pero inactivo: el
 * escritorio trabaja igual contra su base local.
 */
public final class ServicioSync implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(ServicioSync.class.getName());

    /** Minutos entre vueltas automaticas de sincronizacion. */
    public static final int INTERVALO_AUTOMATICO_MINUTOS = 2;

    private final MotorSync motor;
    private final RepositorioOutbox outbox;
    private ScheduledExecutorService planificador;
    private volatile ResultadoSync ultimoResultado;
    private volatile String ultimoMensaje = "";

    public ServicioSync(MotorSync motor, RepositorioOutbox outbox) {
        this.motor = motor;
        this.outbox = outbox;
    }

    /**
     * Crea el servicio con la configuracion del sistema y las credenciales
     * del usuario.
     *
     * @param config    configuracion del sistema
     * @param correo    correo del usuario para la web
     * @param contrasena contrasena del usuario para la web
     * @return servicio de sincronizacion
     */
    public static ServicioSync crear(ConfigSistema config, String correo, String contrasena) {
        Transporte transporte = new TransporteHttp(config.getTimeoutHttp());
        SesionSync sesion = new SesionSync(transporte, config.getUrl(), correo, contrasena);
        RepositorioOutbox outbox = new RepositorioOutbox();
        AplicadorDelta aplicador = new AplicadorDelta();
        MotorSync motor = new MotorSync(transporte, sesion, outbox, aplicador);
        return new ServicioSync(motor, outbox);
    }

    /**
     * Encola un pesaje para enviarlo a la web.
     *
     * <p>El identificador del evento se genera aqui y se guarda en el
     * pesaje: es la clave de idempotencia que impide que un reenvio
     * duplique el pesaje en el servidor.
     *
     * @param pesaje pesaje recien registrado
     */
    public void encolarPesaje(Pesaje pesaje) {
        if (pesaje == null || pesaje.getId() == null) {
            return;
        }
        String clientEventId = pesaje.getSyncEventId();
        if (clientEventId == null || clientEventId.isEmpty()) {
            clientEventId = UUID.randomUUID().toString();
            pesaje.setSyncEventId(clientEventId);
        }
        String payload = MotorSync.payloadPesaje(clientEventId, pesaje);
        outbox.encolar(clientEventId, "pesajes", pesaje.getId(), payload);
    }

    /**
     * Encola un evento de catalogo para enviarlo a la web.
     *
     * @param recurso   recurso del servidor
     * @param entidadId id de la fila local
     * @param payload   cuerpo JSON del evento
     */
    public void encolar(String recurso, Integer entidadId, String payload) {
        outbox.encolar(UUID.randomUUID().toString(), recurso, entidadId, payload);
    }

    /**
     * Ejecuta una vuelta de sincronizacion.
     *
     * @return resultado de la vuelta
     */
    /**
     * Envía el peso capturado en la báscula a la web (card en vivo).
     *
     * @param peso peso en kilogramos
     * @return true si el servidor lo aceptó
     */
    public boolean enviarPesoVivo(int peso) {
        try {
            boolean ok = motor.enviarPesoVivo(peso);
            if (!ok) {
                ultimoMensaje = "No se pudo publicar el peso en vivo en la web.";
            }
            return ok;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Error al enviar peso en vivo", e);
            ultimoMensaje = "Error al enviar peso en vivo: " + e.getMessage();
            return false;
        }
    }

    public ResultadoSync sincronizarAhora() {
        try {
            ResultadoSync resultado = motor.sincronizar();
            ultimoResultado = resultado;
            ultimoMensaje = resultado.getMensaje();
            return resultado;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Error en la sincronizacion", e);
            ultimoMensaje = "Error en la sincronizacion: " + e.getMessage();
            return new ResultadoSync();
        }
    }

    /**
     * Empieza la sincronizacion periodica.
     *
     * @param minutos cada cuantos minutos se sincroniza
     */
    public void iniciarAutomatico(int minutos) {
        detenerAutomatico();
        int espera = Math.max(minutos, 1);
        planificador = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "pesaje-sync");
            hilo.setDaemon(true);
            return hilo;
        });
        planificador.scheduleWithFixedDelay(this::sincronizarAhora, 30, espera, TimeUnit.MINUTES);
    }

    /**
     * Detiene la sincronizacion periodica.
     */
    public void detenerAutomatico() {
        if (planificador != null) {
            planificador.shutdownNow();
            planificador = null;
        }
    }

    /**
     * @return true si la sincronizacion periodica esta activa
     */
    public boolean automaticoActivo() {
        return planificador != null;
    }

    /**
     * @return numero de eventos esperando salir
     */
    public long pendientes() {
        return outbox.pendientesCount();
    }

    /**
     * @return numero de eventos con error permanente
     */
    public long errores() {
        return outbox.erroresCount();
    }

    /**
     * @return ultimo resultado de sincronizacion, o null si nunca corrio
     */
    public ResultadoSync getUltimoResultado() {
        return ultimoResultado;
    }

    /**
     * @return ultimo mensaje de estado de la sincronizacion
     */
    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    /**
     * @return el motor de sincronizacion
     */
    public MotorSync getMotor() {
        return motor;
    }

    @Override
    public void close() {
        detenerAutomatico();
    }
}
