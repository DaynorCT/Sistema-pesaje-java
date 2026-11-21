package bo.firmadigital.jacobitus.pesaje.sync;

/**
 * Respuesta de una peticion HTTP al servidor de sincronizacion.
 *
 * El codigo 0 significa que no hubo respuesta: fallo de red. Un codigo
 * 4xx o 5xx es una respuesta del servidor.
 */
public final class RespuestaHttp {

    private final int codigo;
    private final String cuerpo;
    private final String error;

    public RespuestaHttp(int codigo, String cuerpo, String error) {
        this.codigo = codigo;
        this.cuerpo = cuerpo == null ? "" : cuerpo;
        this.error = error == null ? "" : error;
    }

    /**
     * @param codigo codigo HTTP
     * @param cuerpo cuerpo de la respuesta
     * @return respuesta de exito
     */
    public static RespuestaHttp deCodigo(int codigo, String cuerpo) {
        return new RespuestaHttp(codigo, cuerpo, "");
    }

    /**
     * @param error descripcion del fallo de red
     * @return respuesta sin conexion
     */
    public static RespuestaHttp deFallo(String error) {
        return new RespuestaHttp(0, "", error);
    }

    public int getCodigo() {
        return codigo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public String getError() {
        return error;
    }

    /** @return true si el servidor acepto la peticion. */
    public boolean esExito() {
        return codigo >= 200 && codigo < 300;
    }

    /** @return true si no hubo respuesta del servidor. */
    public boolean esFalloDeRed() {
        return codigo == 0;
    }

    @Override
    public String toString() {
        return "HTTP " + codigo + (error.isEmpty() ? "" : " (" + error + ")");
    }
}
