package bo.firmadigital.jacobitus.pesaje.sync;

import java.util.List;

import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.datos.Fechas;
import bo.firmadigital.jacobitus.pesaje.datos.Sql;

/**
 * Cola local de salida hacia la web.
 *
 * Es durable a proposito: si el escritorio no tiene senal, el evento
 * queda en la base y se reintenta mas tarde. El `client_event_id` es la
 * clave de idempotencia del servidor: reenviar el mismo evento nunca
 * duplica el pesaje.
 */
public final class RepositorioOutbox {

    /** Estados posibles de un evento de la cola. */
    public static final String PENDIENTE = "PENDIENTE";
    /** Estado de un evento que el servidor acepto. */
    public static final String ENVIADO = "ENVIADO";
    /** Estado de un evento que el servidor rechazo de forma permanente. */
    public static final String ERROR = "ERROR";

    /** Cantidad maxima de eventos que se procesan en una vuelta. */
    public static final int LIMITE_POR_VUELTA = 50;

    private final Sql sql = Database.obtenerInstancia().sql();

    /**
     * Encola un evento para enviarlo a la web.
     *
     * @param clientEventId identificador unico del evento
     * @param recurso       recurso del servidor (por ejemplo, "pesajes")
     * @param entidadId     id de la fila local que origino el evento
     * @param payload       cuerpo JSON del evento
     */
    public void encolar(String clientEventId, String recurso, Integer entidadId, String payload) {
        String ahora = Fechas.ahora();
        sql.insertar(
                "INSERT INTO sync_outbox (client_event_id, recurso, entidad_id, payload, estado,"
                        + " intentos, creado_en, actualizado_en) VALUES (?, ?, ?, ?, ?, 0, ?, ?)",
                clientEventId, recurso, entidadId, payload, PENDIENTE, ahora, ahora);
    }

    /**
     * Eventos que corresponde enviar en esta vuelta: los pendientes cuyo
     * proximo intento ya vencio.
     *
     * @return eventos ordenados por antiguedad
     */
    public List<EventoOutbox> pendientes() {
        String ahora = Fechas.ahora();
        return sql.consultar(
                "SELECT client_event_id, recurso, entidad_id, payload, intentos FROM sync_outbox"
                        + " WHERE estado = ? AND (proximo_intento IS NULL OR proximo_intento <= ?)"
                        + " ORDER BY id ASC LIMIT ?",
                fila -> new EventoOutbox(
                        fila.getString("client_event_id"),
                        fila.getString("recurso"),
                        Sql.entero(fila, "entidad_id"),
                        fila.getString("payload"),
                        fila.getInt("intentos")),
                PENDIENTE, ahora, LIMITE_POR_VUELTA);
    }

    /**
     * Marca un evento como enviado por el servidor.
     *
     * @param clientEventId identificador del evento
     * @param webId         id que le asigno el servidor, si lo devolvio
     */
    public void marcarEnviado(String clientEventId, Integer webId) {
        sql.actualizar(
                "UPDATE sync_outbox SET estado = ?, web_id = ?, ultimo_error = NULL,"
                        + " proximo_intento = NULL, actualizado_en = ? WHERE client_event_id = ?",
                ENVIADO, webId, Fechas.ahora(), clientEventId);
    }

    /**
     * Marca un evento con error.
     *
     * @param clientEventId identificador del evento
     * @param mensaje       descripcion del error
     * @param permanente    true si no tiene sentido reintentar solo
     * @param intentos      numero de intentos hechos
     * @param proximoIntento momento del proximo intento, o null si es permanente
     */
    public void marcarError(String clientEventId, String mensaje, boolean permanente, int intentos,
            String proximoIntento) {
        String ahora = Fechas.ahora();
        if (permanente) {
            sql.actualizar(
                    "UPDATE sync_outbox SET estado = ?, intentos = ?, ultimo_error = ?,"
                            + " proximo_intento = NULL, actualizado_en = ? WHERE client_event_id = ?",
                    ERROR, intentos, recortar(mensaje), ahora, clientEventId);
            return;
        }
        sql.actualizar(
                "UPDATE sync_outbox SET intentos = ?, ultimo_error = ?, proximo_intento = ?,"
                        + " actualizado_en = ? WHERE client_event_id = ?",
                intentos, recortar(mensaje), proximoIntento, ahora, clientEventId);
    }

    /**
     * Cantidad de eventos que siguen pendientes.
     *
     * @return numero de eventos pendientes
     */
    public long pendientesCount() {
        return sql.contar("SELECT COUNT(*) FROM sync_outbox WHERE estado = ?", PENDIENTE);
    }

    /**
     * Cantidad de eventos que quedaron con error permanente.
     *
     * @return numero de eventos con error
     */
    public long erroresCount() {
        return sql.contar("SELECT COUNT(*) FROM sync_outbox WHERE estado = ?", ERROR);
    }

    /**
     * Ultimo error registrado en la cola.
     *
     * @return mensaje del ultimo error, o cadena vacia
     */
    public String ultimoError() {
        List<String> errores = sql.consultar(
                "SELECT ultimo_error FROM sync_outbox WHERE ultimo_error IS NOT NULL"
                        + " ORDER BY id DESC LIMIT 1",
                fila -> fila.getString("ultimo_error"));
        return errores.isEmpty() ? "" : errores.get(0);
    }

    private static String recortar(String mensaje) {
        if (mensaje == null) {
            return "";
        }
        return mensaje.length() > 190 ? mensaje.substring(0, 190) : mensaje;
    }
}
