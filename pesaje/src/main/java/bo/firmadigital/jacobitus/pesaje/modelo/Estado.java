package bo.firmadigital.jacobitus.pesaje.modelo;

/**
 * Estado del registro en la base de datos.
 *
 * El sistema original usa borrado logico: las filas no se eliminan, se
 * marcan como INACTIVO.
 */
public enum Estado {

    ACTIVO,
    INACTIVO;

    public static Estado desdeTexto(String texto) {
        if (texto == null) {
            return ACTIVO;
        }
        String normalizado = texto.trim().toUpperCase();
        for (Estado estado : values()) {
            if (estado.name().equals(normalizado)) {
                return estado;
            }
        }
        return ACTIVO;
    }
}
