package bo.firmadigital.jacobitus.pesaje.sync;

/**
 * Resultado de una vuelta de sincronizacion.
 */
public final class ResultadoSync {

    private int enviados;
    private int conflictos;
    private int errores;
    private int descargados;
    private String mensaje = "";

    public int getEnviados() {
        return enviados;
    }

    void sumarEnviado() {
        enviados++;
    }

    public int getConflictos() {
        return conflictos;
    }

    void sumarConflicto() {
        conflictos++;
    }

    public int getErrores() {
        return errores;
    }

    void sumarError() {
        errores++;
    }

    public int getDescargados() {
        return descargados;
    }

    void sumarDescargado() {
        descargados++;
    }

    public String getMensaje() {
        return mensaje;
    }

    void setMensaje(String mensaje) {
        this.mensaje = mensaje == null ? "" : mensaje;
    }

    /**
     * @return true si no quedo nada pendiente ni con error
     */
    public boolean limpio() {
        return errores == 0;
    }

    @Override
    public String toString() {
        return "enviados=" + enviados + ", conflictos=" + conflictos + ", errores=" + errores
                + ", descargados=" + descargados
                + (mensaje.isEmpty() ? "" : " (" + mensaje + ")");
    }
}
