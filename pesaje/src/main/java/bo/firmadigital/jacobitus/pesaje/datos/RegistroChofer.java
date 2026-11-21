package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class RegistroChofer {

    private final int id;
    private final String nombre;
    private final String apellidoPaterno;
    private final String ci;
    private final String licencia;
    private final String telefono;
    private final String estado;

    public RegistroChofer(int id, String nombre, String apellidoPaterno, String ci,
            String licencia, String telefono, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.ci = ci;
        this.licencia = licencia;
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

    public String getCi() {
        return ci;
    }

    public String getLicencia() {
        return licencia;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEstado() {
        return estado;
    }

    public String nombreCompleto() {
        return (nombre != null ? nombre : "") + (apellidoPaterno != null ? " " + apellidoPaterno : "");
    }

    static RegistroChofer deFila(ResultSet fila) throws SQLException {
        return new RegistroChofer(
                fila.getInt("id"),
                Sql.texto(fila, "nombre"),
                Sql.texto(fila, "apellido_paterno"),
                Sql.texto(fila, "ci"),
                Sql.texto(fila, "licencia"),
                Sql.texto(fila, "telefono"),
                Sql.texto(fila, "estado"));
    }
}
