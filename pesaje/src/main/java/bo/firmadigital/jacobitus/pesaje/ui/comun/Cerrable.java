package bo.firmadigital.jacobitus.pesaje.ui.comun;

/**
 * Panel que necesita liberar recursos cuando se deja de mostrar.
 *
 * El shell la invoca al navegar a otro modulo: asi un panel que tenga
 * una conexion abierta (por ejemplo la balanza) la cierra y no deja
 * hilos ni puertos series abiertos.
 */
public interface Cerrable {

    /**
     * Libera los recursos del panel. Debe ser idempotente.
     */
    void cerrar();
}
