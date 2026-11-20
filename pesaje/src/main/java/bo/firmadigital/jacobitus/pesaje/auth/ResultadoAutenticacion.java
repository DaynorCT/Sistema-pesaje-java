package bo.firmadigital.jacobitus.pesaje.auth;

/**
 * Resultado de un intento de inicio de sesion: el estado y, cuando fue
 * exitoso, el usuario con la sesion iniciada.
 */
public final class ResultadoAutenticacion {

    private final EstadoAutenticacion estado;
    private final UsuarioSesion usuario;

    private ResultadoAutenticacion(EstadoAutenticacion estado, UsuarioSesion usuario) {
        this.estado = estado;
        this.usuario = usuario;
    }

    public static ResultadoAutenticacion exito(UsuarioSesion usuario) {
        return new ResultadoAutenticacion(EstadoAutenticacion.EXITO, usuario);
    }

    public static ResultadoAutenticacion fallo(EstadoAutenticacion estado) {
        return new ResultadoAutenticacion(estado, null);
    }

    public boolean esExito() {
        return estado == EstadoAutenticacion.EXITO;
    }

    public EstadoAutenticacion getEstado() {
        return estado;
    }

    /** @return el usuario de la sesion, o null si el inicio no fue exitoso */
    public UsuarioSesion getUsuario() {
        return usuario;
    }
}
