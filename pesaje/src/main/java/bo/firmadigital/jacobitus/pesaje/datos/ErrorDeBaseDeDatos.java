package bo.firmadigital.jacobitus.pesaje.datos;

/**
 * Error de la capa de datos del Sistema de Pesaje.
 *
 * Envuelve las excepciones de JDBC para que el resto de la aplicacion no
 * dependa del driver SQLite y para que los fallos de restriccion (claves
 * foraneas, unicidad o dominio) se distingan de los fallos de
 * infraestructura (archivo, permisos, conexion).
 */
public class ErrorDeBaseDeDatos extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErrorDeBaseDeDatos(String mensaje) {
        super(mensaje);
    }

    public ErrorDeBaseDeDatos(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
