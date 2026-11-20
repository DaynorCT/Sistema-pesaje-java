package bo.firmadigital.jacobitus.pesaje.datos;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pruebas de la capa de datos del Sistema de Pesaje.
 *
 * Todas las pruebas trabajan sobre una base temporal fuera del repositorio:
 * la ruta se sobreescribe con la propiedad de sistema {@code pesaje.db.path}
 * (mismo mecanismo que la variable de entorno PESAJE_DB_PATH), de modo que
 * la base real del usuario nunca se toca.
 */
public class DatabaseTest {

    /**
     * Tablas del dominio definidas en el esquema original del sistema
     * (lazarus-pesaje/src/database/schema.sql). Los nombres deben coincidir
     * exactamente porque el escritorio se sincroniza con la web.
     */
    private static final List<String> TABLAS_DOMINIO = Arrays.asList(
            "personas",
            "usuarios",
            "empresas",
            "proveedores",
            "choferes",
            "vehiculos",
            "vehiculo_chofer",
            "bodegas",
            "productos",
            "origenes",
            "destinos",
            "pesajes",
            "boleta_config",
            "config_balanza",
            "sync_outbox",
            "sync_cursor",
            "sync_conflictos");

    /**
     * SQLite crea la tabla interna {@code sqlite_sequence} en cuanto existe una
     * tabla con AUTOINCREMENT, de modo que el total de tablas del esquema es
     * 18 (17 del dominio + sqlite_sequence).
     */
    private static final int TOTAL_TABLAS_ESPERADO = 18;

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private Path rutaBase;
    private Database base;

    @Before
    public void prepararBaseTemporal() {
        rutaBase = new File(carpeta.getRoot(), Database.ARCHIVO_BASE).toPath();
        System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA, rutaBase.toString());
        Database.cerrarInstancia();
        base = Database.obtenerInstancia();
    }

    @After
    public void cerrarBase() {
        Database.cerrarInstancia();
        System.clearProperty(Database.PROPIEDAD_SISTEMA_RUTA);
    }

    // ───────────────────────────────────────────────────────────────────
    // (a) Base temporal + (b) esquema completo
    // ───────────────────────────────────────────────────────────────────

    @Test
    public void creaElEsquemaCompletoEnUnaBaseTemporal() {
        // La ruta efectiva debe ser la temporal: los tests jamas tocan la base real.
        assertEquals(rutaBase.toAbsolutePath().normalize(), base.rutaBase());
        assertTrue("La base debe quedar dentro del directorio temporal: " + base.rutaBase(),
                base.rutaBase().startsWith(carpeta.getRoot().getAbsolutePath()));
        assertTrue("El archivo de la base debe existir", Files.exists(base.rutaBase()));

        Set<String> tablas = tablasDeLaBase();

        for (String tabla : TABLAS_DOMINIO) {
            assertTrue("Falta la tabla " + tabla + " en el esquema creado", tablas.contains(tabla));
        }

        // 17 tablas del dominio + sqlite_sequence = 18.
        assertEquals("El esquema debe tener 18 tablas", TOTAL_TABLAS_ESPERADO, tablas.size());
        assertTrue("Debe existir la tabla interna sqlite_sequence", tablas.contains("sqlite_sequence"));
    }

    @Test
    public void creaLosIndicesDelEsquemaOriginal() {
        List<String> indices = indicesDeLaBase();
        assertTrue(indices.contains("idx_usuarios_persona"));
        assertTrue(indices.contains("idx_usuarios_email"));
        assertTrue(indices.contains("idx_choferes_persona"));
        assertTrue(indices.contains("idx_pesajes_fecha"));
        assertTrue(indices.contains("idx_pesajes_vehiculo"));
        assertTrue(indices.contains("idx_sync_outbox_pendientes"));
    }

    @Test
    public void usaElDirectorioDeDatosDelSistemaOperativo() {
        Path directorio = Database.directorioDatos();
        assertEquals(Database.CARPETA_DATOS, directorio.getFileName().toString());

        String sistema = System.getProperty("os.name", "").toLowerCase();
        if (sistema.contains("win")) {
            assertTrue("En Windows la base debe vivir bajo APPDATA: " + directorio,
                    directorio.toString().contains("AppData") || directorio.toString().contains("APPDATA"));
        } else if (sistema.contains("mac") || sistema.contains("darwin")) {
            assertTrue("En macOS la base debe vivir en Application Support: " + directorio,
                    directorio.toString().contains("Application Support"));
        } else {
            assertTrue("En Linux la base debe vivir en .local/share: " + directorio,
                    directorio.toString().contains(".local"));
        }

        assertEquals(Database.ARCHIVO_BASE, Database.rutaPorDefecto().getFileName().toString());
    }

    // ───────────────────────────────────────────────────────────────────
    // (c) Claves foraneas activas
    // ───────────────────────────────────────────────────────────────────

    @Test
    public void aplicaLasClavesForaneas() throws SQLException {
        assertEquals("PRAGMA foreign_keys debe estar activo", 1,
                base.sql().consultar("PRAGMA foreign_keys", rs -> rs.getInt(1)).get(0).intValue());
    }

    @Test
    public void rechazaUnaInsercionConClaveForaneaInvalida() {
        // choferes.persona_id -> personas(id): la persona 999999 no existe.
        afirmarViolacionDeClaveForanea(() -> base.sql().insertar(
                "INSERT INTO choferes (persona_id, licencia, estado) VALUES (?, ?, ?)",
                999999L, "L-1", "ACTIVO"));

        // El intento fallido no debe dejar filas en la tabla.
        assertEquals(0L, base.sql().contar("SELECT COUNT(*) FROM choferes"));
    }

    @Test
    public void rechazaUnPesajeConClaveForaneaInvalida() {
        assertEquals(0L, base.sql().contar("SELECT COUNT(*) FROM pesajes"));

        afirmarViolacionDeClaveForanea(() -> base.sql().insertar(
                "INSERT INTO pesajes (vehiculo_id, peso_bruto, estado) VALUES (?, ?, ?)",
                424242L, 1000, "ACTIVO"));
    }

    @Test
    public void aceptaUnaRelacionValidaEntreTablas() {
        long personaId = base.sql().insertar(
                "INSERT INTO personas (nombre, estado, fecha_creacion) VALUES (?, ?, ?)",
                "Juan Perez", "ACTIVO", "2026-11-20 10:00:00");

        long choferId = base.sql().insertar(
                "INSERT INTO choferes (persona_id, licencia, estado) VALUES (?, ?, ?)",
                personaId, "L-777", "ACTIVO");

        assertTrue(choferId > 0);
        assertEquals(1L, base.sql().contar("SELECT COUNT(*) FROM choferes WHERE persona_id = ?", personaId));
    }

    // ───────────────────────────────────────────────────────────────────
    // (d) Migraciones idempotentes
    // ───────────────────────────────────────────────────────────────────

    @Test
    public void lasMigracionesSonIdempotentes() {
        long personaId = base.sql().insertar(
                "INSERT INTO personas (nombre, apellido_paterno, estado, fecha_creacion) VALUES (?, ?, ?, ?)",
                "Maria", " Lopez", "ACTIVO", "2026-11-20 10:00:00");
        long vehiculoId = base.sql().insertar(
                "INSERT INTO vehiculos (placa, tipo_vehiculo, tara, estado) VALUES (?, ?, ?, ?)",
                "1234-ABC", "CAMION", 7000, "ACTIVO");

        Map<String, List<String>> esquemaAntes = esquemaDeColumnas();
        int versionAntes = base.versionEsquema();
        long filasAntes = base.sql().contar("SELECT COUNT(*) FROM personas");
        long pesajesAntes = base.sql().contar("SELECT COUNT(*) FROM pesajes");

        // Aplicar las migraciones dos veces no debe cambiar el esquema ni los datos.
        base.inicializar();
        base.inicializar();

        assertEquals("El esquema no debe cambiar al re-aplicar las migraciones",
                esquemaAntes, esquemaDeColumnas());
        assertEquals("La version del esquema no debe cambiar", versionAntes, base.versionEsquema());
        assertEquals("Los datos no deben perderse", filasAntes,
                base.sql().contar("SELECT COUNT(*) FROM personas"));
        assertEquals("Los datos no deben perderse", pesajesAntes,
                base.sql().contar("SELECT COUNT(*) FROM pesajes"));

        List<String> nombres = base.sql().consultar("SELECT nombre FROM personas WHERE id = ?",
                rs -> rs.getString("nombre"), personaId);
        assertEquals(Arrays.asList("Maria"), nombres);

        List<Integer> taras = base.sql().consultar("SELECT tara FROM vehiculos WHERE id = ?",
                rs -> rs.getInt("tara"), vehiculoId);
        assertEquals(Arrays.asList(7000), taras);
    }

    @Test
    public void conservaLosDatosAlAgregarColumnasFaltantes() {
        // Simula una base instalada antes de las columnas de sincronizacion.
        base.sql().ejecutar("ALTER TABLE pesajes DROP COLUMN sync_event_id");

        base.inicializar();

        assertTrue("La columna sync_event_id debe volver a existir",
                columnasDe("pesajes").contains("sync_event_id"));
        assertEquals("La version del esquema debe quedar al dia",
                Database.VERSION_ESQUEMA, base.versionEsquema());
    }

    @Test
    public void registraLaVersionDelEsquemaEnUserVersion() {
        assertEquals(Database.VERSION_ESQUEMA, base.versionEsquema());
        assertTrue("user_version debe haber quedado escrito", base.versionEsquema() > 0);
    }

    // ───────────────────────────────────────────────────────────────────
    // (e) Respaldo
    // ───────────────────────────────────────────────────────────────────

    @Test
    public void generaElRespaldoDeLaBase() throws Exception {
        Path respaldo = new File(carpeta.getRoot(), Database.ARCHIVO_BACKUP).toPath();

        assertTrue("Debe generarse el respaldo " + respaldo, Files.exists(respaldo));
        assertTrue("El respaldo no debe estar vacio", Files.size(respaldo) > 0);

        // El respaldo debe ser una base utilizable con el mismo esquema.
        try (Connection conexion = DriverManager.getConnection("jdbc:sqlite:" + respaldo);
             Statement statement = conexion.createStatement()) {
            ResultSet resultado = statement.executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'");
            resultado.next();
            assertEquals(TOTAL_TABLAS_ESPERADO, resultado.getInt(1));
        }
    }

    @Test
    public void regeneraElRespaldoConElEstadoActual() throws Exception {
        Path respaldo = new File(carpeta.getRoot(), Database.ARCHIVO_BACKUP).toPath();
        assertTrue(Files.exists(respaldo));

        long personaId = base.sql().insertar(
                "INSERT INTO personas (nombre, estado, fecha_creacion) VALUES (?, ?, ?)",
                "Pedro", "ACTIVO", "2026-11-20 11:00:00");
        assertTrue(personaId > 0);

        Path generado = base.generarBackup();
        assertEquals(respaldo.toAbsolutePath().normalize(), generado.toAbsolutePath().normalize());
        assertTrue(Files.exists(generado));

        try (Connection conexion = DriverManager.getConnection("jdbc:sqlite:" + generado);
             Statement statement = conexion.createStatement()) {
            ResultSet resultado = statement.executeQuery("SELECT COUNT(*) FROM personas WHERE nombre = 'Pedro'");
            resultado.next();
            assertEquals(1, resultado.getInt(1));
        }
    }

    @Test
    public void siembraElUsuarioAdministradorConContrasenaGenerada() {
        long administradores = base.sql().contar("SELECT COUNT(*) FROM usuarios WHERE rol = ?", "administrador");
        assertEquals("Debe sembrarse un usuario administrador", 1L, administradores);

        long conHash = base.sql().contar(
                "SELECT COUNT(*) FROM usuarios WHERE rol = ? AND password_hash IS NOT NULL", "administrador");
        assertEquals("El administrador debe tener contrasena", 1L, conHash);

        // El hash no puede ser el original en texto plano ni una huella debil.
        List<String> hashes = base.sql().consultar(
                "SELECT password_hash FROM usuarios WHERE rol = ?", rs -> rs.getString("password_hash"), "administrador");
        assertEquals(1, hashes.size());
        assertFalse("No se debe usar SHA1 (D3)", hashes.get(0).toLowerCase().contains("sha1"));
        assertFalse("No se debe guardar la contrasena en claro (D3)",
                hashes.get(0).contains("admin123"));

        // Reabrir la base no debe crear un segundo administrador.
        Database.cerrarInstancia();
        base = Database.obtenerInstancia();
        assertEquals(1L, base.sql().contar("SELECT COUNT(*) FROM usuarios WHERE rol = ?", "administrador"));
    }

    @Test
    public void cierraLaConexionDeFormaOrdenada() {
        base.close();
        try {
            base.sql().contar("SELECT COUNT(*) FROM personas");
            fail("La conexion deberia estar cerrada");
        } catch (ErrorDeBaseDeDatos esperado) {
            assertNotNull(esperado.getMessage());
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // Utilidades de apoyo
    // ───────────────────────────────────────────────────────────────────

    private void afirmarViolacionDeClaveForanea(Accion accion) {
        try {
            accion.ejecutar();
            fail("Se esperaba una violacion de clave foranea");
        } catch (ErrorDeBaseDeDatos e) {
            Throwable causa = e.getCause() != null ? e.getCause() : e;
            String mensaje = String.valueOf(causa.getMessage()).toUpperCase();
            assertTrue("La causa debe mencionar la clave foranea: " + causa.getMessage(),
                    mensaje.contains("FOREIGN KEY"));
        }
    }

    private Set<String> tablasDeLaBase() {
        Set<String> tablas = new TreeSet<>();
        base.sql().consultar("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name",
                rs -> rs.getString("name")).forEach(tablas::add);
        return tablas;
    }

    private List<String> indicesDeLaBase() {
        return base.sql().consultar("SELECT name FROM sqlite_master WHERE type = 'index' ORDER BY name",
                rs -> rs.getString("name"));
    }

    private Map<String, List<String>> esquemaDeColumnas() {
        Map<String, List<String>> esquema = new LinkedHashMap<>();
        for (String tabla : TABLAS_DOMINIO) {
            esquema.put(tabla, columnasDe(tabla));
        }
        return esquema;
    }

    private List<String> columnasDe(String tabla) {
        List<String> columnas = new ArrayList<>();
        base.sql().consultar("PRAGMA table_info(" + tabla + ")", rs -> rs.getString("name"))
                .forEach(columnas::add);
        return columnas;
    }

    private interface Accion {
        void ejecutar();
    }
}
