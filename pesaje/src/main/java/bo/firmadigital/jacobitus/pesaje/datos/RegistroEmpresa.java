package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class RegistroEmpresa {

    private final int id;
    private final String nombreEmpresa;
    private final String actividad;
    private final String correo;
    private final String telefono;
    private final String estado;

    public RegistroEmpresa(int id, String nombreEmpresa, String actividad, String correo,
            String telefono, String estado) {
        this.id = id;
        this.nombreEmpresa = nombreEmpresa;
        this.actividad = actividad;
        this.correo = correo;
        this.telefono = telefono;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public String getActividad() {
        return actividad;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEstado() {
        return estado;
    }

    static RegistroEmpresa deFila(ResultSet fila) throws SQLException {
        return new RegistroEmpresa(
                fila.getInt("id"),
                Sql.texto(fila, "nombre_empresa"),
                Sql.texto(fila, "actividad_economica"),
                Sql.texto(fila, "correo_electronico"),
                Sql.texto(fila, "telefono"),
                Sql.texto(fila, "estado"));
    }
}
