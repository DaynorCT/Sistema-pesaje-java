package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class RegistroProveedor {

    private final int id;
    private final String nombre;
    private final String apellidoPaterno;
    private final String nombreEmpresa;
    private final String descripcion;
    private final String telefono;
    private final String estado;

    public RegistroProveedor(int id, String nombre, String apellidoPaterno, String nombreEmpresa,
            String descripcion, String telefono, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.nombreEmpresa = nombreEmpresa;
        this.descripcion = descripcion;
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

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEstado() {
        return estado;
    }

    public String etiqueta() {
        if (nombreEmpresa != null && !nombreEmpresa.isEmpty()) {
            return nombreEmpresa;
        }
        return (nombre != null ? nombre : "") + (apellidoPaterno != null ? " " + apellidoPaterno : "");
    }

    static RegistroProveedor deFila(ResultSet fila) throws SQLException {
        return new RegistroProveedor(
                fila.getInt("id"),
                Sql.texto(fila, "nombre"),
                Sql.texto(fila, "apellido_paterno"),
                Sql.texto(fila, "nombre_empresa"),
                Sql.texto(fila, "descripcion"),
                Sql.texto(fila, "telefono"),
                Sql.texto(fila, "estado"));
    }
}
