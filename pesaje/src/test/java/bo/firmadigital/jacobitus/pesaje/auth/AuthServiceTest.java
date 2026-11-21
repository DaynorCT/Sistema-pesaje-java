package bo.firmadigital.jacobitus.pesaje.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bo.firmadigital.jacobitus.pesaje.datos.Database;

/**
 * El login se prueba contra una base temporal fuera del repositorio: la
 * base real del usuario no se toca nunca.
 */
public class AuthServiceTest {

    private String rutaOriginal;
    private File carpetaTemporal;
    private Database base;
    private AuthService auth;

    @Before
    public void preparar() throws IOException {
        rutaOriginal = System.getProperty(Database.PROPIEDAD_SISTEMA_RUTA);
        carpetaTemporal = Files.createTempDirectory("pesaje-auth-test").toFile();
        System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA,
                new File(carpetaTemporal, "pesaje.db").getAbsolutePath());
        Database.cerrarInstancia();
        base = Database.obtenerInstancia();
        auth = new AuthService(base);
    }

    @After
    public void limpiar() {
        Database.cerrarInstancia();
        if (rutaOriginal == null) {
            System.clearProperty(Database.PROPIEDAD_SISTEMA_RUTA);
        } else {
            System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA, rutaOriginal);
        }
        borrar(carpetaTemporal);
    }

    @Test
    public void elAdministradorSembradoPuedeIniciarSesion() {
        String clave = base.claveAdministrador();
        assertNotNull("El primer arranque debe sembrar el administrador.", clave);

        ResultadoAutenticacion resultado = auth.autenticar("admin@sistema.com", clave);

        assertTrue(resultado.esExito());
        assertEquals(EstadoAutenticacion.EXITO, resultado.getEstado());
        assertNotNull(resultado.getUsuario());
        assertEquals("admin@sistema.com", resultado.getUsuario().getEmail());
        assertTrue(resultado.getUsuario().esAdministrador());
    }

    @Test
    public void unaContrasenaIncorrectaSeRechaza() {
        String clave = base.claveAdministrador();

        ResultadoAutenticacion resultado = auth.autenticar("admin@sistema.com", clave + "x");

        assertFalse(resultado.esExito());
        assertEquals(EstadoAutenticacion.CONTRASENA_INVALIDA, resultado.getEstado());
    }

    @Test
    public void unCorreoDesconocidoSeRechaza() {
        ResultadoAutenticacion resultado = auth.autenticar("nadie@sistema.com", "lo-que-sea");

        assertFalse(resultado.esExito());
        assertEquals(EstadoAutenticacion.EMAIL_INVALIDO, resultado.getEstado());
    }

    @Test
    public void unUsuarioInactivoNoPuedeIniciarSesion() {
        crearUsuario("inactivo@sistema.com", "clave123", "INACTIVO");

        ResultadoAutenticacion resultado = auth.autenticar("inactivo@sistema.com", "clave123");

        assertFalse(resultado.esExito());
        assertEquals(EstadoAutenticacion.USUARIO_INACTIVO, resultado.getEstado());
    }

    @Test
    public void laVerificacionDeContrasenaSirveParaCambiarDeModo() {
        String clave = base.claveAdministrador();

        assertTrue(auth.verificarContrasena("admin@sistema.com", clave));
        assertFalse(auth.verificarContrasena("admin@sistema.com", "otra-clave"));
        assertFalse(auth.verificarContrasena("nadie@sistema.com", clave));
    }

    @Test
    public void migraHashSha1APbkdf2AlIniciarSesion() {
        String legado = "sha1:NTU3YzU1NWNlNzg1ZGViMDZjNjA0NjhhZjViYzk3YTUxMDMyYjFiOA==:"
                + "Zlg1akJleTdqbmRWaU44VA==";
        long personaId = base.sql().insertar(
                "INSERT INTO personas (nombre, correo, estado, fecha_creacion) VALUES (?, ?, ?, ?)",
                "Legacy", "legacy@sistema.com", "ACTIVO", "2026-11-20 10:00:00");
        base.sql().insertar(
                "INSERT INTO usuarios (persona_id, email, password_hash, rol, estado, fecha_creacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                personaId, "legacy@sistema.com", legado, "usuario", "ACTIVO", "2026-11-20 10:00:00");

        ResultadoAutenticacion resultado = auth.autenticar("legacy@sistema.com", "admin123");
        assertTrue(resultado.esExito());

        String hash = base.sql().consultar(
                "SELECT password_hash FROM usuarios WHERE email = ?",
                rs -> rs.getString("password_hash"),
                "legacy@sistema.com").get(0);
        assertTrue(Passwords.esHashModerno(hash));
        assertTrue(Passwords.verificar("admin123", hash));
    }

    @Test
    public void elUltimoLoginQuedaRegistrado() {
        String clave = base.claveAdministrador();

        auth.autenticar("admin@sistema.com", clave);

        List<Long> ultimos = base.sql().consultar(
                "SELECT ultimo_login FROM usuarios WHERE email = ?",
                resultado -> resultado.getLong("ultimo_login"),
                "admin@sistema.com");
        assertFalse(ultimos.isEmpty());
        assertTrue("El ultimo login debe quedar registrado.", ultimos.get(0) > 0L);
    }

    private void crearUsuario(String email, String clave, String estado) {
        long personaId = base.sql().insertar(
                "INSERT INTO personas (nombre, correo, estado, fecha_creacion) VALUES (?, ?, ?, ?)",
                "Usuario", email, "ACTIVO", "2026-11-20 10:00:00");
        base.sql().insertar(
                "INSERT INTO usuarios (persona_id, email, password_hash, rol, estado, fecha_creacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                personaId, email, Passwords.hash(clave), "usuario", estado, "2026-11-20 10:00:00");
    }

    private static void borrar(File archivo) {
        if (archivo == null || !archivo.exists()) {
            return;
        }
        File[] hijos = archivo.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                borrar(hijo);
            }
        }
        archivo.delete();
    }
}
