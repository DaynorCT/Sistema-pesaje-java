package bo.firmadigital.jacobitus.pesaje.config;

/**
 * Modo de operacion del sistema de pesaje.
 *
 * INTEGRADO: la pantalla queda reducida a capturar el peso y enviarlo al
 * sistema web; el resto de los datos se completan del lado del servidor.
 * AUTONOMO: el sistema completo (dashboard, catalogos, reportes) queda
 * disponible en el escritorio.
 */
public enum ModoOperacion {
    INTEGRADO,
    AUTONOMO;

    public static ModoOperacion desdeTexto(String texto) {
        if (texto == null) {
            return INTEGRADO;
        }
        String normalizado = texto.trim().toUpperCase();
        for (ModoOperacion modo : values()) {
            if (modo.name().equals(normalizado)) {
                return modo;
            }
        }
        return INTEGRADO;
    }
}
