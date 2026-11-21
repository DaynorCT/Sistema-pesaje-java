package bo.firmadigital.jacobitus.pesaje.datos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import bo.firmadigital.jacobitus.pesaje.auth.Passwords;

/**
 * Gestion de la base de datos SQLite del Sistema de Pesaje.
 *
 * Responsabilidades:
 * <ul>
 *   <li>Resuelve la ruta de la base segun el sistema operativo
 *       (Windows: {@code %APPDATA%\SistemaPesaje}; macOS:
 *       {@code ~/Library/Application Support/SistemaPesaje}; Linux:
 *       {@code ~/.local/share/SistemaPesaje}).</li>
 *   <li>Abre una unica conexion con {@code PRAGMA foreign_keys = ON}.</li>
 *   <li>Crea el esquema en el primer arranque a partir del recurso
 *       {@code /db/schema.sql}.</li>
 *   <li>Aplica migraciones idempotentes versionadas con
 *       {@code PRAGMA user_version}: las columnas que falten se agregan con
 *       {@code ALTER TABLE ... ADD COLUMN} sin perder datos.</li>
 *   <li>Genera el respaldo {@code pesaje.db.backup}.</li>
 *   <li>Siembra el usuario administrador con una contrasena generada en el
 *       primer arranque (decision D3: no hay contrasena por defecto).</li>
 * </ul>
 *
 * Sobre el uso desde hilos: la conexion SQLite no debe compartirse entre
 * hilos. Toda operacion de esta clase y de {@link Sql} pasa por un cerrojo
 * reentrante, de modo que el acceso queda serializado, pero la capa de datos
 * no debe usarse desde hilos de fondo: el sincronizador (T11) resolvera ese
 * escenario por su cuenta.
 *
 * La ruta de la base puede sobreescribirse con la variable de entorno
 * {@code PESAJE_DB_PATH} o con la propiedad de sistema
 * {@code pesaje.db.path}; esto permite que las pruebas trabajen sobre una
 * base temporal fuera del repositorio sin tocar la base del usuario.
 */
public final class Database implements AutoCloseable {

    /** Carpeta de datos del sistema. */
    public static final String CARPETA_DATOS = "SistemaPesaje";

    /** Nombre del archivo de la base de datos. */
    public static final String ARCHIVO_BASE = "pesaje.db";

    /** Nombre del archivo de respaldo. */
    public static final String ARCHIVO_BACKUP = "pesaje.db.backup";

    /** Variable de entorno que sobreescribe la ruta de la base. */
    public static final String VARIABLE_ENTORNO_RUTA = "PESAJE_DB_PATH";

    /** Propiedad de sistema que sobreescribe la ruta de la base. */
    public static final String PROPIEDAD_SISTEMA_RUTA = "pesaje.db.path";

    /** Version del esquema (PRAGMA user_version). */
    public static final int VERSION_ESQUEMA = 2;

    /** Recurso con el esquema completo de la base. */
    public static final String RECURSO_ESQUEMA = "/db/schema.sql";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String EMAIL_ADMINISTRADOR = "admin@sistema.com";
    private static final int LONGITUD_CLAVE_GENERADA = 12;
    private static final String ALFABETO_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Object CERROJO_INSTANCIA = new Object();

    private static Database instancia;

    private final Path rutaBase;
    private final Connection conexion;
    private final ReentrantLock cerrojo = new ReentrantLock();
    private final Sql sql;

    private boolean cerrada;
    private String claveAdministrador;

    private Database(Path rutaBase) {
        this.rutaBase = rutaBase.toAbsolutePath().normalize();
        boolean baseExistente = Files.exists(this.rutaBase);
        try {
            Path padre = this.rutaBase.getParent();
            if (padre != null && !Files.isDirectory(padre)) {
                Files.createDirectories(padre);
            }
            this.conexion = DriverManager.getConnection("jdbc:sqlite:" + this.rutaBase);
            this.conexion.setAutoCommit(true);
            try (Statement statement = this.conexion.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            this.sql = new Sql(this);
        } catch (IOException | SQLException e) {
            throw new ErrorDeBaseDeDatos("No se pudo abrir la base de datos " + this.rutaBase
                    + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", e);
        }
        inicializar(baseExistente);
    }

    // ───────────────────────────────────────────────────────────────────
    // Instancia unica
    // ───────────────────────────────────────────────────────────────────

    /**
     * Devuelve la instancia unica de la base de datos, creandola si hace
     * falta (aplica el esquema, las migraciones y la semilla inicial).
     *
     * @return instancia de la base de datos
     */
    public static Database obtenerInstancia() {
        synchronized (CERROJO_INSTANCIA) {
            if (instancia == null) {
                instancia = new Database(rutaResuelta());
            }
            return instancia;
        }
    }

    /**
     * Cierra la instancia unica (si existe) y libera la conexion.
     */
    public static void cerrarInstancia() {
        synchronized (CERROJO_INSTANCIA) {
            if (instancia != null) {
                instancia.close();
                instancia = null;
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // Rutas
    // ───────────────────────────────────────────────────────────────────

    /**
     * Ruta efectiva de la base: la sobreescrita por
     * {@link #PROPIEDAD_SISTEMA_RUTA} o {@link #VARIABLE_ENTORNO_RUTA}, y si
     * no hay ninguna, la ruta por defecto del sistema operativo.
     *
     * @return ruta del archivo de la base de datos
     */
    public static Path rutaResuelta() {
        String propiedad = System.getProperty(PROPIEDAD_SISTEMA_RUTA);
        if (propiedad != null && !propiedad.trim().isEmpty()) {
            return Paths.get(propiedad.trim());
        }
        String entorno = System.getenv(VARIABLE_ENTORNO_RUTA);
        if (entorno != null && !entorno.trim().isEmpty()) {
            return Paths.get(entorno.trim());
        }
        return rutaPorDefecto();
    }

    /**
     * Directorio de datos por defecto segun el sistema operativo.
     *
     * @return directorio de datos
     */
    public static Path directorioDatos() {
        String sistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Path home = Paths.get(System.getProperty("user.home", "."));
        if (sistema.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path base = (appData != null && !appData.trim().isEmpty()) ? Paths.get(appData.trim()) : home;
            return base.resolve(CARPETA_DATOS);
        }
        if (sistema.contains("mac") || sistema.contains("darwin")) {
            return home.resolve("Library").resolve("Application Support").resolve(CARPETA_DATOS);
        }
        return home.resolve(".local").resolve("share").resolve(CARPETA_DATOS);
    }

    /**
     * Ruta por defecto del archivo de la base de datos.
     *
     * @return ruta del archivo {@code pesaje.db}
     */
    public static Path rutaPorDefecto() {
        return directorioDatos().resolve(ARCHIVO_BASE);
    }

    /**
     * @return ruta del archivo de la base de datos en uso
     */
    public Path rutaBase() {
        return rutaBase;
    }

    /**
     * @return helpers de SQL sobre la conexion de esta base
     */
    public Sql sql() {
        return sql;
    }

    // ───────────────────────────────────────────────────────────────────
    // Arranque, esquema y migraciones
    // ───────────────────────────────────────────────────────────────────

    /**
     * Aplica el esquema y las migraciones. Es idempotente: invocarla de nuevo
     * no cambia el esquema ni los datos.
     */
    public void inicializar() {
        cerrojo.lock();
        try {
            try {
                aplicarEsquemaYMigraciones();
            } catch (SQLException e) {
                throw new ErrorDeBaseDeDatos("No se pudo inicializar el esquema de " + rutaBase, e);
            }
        } finally {
            cerrojo.unlock();
        }
    }

    private void inicializar(boolean baseExistente) {
        try {
            if (baseExistente) {
                // Respaldo del estado previo a las migraciones: si una
                // migracion falla, se puede volver a este punto.
                generarBackup();
            }
            aplicarEsquemaYMigraciones();
            if (!baseExistente) {
                // Primer arranque: se deja un respaldo restaurable del esquema.
                generarBackup();
            }
        } catch (SQLException e) {
            throw new ErrorDeBaseDeDatos("No se pudo inicializar el esquema de " + rutaBase, e);
        }
    }

    private void aplicarEsquemaYMigraciones() throws SQLException {
        aplicarEsquema();
        aplicarMigraciones();
        sembrarAdministrador();
    }

    /**
     * Crea las tablas y los indices que faltan a partir del recurso de
     * esquema. Las sentencias se aplican de una en una porque el driver
     * SQLite solo ejecuta la primera sentencia de un lote.
     */
    private void aplicarEsquema() throws SQLException {
        for (String sentencia : dividirSentencias(leerRecursoEsquema())) {
            try (Statement statement = conexion.createStatement()) {
                statement.execute(sentencia);
            }
        }
    }

    /**
     * Aplica las migraciones versionadas.
     *
     * La version vigente se lleva en {@code PRAGMA user_version}. Las
     * adiciones de columna son declarativas e idempotentes, por lo que se
     * revisan en cada arranque: asi una base creada por otra herramienta
     * (por ejemplo el sistema original Lazarus) converge al esquema sin
     * perder datos. Cuando una migracion agregue transformaciones de datos
     * (no idempotentes), debera quedar acotada por la version y ejecutarse
     * solo si su version es mayor que la vigente.
     */
    private void aplicarMigraciones() throws SQLException {
        int versionActual = leerVersionEsquema();
        for (Migracion migracion : MIGRACIONES) {
            agregarColumnasFaltantes(migracion.columnas);
            if (migracion.version > versionActual) {
                escribirVersionEsquema(migracion.version);
                versionActual = migracion.version;
            }
        }
        if (versionActual < VERSION_ESQUEMA) {
            escribirVersionEsquema(VERSION_ESQUEMA);
        }
    }

    /**
     * Version del esquema registrada en {@code PRAGMA user_version}.
     *
     * @return version vigente
     */
    public int versionEsquema() {
        try {
            return leerVersionEsquema();
        } catch (SQLException e) {
            throw new ErrorDeBaseDeDatos("No se pudo leer la version del esquema", e);
        }
    }

    private int leerVersionEsquema() throws SQLException {
        try (Statement statement = conexion.createStatement();
             ResultSet resultado = statement.executeQuery("PRAGMA user_version")) {
            return resultado.next() ? resultado.getInt(1) : 0;
        }
    }

    private void escribirVersionEsquema(int version) throws SQLException {
        try (Statement statement = conexion.createStatement()) {
            statement.execute("PRAGMA user_version = " + version);
        }
    }

    private void agregarColumnasFaltantes(List<Columna> columnas) throws SQLException {
        for (Columna columna : columnas) {
            if (!existeTabla(columna.tabla) || existeColumna(columna.tabla, columna.columna)) {
                continue;
            }
            // Los nombres provienen de la definicion interna de migraciones y se
            // validan contra el catalogo de SQLite antes de usarlos.
            String tabla = identificadorSeguro(columna.tabla);
            String nombre = identificadorSeguro(columna.columna);
            try (Statement statement = conexion.createStatement()) {
                statement.execute("ALTER TABLE " + tabla + " ADD COLUMN " + nombre + " " + columna.definicion);
            }
        }
    }

    private boolean existeColumna(String tabla, String columna) throws SQLException {
        String consulta = "PRAGMA table_info(" + identificadorSeguro(tabla) + ")";
        try (Statement statement = conexion.createStatement();
             ResultSet resultado = statement.executeQuery(consulta)) {
            while (resultado.next()) {
                if (columna.equalsIgnoreCase(resultado.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean existeTabla(String tabla) throws SQLException {
        try (PreparedStatement preparacion = conexion.prepareStatement(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            preparacion.setString(1, tabla);
            try (ResultSet resultado = preparacion.executeQuery()) {
                return resultado.next() && resultado.getInt(1) > 0;
            }
        }
    }

    /**
     * Valida un identificador de tabla o columna: solo se admiten nombres
     * simples (letras, digitos y guion bajo), de modo que no se puedan
     * inyectar fragmentos de SQL.
     *
     * @param identificador nombre a validar
     * @return identificador validado
     */
    private static String identificadorSeguro(String identificador) {
        if (identificador == null || !identificador.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Identificador de SQL no valido: " + identificador);
        }
        return identificador;
    }

    // ───────────────────────────────────────────────────────────────────
    // Respaldo
    // ───────────────────────────────────────────────────────────────────

    /**
     * Genera el respaldo {@code pesaje.db.backup} junto a la base. La copia se
     * hace con {@code VACUUM INTO}, que produce un是一致的 aunque la base
     * este siendo modificada.
     *
     * @return ruta del archivo de respaldo
     */
    public Path generarBackup() {
        Path destino = rutaBase.resolveSibling(ARCHIVO_BACKUP);
        cerrojo.lock();
        try {
            Files.deleteIfExists(destino);
            try (PreparedStatement preparacion = conexion().prepareStatement("VACUUM INTO ?")) {
                preparacion.setString(1, destino.toString());
                preparacion.execute();
            }
            return destino;
        } catch (SQLException | IOException e) {
            // Respaldo de ultimo recurso: copia del archivo, que puede quedar
            // inconsistente si la base esta abierta.
            try {
                Files.copy(rutaBase, destino, StandardCopyOption.REPLACE_EXISTING);
                return destino;
            } catch (IOException errorCopia) {
                throw new ErrorDeBaseDeDatos("No se pudo generar el respaldo " + destino, errorCopia);
            }
        } finally {
            cerrojo.unlock();
        }
    }

    /**
     * @return ruta del archivo de respaldo de esta base
     */
    public Path rutaBackup() {
        return rutaBase.resolveSibling(ARCHIVO_BACKUP);
    }

    // ───────────────────────────────────────────────────────────────────
    // Semilla inicial
    // ───────────────────────────────────────────────────────────────────

    /**
     * Siembra el usuario administrador con una contrasena generada en el
     * primer arranque (decision D3: no existe contrasena por defecto).
     * El hash se calcula con {@link Passwords} (PBKDF2), que es el mismo
     * usado por la autenticacion.
     * La operacion es idempotente: si ya hay un administrador, no se hace nada.
     */
    private void sembrarAdministrador() {
        if (sql.contar("SELECT COUNT(*) FROM usuarios WHERE rol = ?", "administrador") > 0) {
            return;
        }
        String clave = generarClaveAleatoria();
        long personaId = sql.insertar(
                "INSERT INTO personas (nombre, apellido_paterno, correo, estado, fecha_creacion)"
                        + " VALUES (?, ?, ?, ?, ?)",
                "Administrador", "Sistema", EMAIL_ADMINISTRADOR, "ACTIVO", ahora());
        sql.insertar(
                "INSERT INTO usuarios (persona_id, email, password_hash, rol, estado, fecha_creacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                personaId, EMAIL_ADMINISTRADOR, Passwords.hash(clave), "administrador", "ACTIVO", ahora());
        claveAdministrador = clave;
        System.out.println("[SistemaPesaje] Usuario administrador creado (" + EMAIL_ADMINISTRADOR + ").");
        System.out.println("[SistemaPesaje] Contrasena inicial: " + clave + " (cambiela en el primer inicio).");
    }

    /**
     * Contrasena generada en el ultimo arranque con semilla de administrador.
     * Solo tiene valor justo despues del primer arranque.
     *
     * @return contrasena inicial o {@code null} si no se genero ninguna
     */
    public String claveAdministrador() {
        return claveAdministrador;
    }

    /**
     * Genera una contrasena aleatoria legible para el primer arranque.
     *
     * @return contrasena aleatoria
     */
    private static String generarClaveAleatoria() {
        StringBuilder clave = new StringBuilder(LONGITUD_CLAVE_GENERADA);
        for (int i = 0; i < LONGITUD_CLAVE_GENERADA; i++) {
            clave.append(ALFABETO_CLAVE.charAt(ALEATORIO.nextInt(ALFABETO_CLAVE.length())));
        }
        return clave.toString();
    }

    private static String ahora() {
        return LocalDateTime.now().format(FORMATO_FECHA);
    }

    // ───────────────────────────────────────────────────────────────────
    // Conexion
    // ───────────────────────────────────────────────────────────────────

    /**
     * @return conexion JDBC en uso (uso interno del paquete)
     */
    Connection conexion() {
        if (cerrada) {
            throw new ErrorDeBaseDeDatos("La base de datos " + rutaBase + " esta cerrada.");
        }
        return conexion;
    }

    /**
     * @return cerrojo que serializa el acceso a la conexion (uso interno)
     */
    ReentrantLock cerrojo() {
        return cerrojo;
    }

    /**
     * Cierra la conexion de forma ordenada. Es idempotente.
     */
    @Override
    public void close() {
        cerrojo.lock();
        try {
            if (cerrada) {
                return;
            }
            cerrada = true;
            try {
                if (!conexion.isClosed()) {
                    conexion.close();
                }
            } catch (SQLException e) {
                // No hay nada que recuperar: la conexion se descarta igual.
            }
        } finally {
            cerrojo.unlock();
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // Utilidades internas
    // ───────────────────────────────────────────────────────────────────

    private String leerRecursoEsquema() {
        try (InputStream entrada = Database.class.getResourceAsStream(RECURSO_ESQUEMA)) {
            if (entrada == null) {
                throw new ErrorDeBaseDeDatos("No se encontro el recurso de esquema " + RECURSO_ESQUEMA);
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(buffer)) > 0) {
                salida.write(buffer, 0, leidos);
            }
            return new String(salida.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ErrorDeBaseDeDatos("No se pudo leer el recurso de esquema " + RECURSO_ESQUEMA, e);
        }
    }

    /**
     * Divide un script SQL en sentencias, respetando literales, identificadores
     * entre comillas dobles y comentarios.
     *
     * @param sql script SQL
     * @return lista de sentencias
     */
    static List<String> dividirSentencias(String sql) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillaSimple = false;
        boolean enComillaDoble = false;
        boolean enAcento = false;
        boolean enComentarioLinea = false;
        boolean enComentarioBloque = false;

        for (int i = 0; i < sql.length(); i++) {
            char caracter = sql.charAt(i);
            char siguiente = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';

            if (enComentarioLinea) {
                if (caracter == '\n') {
                    enComentarioLinea = false;
                    actual.append(caracter);
                }
                continue;
            }
            if (enComentarioBloque) {
                if (caracter == '*' && siguiente == '/') {
                    enComentarioBloque = false;
                    i++;
                }
                continue;
            }
            if (!enComillaSimple && !enComillaDoble && !enAcento) {
                if (caracter == '-' && siguiente == '-') {
                    enComentarioLinea = true;
                    i++;
                    continue;
                }
                if (caracter == '/' && siguiente == '*') {
                    enComentarioBloque = true;
                    i++;
                    continue;
                }
                if (caracter == ';') {
                    agregarSiNoEstaVacia(sentencias, actual);
                    actual.setLength(0);
                    continue;
                }
            }
            if (caracter == '\'' && !enComillaDoble && !enAcento) {
                if (siguiente == '\'') {
                    actual.append(caracter).append(siguiente);
                    i++;
                    continue;
                }
                enComillaSimple = !enComillaSimple;
            } else if (caracter == '"' && !enComillaSimple && !enAcento) {
                enComillaDoble = !enComillaDoble;
            } else if (caracter == '`' && !enComillaSimple && !enComillaDoble) {
                enAcento = !enAcento;
            }
            actual.append(caracter);
        }
        agregarSiNoEstaVacia(sentencias, actual);
        return sentencias;
    }

    private static void agregarSiNoEstaVacia(List<String> sentencias, StringBuilder actual) {
        String sentencia = actual.toString().trim();
        if (!sentencia.isEmpty()) {
            sentencias.add(sentencia);
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // Migraciones
    // ───────────────────────────────────────────────────────────────────

    /**
     * Columna que debe existir en una tabla.
     */
    private static final class Columna {
        private final String tabla;
        private final String columna;
        private final String definicion;

        private Columna(String tabla, String columna, String definicion) {
            this.tabla = tabla;
            this.columna = columna;
            this.definicion = definicion;
        }
    }

    /**
     * Migracion versionada del esquema.
     */
    private static final class Migracion {
        private final int version;
        private final String descripcion;
        private final List<Columna> columnas;

        private Migracion(int version, String descripcion, List<Columna> columnas) {
            this.version = version;
            this.descripcion = descripcion;
            this.columnas = columnas;
        }
    }

    private static final List<Columna> COLUMNAS_SINCRONIZACION = Collections.unmodifiableList(
            Arrays.asList(
                    new Columna("pesajes", "sincronizado", "INTEGER NOT NULL DEFAULT 0"),
                    new Columna("pesajes", "sync_event_id", "TEXT"),
                    new Columna("pesajes", "sync_error", "TEXT")));

    private static final List<Columna> COLUMNAS_CATALOGOS = Collections.unmodifiableList(
            columnasDeCatalogos());

    private static List<Columna> columnasDeCatalogos() {
        List<Columna> columnas = new ArrayList<>();
        for (String catalogo : new String[]{
                "vehiculos", "choferes", "proveedores", "productos", "origenes", "destinos"}) {
            columnas.add(new Columna(catalogo, "version", "INTEGER NOT NULL DEFAULT 0"));
            columnas.add(new Columna(catalogo, "pendiente_push", "INTEGER NOT NULL DEFAULT 0"));
        }
        return columnas;
    }

    private static final List<Migracion> MIGRACIONES = Collections.unmodifiableList(
            Arrays.asList(
                    // Migracion 1: control de envio a la web y telefono de chofer.
                    new Migracion(1, "Columnas de sincronizacion de pesajes y telefono de chofer",
                            columnasSincronizacion()),
                    // Migracion 2: version de sincronizacion en los catalogos y
                    // origen de los pesajes (LWW con la web).
                    new Migracion(2, "Version de sincronizacion en catalogos y origen de pesajes",
                            columnasDeCatalogosConOrigen())));

    private static List<Columna> columnasSincronizacion() {
        List<Columna> columnas = new ArrayList<>(COLUMNAS_SINCRONIZACION);
        columnas.add(new Columna("choferes", "telefono", "TEXT"));
        return columnas;
    }

    private static List<Columna> columnasDeCatalogosConOrigen() {
        List<Columna> columnas = new ArrayList<>(COLUMNAS_CATALOGOS);
        columnas.add(new Columna("pesajes", "origen", "TEXT NOT NULL DEFAULT 'ESCRITORIO'"));
        columnas.add(new Columna("pesajes", "web_id", "INTEGER"));
        return columnas;
    }
}
