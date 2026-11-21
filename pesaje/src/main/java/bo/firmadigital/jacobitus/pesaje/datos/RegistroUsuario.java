package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class RegistroUsuario {

    private final int id;
    private final String nombre;
    private final String apellidoPaterno;
    private final String email;
    private final String rol;
    private final String telefono;
    private final String estado;

    public RegistroUsuario(int id, String nombre, String apellidoPaterno, String email,
            String rol, String telefono, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.email = email;
        this.rol = rol;
        this.telefono = telefono;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    public String getEmail() {
        return email;
    }

    public String getRol() {
        return rol;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEstado() {
        return estado;
    }

    static RegistroUsuario deFila(ResultSet fila) throws SQLException {
        return new RegistroUsuario(
                fila.getInt("id"),
                Sql.texto(fila, "nombre"),
                Sql.texto(fila, "apellido_paterno"),
                Sql.texto(fila, "email"),
                Sql.texto(fila, "rol"),
                Sql.texto(fila, "telefono"),
                Sql.texto(fila, "estado"));
    }
}
