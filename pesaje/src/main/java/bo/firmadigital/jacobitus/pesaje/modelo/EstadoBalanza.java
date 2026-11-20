package bo.firmadigital.jacobitus.pesaje.modelo;

/**
 * Estado del pesaje frente a la bascula.
 *
 * EN_PROCESO: se esta pesando, todavia se puede completar o anular.
 * FINALIZADO: el pesaje quedo cerrado y se puede reportar.
 */
public enum EstadoBalanza {

    EN_PROCESO,
    FINALIZADO;

    public static EstadoBalanza desdeTexto(String texto) {
        if (texto == null) {
            return EN_PROCESO;
        }
        String normalizado = texto.trim().toUpperCase();
        for (EstadoBalanza estado : values()) {
            if (estado.name().equals(normalizado)) {
                return estado;
            }
        }
        return EN_PROCESO;
    }
}
