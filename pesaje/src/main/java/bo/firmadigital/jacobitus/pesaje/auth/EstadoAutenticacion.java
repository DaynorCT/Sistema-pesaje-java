package bo.firmadigital.jacobitus.pesaje.auth;

/**
 * Resultado de un intento de inicio de sesion.
 *
 * Los motivos se distinguen porque la pantalla de login los trata
 * distinto: un email inexistente y una contrasena incorrecta no deben
 * dar el mismo mensaje.
 */
public enum EstadoAutenticacion {

    EXITO,
    EMAIL_INVALIDO,
    CONTRASENA_INVALIDA,
    USUARIO_INACTIVO,
    ERROR
}
