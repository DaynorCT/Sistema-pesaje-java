package bo.firmadigital.jacobitus.pesaje.balanza;

/**
 * Fuente de peso.
 *
 * Define el contrato minimo que necesita el sistema para obtener un peso:
 * conectar, desconectar, leer la trama cruda y consultar el estado de la
 * conexion. La lectura real por puerto serie la implementa
 * {@link LectorSerie}; las pruebas pueden inyectar otra implementacion
 * (por ejemplo {@link FuentePesoSimulada}).
 */
public interface LectorPeso {

    /**
     * Abre la conexion con la bascula.
     *
     * @return true si la conexion quedo establecida
     */
    boolean conectar();

    /** Cierra la conexion con la bascula. No debe fallar si no habia conexion. */
    void desconectar();

    /**
     * Lee la proxima trama cruda de la bascula.
     *
     * @return la trama leida o una cadena vacia si no hay datos disponibles
     */
    String leerTrama();

    /** @return true si hay conexion activa con la bascula. */
    boolean estaConectado();

    /**
     * Ultimo error registrado por el lector.
     *
     * @return la descripcion del error o una cadena vacia si no hubo errores
     */
    default String ultimoError() {
        return "";
    }
}
