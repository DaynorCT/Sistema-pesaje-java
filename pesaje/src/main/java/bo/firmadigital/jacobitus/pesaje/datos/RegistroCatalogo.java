package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Fila de un catalogo con nombre y descripcion (productos, origenes, destinos). */
public final class RegistroCatalogo {

    private final int id;
    private final String nombre;
    private final String descripcion;
    private final String estado;

    public RegistroCatalogo(int id, String nombre, String descripcion, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEstado() {
        return estado;
    }

    static RegistroCatalogo deFila(ResultSet fila) throws SQLException {
        return new RegistroCatalogo(
                fila.getInt("id"),
                Sql.texto(fila, "nombre"),
                Sql.texto(fila, "descripcion"),
                Sql.texto(fila, "estado"));
    }
}
