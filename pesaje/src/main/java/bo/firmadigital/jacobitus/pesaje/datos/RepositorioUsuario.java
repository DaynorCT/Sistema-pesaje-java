package bo.firmadigital.jacobitus.pesaje.datos;

import java.util.List;

import bo.firmadigital.jacobitus.pesaje.auth.Passwords;

public final class RepositorioUsuario {

    private final Sql sql = Database.obtenerInstancia().sql();

    private static final String SELECT =
            "SELECT u.id, p.nombre, p.apellido_paterno, u.email, u.rol, p.telefono, u.estado"
                    + " FROM usuarios u JOIN personas p ON p.id = u.persona_id";

    public List<RegistroUsuario> listarTodos() {
        return sql.consultar(SELECT + " ORDER BY u.id DESC", RegistroUsuario::deFila);
    }

    public long crear(String nombre, String apellidoPaterno, String apellidoMaterno,
            String ci, String telefono, String correo, String rol, String contrasenaPlana) {
        String fecha = Fechas.ahora();
        String hash = contrasenaPlana != null && !contrasenaPlana.isEmpty()
                ? Passwords.hash(contrasenaPlana) : Passwords.hash(generarClaveTemporal());
        return sql.enTransaccion(operaciones -> {
            long personaId = operaciones.insertar(
                    "INSERT INTO personas (nombre, apellido_paterno, apellido_materno, ci, correo,"
                            + " telefono, estado, fecha_creacion, fecha_modificacion)"
                            + " VALUES (?, ?, ?, ?, ?, ?, 'ACTIVO', ?, ?)",
                    nombre, apellidoPaterno, apellidoMaterno, ci, correo, telefono, fecha, fecha);
            return operaciones.insertar(
                    "INSERT INTO usuarios (persona_id, email, password_hash, rol, estado,"
                            + " fecha_creacion, fecha_modificacion) VALUES (?, ?, ?, ?, 'ACTIVO', ?, ?)",
                    personaId, correo, hash, rol, fecha, fecha);
        });
    }

    public void actualizar(int usuarioId, String nombre, String apellidoPaterno, String apellidoMaterno,
            String ci, String telefono, String correo, String rol, String estado, String nuevaClaveOpcional) {
        String fecha = Fechas.ahora();
        sql.enTransaccion(operaciones -> {
            Integer personaId = operaciones.consultar(
                    "SELECT persona_id FROM usuarios WHERE id = ?",
                    fila -> fila.getInt("persona_id"),
                    usuarioId).stream().findFirst().orElse(null);
            if (personaId == null) {
                throw new ErrorDeBaseDeDatos("Usuario no encontrado: " + usuarioId);
            }
            operaciones.actualizar(
                    "UPDATE personas SET nombre = ?, apellido_paterno = ?, apellido_materno = ?,"
                            + " ci = ?, correo = ?, telefono = ?, estado = ?, fecha_modificacion = ? WHERE id = ?",
                    nombre, apellidoPaterno, apellidoMaterno, ci, correo, telefono, estado, fecha, personaId);
            if (nuevaClaveOpcional != null && !nuevaClaveOpcional.isEmpty()) {
                operaciones.actualizar(
                        "UPDATE usuarios SET email = ?, rol = ?, estado = ?, password_hash = ?,"
                                + " fecha_modificacion = ? WHERE id = ?",
                        correo, rol, estado, Passwords.hash(nuevaClaveOpcional), fecha, usuarioId);
            } else {
                operaciones.actualizar(
                        "UPDATE usuarios SET email = ?, rol = ?, estado = ?, fecha_modificacion = ? WHERE id = ?",
                        correo, rol, estado, fecha, usuarioId);
            }
            return null;
        });
    }

    private static String generarClaveTemporal() {
        return "Tmp" + System.nanoTime();
    }
}
