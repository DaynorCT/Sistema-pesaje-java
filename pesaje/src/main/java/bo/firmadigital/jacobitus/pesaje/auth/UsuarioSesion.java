package bo.firmadigital.jacobitus.pesaje.auth;

/**
 * Usuario con sesion iniciada.
 */
public class UsuarioSesion {

    private final int id;
    private final int personaId;
    private final String email;
    private final String nombre;
    private final String rol;

    public UsuarioSesion(int id, int personaId, String email, String nombre, String rol) {
        this.id = id;
        this.personaId = personaId;
        this.email = email;
        this.nombre = nombre;
        this.rol = rol == null ? "" : rol;
    }

    public int getId() {
        return id;
    }

    public int getPersonaId() {
        return personaId;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public boolean esAdministrador() {
        return "administrador".equalsIgnoreCase(rol);
    }

    @Override
    public String toString() {
        return nombre + " (" + email + ")";
    }
}
