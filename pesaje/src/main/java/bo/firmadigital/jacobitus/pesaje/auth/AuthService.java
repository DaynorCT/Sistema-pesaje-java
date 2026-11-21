package bo.firmadigital.jacobitus.pesaje.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.datos.Sql;

/**
 * Autenticacion local contra la tabla `usuarios`.
 *
 * Replica el comportamiento del sistema original (login por email,
 * verificacion de contrasena para cambiar de modo de operacion y
 * registro del ultimo login) con el hash PBKDF2 de {@link Passwords}.
 */
public class AuthService {

    private static final String CONSULTA_USUARIO =
            "SELECT u.id, u.persona_id, u.email, u.password_hash, u.rol, u.estado, "
                    + "p.nombre, p.apellido_paterno, p.apellido_materno "
                    + "FROM usuarios u "
                    + "INNER JOIN personas p ON p.id = u.persona_id "
                    + "WHERE u.email = ?";

    private final Sql sql;

    public AuthService() {
        this(Database.obtenerInstancia());
    }

    public AuthService(Database database) {
        this.sql = database.sql();
    }

    /**
     * Inicia sesion.
     *
     * @param email correo del usuario
     * @param contrasena contrasena del usuario
     * @return el resultado, con el usuario cuando el inicio fue exitoso
     */
    public ResultadoAutenticacion autenticar(String email, String contrasena) {
        if (email == null || email.trim().isEmpty() || contrasena == null) {
            return ResultadoAutenticacion.fallo(EstadoAutenticacion.EMAIL_INVALIDO);
        }

        List<FilaUsuario> filas;
        try {
            filas = sql.consultar(CONSULTA_USUARIO, FilaUsuario::deFila, email.trim());
        } catch (RuntimeException ex) {
            return ResultadoAutenticacion.fallo(EstadoAutenticacion.ERROR);
        }

        if (filas.isEmpty()) {
            return ResultadoAutenticacion.fallo(EstadoAutenticacion.EMAIL_INVALIDO);
        }

        FilaUsuario fila = filas.get(0);
        if (!"ACTIVO".equalsIgnoreCase(fila.estado)) {
            return ResultadoAutenticacion.fallo(EstadoAutenticacion.USUARIO_INACTIVO);
        }
        if (!Passwords.verificar(contrasena, fila.passwordHash)) {
            return ResultadoAutenticacion.fallo(EstadoAutenticacion.CONTRASENA_INVALIDA);
        }

        actualizarHashSiLegado(fila.id, contrasena, fila.passwordHash);
        registrarUltimoLogin(fila.id);
        return ResultadoAutenticacion.exito(fila.aUsuarioSesion());
    }

    /**
     * Verifica la contrasena de un usuario sin iniciar sesion.
     *
     * Se usa para proteger el cambio de modo: el sistema pide la
     * contrasena antes de alternar entre el modo integrado y el sistema
     * completo.
     *
     * @param email correo del usuario
     * @param contrasena contrasena del usuario
     * @return true si la contrasena es correcta y el usuario esta activo
     */
    public boolean verificarContrasena(String email, String contrasena) {
        if (email == null || email.trim().isEmpty() || contrasena == null) {
            return false;
        }
        List<FilaUsuario> filas;
        try {
            filas = sql.consultar(CONSULTA_USUARIO, FilaUsuario::deFila, email.trim());
        } catch (RuntimeException ex) {
            return false;
        }
        if (filas.isEmpty()) {
            return false;
        }
        FilaUsuario fila = filas.get(0);
        if (!"ACTIVO".equalsIgnoreCase(fila.estado)) {
            return false;
        }
        boolean ok = Passwords.verificar(contrasena, fila.passwordHash);
        if (ok) {
            actualizarHashSiLegado(fila.id, contrasena, fila.passwordHash);
        }
        return ok;
    }

    /**
     * Tras un login exitoso con hash SHA1, lo reemplaza por PBKDF2.
     */
    private void actualizarHashSiLegado(int usuarioId, String contrasena, String hashAlmacenado) {
        if (Passwords.esHashModerno(hashAlmacenado)) {
            return;
        }
        if (!Passwords.esHashLegadoSha1(hashAlmacenado)) {
            return;
        }
        try {
            sql.actualizar("UPDATE usuarios SET password_hash = ? WHERE id = ?",
                    Passwords.hash(contrasena), usuarioId);
        } catch (RuntimeException ex) {
            // El login ya fue exito; si falla la migracion no se bloquea al usuario.
        }
    }

    private void registrarUltimoLogin(int usuarioId) {
        try {
            sql.actualizar("UPDATE usuarios SET ultimo_login = ? WHERE id = ?",
                    Instant.now().getEpochSecond(), usuarioId);
        } catch (RuntimeException ex) {
            // El registro del ultimo login no es critico: no debe impedir
            // el inicio de sesion.
        }
    }

    /**
     * Fila de `usuarios` unida con `personas`.
     */
    private static final class FilaUsuario {

        private final int id;
        private final int personaId;
        private final String email;
        private final String passwordHash;
        private final String rol;
        private final String estado;
        private final String nombre;
        private final String apellidoPaterno;
        private final String apellidoMaterno;

        private FilaUsuario(int id, int personaId, String email, String passwordHash, String rol,
                String estado, String nombre, String apellidoPaterno, String apellidoMaterno) {
            this.id = id;
            this.personaId = personaId;
            this.email = email;
            this.passwordHash = passwordHash;
            this.rol = rol;
            this.estado = estado;
            this.nombre = nombre;
            this.apellidoPaterno = apellidoPaterno;
            this.apellidoMaterno = apellidoMaterno;
        }

        private static FilaUsuario deFila(ResultSet resultado) throws SQLException {
            return new FilaUsuario(
                    Sql.entero(resultado, "id"),
                    Sql.entero(resultado, "persona_id"),
                    Sql.texto(resultado, "email"),
                    Sql.texto(resultado, "password_hash"),
                    Sql.texto(resultado, "rol"),
                    Sql.texto(resultado, "estado"),
                    Sql.texto(resultado, "nombre"),
                    Sql.texto(resultado, "apellido_paterno"),
                    Sql.texto(resultado, "apellido_materno"));
        }

        private UsuarioSesion aUsuarioSesion() {
            String nombreCompleto = nombre;
            if (apellidoPaterno != null && !apellidoPaterno.isEmpty()) {
                nombreCompleto = nombreCompleto + " " + apellidoPaterno;
            }
            if (apellidoMaterno != null && !apellidoMaterno.isEmpty()) {
                nombreCompleto = nombreCompleto + " " + apellidoMaterno;
            }
            return new UsuarioSesion(id, personaId, email, nombreCompleto.trim(), rol);
        }
    }
}
