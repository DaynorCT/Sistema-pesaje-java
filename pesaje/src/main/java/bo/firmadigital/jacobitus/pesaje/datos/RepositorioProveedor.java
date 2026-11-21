package bo.firmadigital.jacobitus.pesaje.datos;

import java.util.List;

public final class RepositorioProveedor {

    private final Sql sql = Database.obtenerInstancia().sql();

    private static final String SELECT =
            "SELECT pr.id, p.nombre, p.apellido_paterno, p.apellido_materno, p.ci, pr.nombre_empresa,"
                    + " pr.descripcion, p.telefono, pr.estado"
                    + " FROM proveedores pr JOIN personas p ON p.id = pr.persona_id";

    public List<RegistroProveedor> listarActivos() {
        return sql.consultar(SELECT + " WHERE pr.estado = 'ACTIVO' ORDER BY pr.nombre_empresa, p.nombre",
                RegistroProveedor::deFila);
    }

    public List<RegistroProveedor> listarTodos() {
        return sql.consultar(SELECT + " ORDER BY pr.id DESC", RegistroProveedor::deFila);
    }

    public long crear(String nombre, String apellidoPaterno, String apellidoMaterno,
            String ci, String telefono, String nombreEmpresa, String descripcion) {
        String fecha = Fechas.ahora();
        return sql.enTransaccion(operaciones -> {
            long personaId = operaciones.insertar(
                    "INSERT INTO personas (nombre, apellido_paterno, apellido_materno, ci,"
                            + " telefono, estado, fecha_creacion, fecha_modificacion)"
                            + " VALUES (?, ?, ?, ?, ?, 'ACTIVO', ?, ?)",
                    nombre, apellidoPaterno, apellidoMaterno, ci, telefono, fecha, fecha);
            return operaciones.insertar(
                    "INSERT INTO proveedores (persona_id, nombre_empresa, descripcion, estado,"
                            + " pendiente_push, fecha_creacion, fecha_modificacion)"
                            + " VALUES (?, ?, ?, 'ACTIVO', 1, ?, ?)",
                    personaId, nombreEmpresa, descripcion, fecha, fecha);
        });
    }

    public void actualizar(int proveedorId, String nombre, String apellidoPaterno, String apellidoMaterno,
            String ci, String telefono, String nombreEmpresa, String descripcion, String estado) {
        String fecha = Fechas.ahora();
        sql.enTransaccion(operaciones -> {
            Integer personaId = operaciones.consultar(
                    "SELECT persona_id FROM proveedores WHERE id = ?",
                    fila -> fila.getInt("persona_id"),
                    proveedorId).stream().findFirst().orElse(null);
            if (personaId == null) {
                throw new ErrorDeBaseDeDatos("Proveedor no encontrado: " + proveedorId);
            }
            operaciones.actualizar(
                    "UPDATE personas SET nombre = ?, apellido_paterno = ?, apellido_materno = ?,"
                            + " ci = ?, telefono = ?, estado = ?, fecha_modificacion = ? WHERE id = ?",
                    nombre, apellidoPaterno, apellidoMaterno, ci, telefono, estado, fecha, personaId);
            operaciones.actualizar(
                    "UPDATE proveedores SET nombre_empresa = ?, descripcion = ?, estado = ?,"
                            + " pendiente_push = 1, fecha_modificacion = ? WHERE id = ?",
                    nombreEmpresa, descripcion, estado, fecha, proveedorId);
            return null;
        });
    }

    public void cambiarEstado(int proveedorId, String nuevoEstado) {
        String fecha = Fechas.ahora();
        sql.enTransaccion(operaciones -> {
            Integer personaId = operaciones.consultar(
                    "SELECT persona_id FROM proveedores WHERE id = ?",
                    fila -> fila.getInt("persona_id"),
                    proveedorId).stream().findFirst().orElse(null);
            if (personaId == null) {
                throw new ErrorDeBaseDeDatos("Proveedor no encontrado: " + proveedorId);
            }
            operaciones.actualizar(
                    "UPDATE personas SET estado = ?, fecha_modificacion = ? WHERE id = ?",
                    nuevoEstado, fecha, personaId);
            operaciones.actualizar(
                    "UPDATE proveedores SET estado = ?, pendiente_push = 1, fecha_modificacion = ? WHERE id = ?",
                    nuevoEstado, fecha, proveedorId);
            return null;
        });
    }
}
