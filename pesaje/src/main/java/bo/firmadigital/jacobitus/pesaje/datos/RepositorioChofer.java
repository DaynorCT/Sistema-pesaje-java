package bo.firmadigital.jacobitus.pesaje.datos;

import java.util.List;

public final class RepositorioChofer {

    private final Sql sql = Database.obtenerInstancia().sql();

    private static final String SELECT =
            "SELECT c.id, p.nombre, p.apellido_paterno, p.apellido_materno, p.ci, c.licencia,"
                    + " c.telefono, c.estado"
                    + " FROM choferes c JOIN personas p ON p.id = c.persona_id";

    public List<RegistroChofer> listarActivos() {
        return sql.consultar(SELECT + " WHERE c.estado = 'ACTIVO' ORDER BY p.nombre",
                RegistroChofer::deFila);
    }

    public List<RegistroChofer> listarTodos() {
        return sql.consultar(SELECT + " ORDER BY c.id DESC", RegistroChofer::deFila);
    }

    public long crear(String nombre, String apellidoPaterno, String apellidoMaterno,
            String ci, String telefono, String licencia) {
        String fecha = Fechas.ahora();
        return sql.enTransaccion(operaciones -> {
            long personaId = operaciones.insertar(
                    "INSERT INTO personas (nombre, apellido_paterno, apellido_materno, ci,"
                            + " telefono, estado, fecha_creacion, fecha_modificacion)"
                            + " VALUES (?, ?, ?, ?, ?, 'ACTIVO', ?, ?)",
                    nombre, apellidoPaterno, apellidoMaterno, ci, telefono, fecha, fecha);
            return operaciones.insertar(
                    "INSERT INTO choferes (persona_id, licencia, telefono, estado, pendiente_push,"
                            + " fecha_creacion, fecha_modificacion) VALUES (?, ?, ?, 'ACTIVO', 1, ?, ?)",
                    personaId, licencia, telefono, fecha, fecha);
        });
    }

    public void actualizar(int choferId, String nombre, String apellidoPaterno, String apellidoMaterno,
            String ci, String telefono, String licencia, String estado) {
        String fecha = Fechas.ahora();
        sql.enTransaccion(operaciones -> {
            Integer personaId = operaciones.consultar(
                    "SELECT persona_id FROM choferes WHERE id = ?",
                    fila -> fila.getInt("persona_id"),
                    choferId).stream().findFirst().orElse(null);
            if (personaId == null) {
                throw new ErrorDeBaseDeDatos("Chofer no encontrado: " + choferId);
            }
            operaciones.actualizar(
                    "UPDATE personas SET nombre = ?, apellido_paterno = ?, apellido_materno = ?,"
                            + " ci = ?, telefono = ?, estado = ?, fecha_modificacion = ? WHERE id = ?",
                    nombre, apellidoPaterno, apellidoMaterno, ci, telefono, estado, fecha, personaId);
            operaciones.actualizar(
                    "UPDATE choferes SET licencia = ?, telefono = ?, estado = ?, pendiente_push = 1,"
                            + " fecha_modificacion = ? WHERE id = ?",
                    licencia, telefono, estado, fecha, choferId);
            return null;
        });
    }

    public void cambiarEstado(int choferId, String nuevoEstado) {
        String fecha = Fechas.ahora();
        sql.enTransaccion(operaciones -> {
            Integer personaId = operaciones.consultar(
                    "SELECT persona_id FROM choferes WHERE id = ?",
                    fila -> fila.getInt("persona_id"),
                    choferId).stream().findFirst().orElse(null);
            if (personaId == null) {
                throw new ErrorDeBaseDeDatos("Chofer no encontrado: " + choferId);
            }
            operaciones.actualizar(
                    "UPDATE personas SET estado = ?, fecha_modificacion = ? WHERE id = ?",
                    nuevoEstado, fecha, personaId);
            operaciones.actualizar(
                    "UPDATE choferes SET estado = ?, pendiente_push = 1, fecha_modificacion = ? WHERE id = ?",
                    nuevoEstado, fecha, choferId);
            return null;
        });
    }
}
