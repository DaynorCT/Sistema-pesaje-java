package bo.firmadigital.jacobitus.pesaje.sync;

/**
 * Evento pendiente de la cola local de salida.
 */
public final class EventoOutbox {

    private final String clientEventId;
    private final String recurso;
    private final Integer entidadId;
    private final String payload;
    private final int intentos;

    public EventoOutbox(String clientEventId, String recurso, Integer entidadId, String payload,
            int intentos) {
        this.clientEventId = clientEventId;
        this.recurso = recurso;
        this.entidadId = entidadId;
        this.payload = payload;
        this.intentos = intentos;
    }

    public String getClientEventId() {
        return clientEventId;
    }

    public String getRecurso() {
        return recurso;
    }

    public Integer getEntidadId() {
        return entidadId;
    }

    public String getPayload() {
        return payload;
    }

    public int getIntentos() {
        return intentos;
    }
}
