package bo.firmadigital.jacobitus.pesaje.datos;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ABM de tablas simples nombre/descripcion (productos, origenes, destinos).
 */
public final class RepositorioCatalogo {

    private static final Set<String> TABLAS_PERMITIDAS = new HashSet<>(
            Arrays.asList("productos", "origenes", "destinos"));

    private final Sql sql;
    private final String tabla;

    public RepositorioCatalogo(String tabla) {
        if (!TABLAS_PERMITIDAS.contains(tabla)) {
            throw new IllegalArgumentException("Tabla de catalogo no permitida: " + tabla);
        }
        this.sql = Database.obtenerInstancia().sql();
        this.tabla = tabla;
    }

    public List<RegistroCatalogo> listarActivos() {
        return listar("ACTIVO");
    }

    public List<RegistroCatalogo> listarTodos() {
        return sql.consultar(
                "SELECT id, nombre, descripcion, estado FROM " + tabla + " ORDER BY id DESC",
                RegistroCatalogo::deFila);
    }

    public List<RegistroCatalogo> listar(String estado) {
        return sql.consultar(
                "SELECT id, nombre, descripcion, estado FROM " + tabla
                        + " WHERE estado = ? ORDER BY nombre",
                RegistroCatalogo::deFila,
                estado);
    }

    public long crear(String nombre, String descripcion) {
        String fecha = Fechas.ahora();
        return sql.insertar(
                "INSERT INTO " + tabla + " (nombre, descripcion, estado, pendiente_push,"
                        + " fecha_creacion, fecha_modificacion) VALUES (?, ?, 'ACTIVO', 1, ?, ?)",
                nombre, descripcion, fecha, fecha);
    }

    public void actualizar(int id, String nombre, String descripcion, String estado) {
        sql.actualizar(
                "UPDATE " + tabla + " SET nombre = ?, descripcion = ?, estado = ?,"
                        + " pendiente_push = 1, fecha_modificacion = ? WHERE id = ?",
                nombre, descripcion, estado, Fechas.ahora(), id);
    }

    public void cambiarEstado(int id, String nuevoEstado) {
        sql.actualizar(
                "UPDATE " + tabla + " SET estado = ?, pendiente_push = 1, fecha_modificacion = ? WHERE id = ?",
                nuevoEstado, Fechas.ahora(), id);
    }
}
