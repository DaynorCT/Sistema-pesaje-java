package bo.firmadigital.jacobitus.escritorio.utilidades;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.utilidades.SistemaOperativoHelper;

public final class AutoInicioHelper {
    private static final Logger LOGGER = Logger.getLogger(AutoInicioHelper.class.getName());
    public static final String ARGUMENTO_SEGUNDO_PLANO = "--segundo-plano";
    private static final String WINDOWS_RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String WINDOWS_STARTUP_APPROVED_RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run";
    private static final String WINDOWS_VALUE_NAME = "Jacobitus Escritorio";
    private static final String WINDOWS_STARTUP_APPROVED_DISABLED_BY_USER = "01";
    private static final String WINDOWS_STARTUP_APPROVED_DISABLED = "03";
    private static final String WINDOWS_STARTUP_APPROVED_DISABLED_FORCED = "09";
    private static final String LINUX_DESKTOP_FILE_NAME = "jacobitus-escritorio.desktop";
    private static final String MACOS_LAUNCH_AGENT_LABEL = "bo.firmadigital.jacobitus.escritorio";
    private static final String MACOS_LAUNCH_AGENT_FILE_NAME = MACOS_LAUNCH_AGENT_LABEL + ".plist";

    private AutoInicioHelper() {}

    public static boolean estaSoportado() {
        return SistemaOperativoHelper.esWindows()
                || SistemaOperativoHelper.esUnix()
                || SistemaOperativoHelper.esMacOS();
    }

    public static boolean estaActivado() {
        if (!estaSoportado()) {
            return false;
        }
        try {
            if (SistemaOperativoHelper.esWindows()) {
                return estaActivadoWindows();
            }
            if (SistemaOperativoHelper.esUnix()) {
                return estaActivadoLinux();
            }
            if (SistemaOperativoHelper.esMacOS()) {
                return estaActivadoMacOS();
            }
            return false;
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "No se pudo consultar el inicio automatico", ex);
            return false;
        }
    }

    public static void activar() {
        if (!estaSoportado()) {
            throw new JacobitusException("El inicio automático aún no está disponible para este sistema operativo.");
        }
        if (SistemaOperativoHelper.esWindows()) {
            ejecutarWindows("reg", "add", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE_NAME, "/t", "REG_SZ", "/d",
                    escaparDatoRegAddWindows(obtenerComandoInicioWindows()), "/f");
        } else if (SistemaOperativoHelper.esUnix()) {
            activarLinux();
        } else if (SistemaOperativoHelper.esMacOS()) {
            activarMacOS();
        }
    }

    public static void desactivar() {
        if (!estaSoportado()) {
            throw new JacobitusException("El inicio automático aún no está disponible para este sistema operativo.");
        }
        if (SistemaOperativoHelper.esWindows()) {
            Optional<String> comandoRegistrado = obtenerComandoRegistradoWindows();
            if (comandoRegistrado.isPresent()) {
                ejecutarWindows("reg", "delete", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE_NAME, "/f");
            }
            borrarValorRegistroWindowsSiExiste(WINDOWS_STARTUP_APPROVED_RUN_KEY, WINDOWS_VALUE_NAME);
        } else if (SistemaOperativoHelper.esUnix()) {
            desactivarLinux();
        } else if (SistemaOperativoHelper.esMacOS()) {
            desactivarMacOS();
        }
    }

    private static boolean estaActivadoWindows() {
        if (estaDesactivadoPorWindows()) {
            return false;
        }
        return obtenerComandoRegistradoWindows()
                .map(comando -> normalizarComandoWindows(comando).equals(normalizarComandoWindows(obtenerComandoInicioWindows())))
                .orElse(false);
    }

    private static boolean estaActivadoLinux() {
        File desktopFile = obtenerArchivoAutoinicioLinuxEfectivo();
        if (!desktopFile.exists()) {
            return false;
        }
        Properties propiedades = leerDesktopFile(desktopFile);
        if ("true".equalsIgnoreCase(propiedades.getProperty("Hidden"))) {
            return false;
        }
        if ("false".equalsIgnoreCase(propiedades.getProperty("X-GNOME-Autostart-enabled"))) {
            return false;
        }
        if (!estaPermitidoEnEscritorioLinux(propiedades)) {
            return false;
        }
        if (!tryExecDisponibleLinux(propiedades.getProperty("TryExec"))) {
            return false;
        }
        String exec = propiedades.getProperty("Exec");
        return exec != null && normalizarComandoUnix(exec).equals(normalizarComandoUnix(obtenerComandoInicioUnix()));
    }

    private static boolean estaActivadoMacOS() {
        File launchAgent = obtenerArchivoLaunchAgentMacOS();
        if (!launchAgent.exists() || estaDesactivadoPorMacOS()) {
            return false;
        }
        try {
            String contenido = new String(Files.readAllBytes(launchAgent.toPath()), StandardCharsets.UTF_8);
            if (!runAtLoadActivadoMacOS(contenido)) {
                return false;
            }
            return contenido.contains("<string>" + escaparXml(obtenerEjecutableActual().getAbsolutePath()) + "</string>");
        } catch (IOException ex) {
            throw new JacobitusException("No se pudo leer la configuración de inicio automático.");
        }
    }

    private static String obtenerComandoInicioWindows() {
        File ejecutable = obtenerEjecutableActual();
        String comando = "\"" + ejecutable.getAbsolutePath() + "\"";
        if (Config.getInstance().isTrayIconEnabled()) {
            comando += " " + ARGUMENTO_SEGUNDO_PLANO;
        }
        if (comando.length() > 260) {
            throw new JacobitusException("La ruta del ejecutable es demasiado larga para el inicio automático de Windows.");
        }
        return comando;
    }

    private static String obtenerComandoInicioUnix() {
        File ejecutable = obtenerEjecutableActual();
        String comando = formatearArgumentoDesktop(ejecutable.getAbsolutePath());
        if (Config.getInstance().isTrayIconEnabled()) {
            comando += " " + ARGUMENTO_SEGUNDO_PLANO;
        }
        return comando;
    }

    private static File obtenerEjecutableActual() {
        Optional<String> comando = ProcessHandle.current().info().command();
        if (comando.isPresent()) {
            File ejecutable = new File(comando.get());
            if (ejecutable.exists() && ejecutable.isFile()) {
                return ejecutable;
            }
        }
        throw new JacobitusException("No se pudo obtener la ruta del ejecutable de Jacobitus.");
    }

    private static Optional<String> obtenerComandoRegistradoWindows() {
        ResultadoProceso resultado = ejecutarWindows(false, "reg", "query", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE_NAME);
        if (resultado.codigoSalida != 0) {
            return Optional.empty();
        }
        String[] lineas = resultado.salida.split("\\R");
        for (String linea : lineas) {
            String lineaLimpia = linea.trim();
            if (!lineaLimpia.toLowerCase(Locale.ROOT).startsWith(WINDOWS_VALUE_NAME.toLowerCase(Locale.ROOT))) {
                continue;
            }
            Optional<String> valor = extraerDatoRegistroWindows(lineaLimpia, "REG_SZ");
            if (valor.isPresent()) {
                return valor;
            }
        }
        return Optional.empty();
    }

    private static boolean estaDesactivadoPorWindows() {
        Optional<String> estado = obtenerEstadoStartupApprovedWindows();
        if (!estado.isPresent()) {
            return false;
        }
        String primerByte = estado.get();
        if (WINDOWS_STARTUP_APPROVED_DISABLED_BY_USER.equals(primerByte)
                || WINDOWS_STARTUP_APPROVED_DISABLED.equals(primerByte)
                || WINDOWS_STARTUP_APPROVED_DISABLED_FORCED.equals(primerByte)) {
            return true;
        }
        return false;
    }

    private static Optional<String> obtenerEstadoStartupApprovedWindows() {
        ResultadoProceso resultado = ejecutarWindows(false, "reg", "query", WINDOWS_STARTUP_APPROVED_RUN_KEY, "/v",
                WINDOWS_VALUE_NAME);
        if (resultado.codigoSalida != 0) {
            return Optional.empty();
        }
        String[] lineas = resultado.salida.split("\\R");
        for (String linea : lineas) {
            String lineaLimpia = linea.trim();
            if (!lineaLimpia.toLowerCase(Locale.ROOT).startsWith(WINDOWS_VALUE_NAME.toLowerCase(Locale.ROOT))) {
                continue;
            }
            Optional<String> dato = extraerDatoRegistroWindows(lineaLimpia, "REG_BINARY");
            if (dato.isPresent()) {
                String valor = dato.get().replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
                if (valor.length() >= 2) {
                    return Optional.of(valor.substring(0, 2));
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> extraerDatoRegistroWindows(String linea, String tipoRegistro) {
        String tipo = " " + tipoRegistro.toUpperCase(Locale.ROOT) + " ";
        String lineaNormalizada = linea.replace('\t', ' ');
        String lineaMayuscula = lineaNormalizada.toUpperCase(Locale.ROOT);
        int indiceTipo = lineaMayuscula.indexOf(tipo);
        if (indiceTipo < 0) {
            return Optional.empty();
        }
        return Optional.of(lineaNormalizada.substring(indiceTipo + tipo.length()).trim());
    }

    private static String escaparDatoRegAddWindows(String valor) {
        return valor.replace("\"", "\\\"");
    }

    private static void borrarValorRegistroWindowsSiExiste(String clave, String nombre) {
        ejecutarWindows(false, "reg", "delete", clave, "/v", nombre, "/f");
    }

    private static String normalizarComandoWindows(String comando) {
        return comando.trim()
                .replace(" " + ARGUMENTO_SEGUNDO_PLANO, "")
                .replace('/', '\\')
                .replaceAll("^\"|\"$", "")
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizarComandoUnix(String comando) {
        return comando.trim()
                .replace(" " + ARGUMENTO_SEGUNDO_PLANO, "")
                .replaceAll("^\"|\"$", "");
    }

    private static void activarLinux() {
        File desktopFile = obtenerArchivoAutoinicioLinux();
        File parent = desktopFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new JacobitusException("No se pudo crear el directorio de inicio automático.");
        }
        String contenido = "[Desktop Entry]\n"
                + "Type=Application\n"
                + "Name=Jacobitus Escritorio\n"
                + "Comment=Inicia Jacobitus Escritorio al iniciar sesión\n"
                + "Exec=" + obtenerComandoInicioUnix() + "\n"
                + "Terminal=false\n";
        try {
            Files.write(desktopFile.toPath(), contenido.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new JacobitusException("No se pudo configurar el inicio automático.");
        }
    }

    private static void desactivarLinux() {
        File desktopFile = obtenerArchivoAutoinicioLinux();
        if (!desktopFile.exists()) {
            if (obtenerArchivoAutoinicioLinuxSistema().exists()) {
                escribirArchivoAutoinicioLinuxOculto(desktopFile);
            }
            return;
        }
        try {
            Files.delete(desktopFile.toPath());
            if (obtenerArchivoAutoinicioLinuxSistema().exists()) {
                escribirArchivoAutoinicioLinuxOculto(desktopFile);
            }
        } catch (IOException ex) {
            throw new JacobitusException("No se pudo desactivar el inicio automático.");
        }
    }

    private static File obtenerArchivoAutoinicioLinux() {
        String configHome = System.getenv("XDG_CONFIG_HOME");
        File base = configHome == null || configHome.trim().equals("")
                ? new File(System.getProperty("user.home"), ".config")
                : new File(configHome);
        return new File(new File(base, "autostart"), LINUX_DESKTOP_FILE_NAME);
    }

    private static File obtenerArchivoAutoinicioLinuxEfectivo() {
        File archivoUsuario = obtenerArchivoAutoinicioLinux();
        if (archivoUsuario.exists()) {
            return archivoUsuario;
        }
        return obtenerArchivoAutoinicioLinuxSistema();
    }

    private static File obtenerArchivoAutoinicioLinuxSistema() {
        String configDirs = System.getenv("XDG_CONFIG_DIRS");
        if (configDirs == null || configDirs.trim().equals("")) {
            configDirs = "/etc/xdg";
        }
        for (String configDir : configDirs.split(File.pathSeparator)) {
            if (configDir.trim().equals("")) {
                continue;
            }
            File archivo = new File(new File(configDir, "autostart"), LINUX_DESKTOP_FILE_NAME);
            if (archivo.exists()) {
                return archivo;
            }
        }
        return new File(new File("/etc/xdg", "autostart"), LINUX_DESKTOP_FILE_NAME);
    }

    private static void escribirArchivoAutoinicioLinuxOculto(File desktopFile) {
        File parent = desktopFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new JacobitusException("No se pudo crear el directorio de inicio automático.");
        }
        String contenido = "[Desktop Entry]\n"
                + "Type=Application\n"
                + "Name=Jacobitus Escritorio\n"
                + "Hidden=true\n";
        try {
            Files.write(desktopFile.toPath(), contenido.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new JacobitusException("No se pudo desactivar el inicio automático.");
        }
    }

    private static Properties leerDesktopFile(File file) {
        Properties propiedades = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String lineaLimpia = linea.trim();
                if (lineaLimpia.startsWith("#") || lineaLimpia.startsWith("[") || !lineaLimpia.contains("=")) {
                    continue;
                }
                int idx = lineaLimpia.indexOf('=');
                propiedades.setProperty(lineaLimpia.substring(0, idx), lineaLimpia.substring(idx + 1));
            }
            return propiedades;
        } catch (IOException ex) {
            throw new JacobitusException("No se pudo leer la configuración de inicio automático.");
        }
    }

    private static boolean estaPermitidoEnEscritorioLinux(Properties propiedades) {
        String escritoriosActuales = System.getenv("XDG_CURRENT_DESKTOP");
        String onlyShowIn = propiedades.getProperty("OnlyShowIn");
        String notShowIn = propiedades.getProperty("NotShowIn");
        if ((onlyShowIn == null || onlyShowIn.trim().equals(""))
                && (notShowIn == null || notShowIn.trim().equals(""))) {
            return true;
        }
        if (escritoriosActuales == null || escritoriosActuales.trim().equals("")) {
            return onlyShowIn == null || onlyShowIn.trim().equals("");
        }
        String[] actuales = escritoriosActuales.split(":");
        if (contieneEscritorioLinux(notShowIn, actuales)) {
            return false;
        }
        return onlyShowIn == null || onlyShowIn.trim().equals("") || contieneEscritorioLinux(onlyShowIn, actuales);
    }

    private static boolean contieneEscritorioLinux(String listaEscritorios, String[] actuales) {
        if (listaEscritorios == null || listaEscritorios.trim().equals("")) {
            return false;
        }
        String[] esperados = listaEscritorios.split(";");
        for (String actual : actuales) {
            for (String esperado : esperados) {
                if (actual.trim().equalsIgnoreCase(esperado.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tryExecDisponibleLinux(String tryExec) {
        if (tryExec == null || tryExec.trim().equals("")) {
            return true;
        }
        File archivo = new File(tryExec);
        if (archivo.isAbsolute()) {
            return archivo.exists() && archivo.canExecute();
        }
        String path = System.getenv("PATH");
        if (path == null || path.trim().equals("")) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            File ejecutable = new File(dir, tryExec);
            if (ejecutable.exists() && ejecutable.canExecute()) {
                return true;
            }
        }
        return false;
    }

    private static String formatearArgumentoDesktop(String argumento) {
        return "\"" + argumento
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("`", "\\`")
                .replace("$", "\\$")
                + "\"";
    }

    private static void activarMacOS() {
        File launchAgent = obtenerArchivoLaunchAgentMacOS();
        File parent = launchAgent.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new JacobitusException("No se pudo crear el directorio de inicio automático.");
        }
        StringBuilder argumentos = new StringBuilder();
        argumentos.append("        <string>")
                .append(escaparXml(obtenerEjecutableActual().getAbsolutePath()))
                .append("</string>\n");
        if (Config.getInstance().isTrayIconEnabled()) {
            argumentos.append("        <string>")
                    .append(ARGUMENTO_SEGUNDO_PLANO)
                    .append("</string>\n");
        }
        String contenido = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
                + "<plist version=\"1.0\">\n"
                + "<dict>\n"
                + "    <key>Label</key>\n"
                + "    <string>" + MACOS_LAUNCH_AGENT_LABEL + "</string>\n"
                + "    <key>ProgramArguments</key>\n"
                + "    <array>\n"
                + argumentos
                + "    </array>\n"
                + "    <key>RunAtLoad</key>\n"
                + "    <true/>\n"
                + "</dict>\n"
                + "</plist>\n";
        try {
            Files.write(launchAgent.toPath(), contenido.getBytes(StandardCharsets.UTF_8));
            ejecutarMacOS(false, "launchctl", "enable", "gui/" + obtenerUidMacOS() + "/" + MACOS_LAUNCH_AGENT_LABEL);
        } catch (IOException ex) {
            throw new JacobitusException("No se pudo configurar el inicio automático.");
        }
    }

    private static void desactivarMacOS() {
        ejecutarMacOS(false, "launchctl", "disable", "gui/" + obtenerUidMacOS() + "/" + MACOS_LAUNCH_AGENT_LABEL);
        Path path = obtenerArchivoLaunchAgentMacOS().toPath();
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.delete(path);
        } catch (IOException ex) {
            throw new JacobitusException("No se pudo desactivar el inicio automático.");
        }
    }

    private static File obtenerArchivoLaunchAgentMacOS() {
        return new File(new File(System.getProperty("user.home"), "Library/LaunchAgents"), MACOS_LAUNCH_AGENT_FILE_NAME);
    }

    private static boolean estaDesactivadoPorMacOS() {
        ResultadoProceso resultado = ejecutarMacOS(false, "launchctl", "print-disabled", "gui/" + obtenerUidMacOS());
        if (resultado.codigoSalida != 0) {
            return false;
        }
        String[] lineas = resultado.salida.split("\\R");
        for (String linea : lineas) {
            if (linea.contains("\"" + MACOS_LAUNCH_AGENT_LABEL + "\"") && linea.contains("=> true")) {
                return true;
            }
        }
        return false;
    }

    private static boolean runAtLoadActivadoMacOS(String contenido) {
        int indiceRunAtLoad = contenido.indexOf("<key>RunAtLoad</key>");
        if (indiceRunAtLoad < 0) {
            return false;
        }
        int indiceTrue = contenido.indexOf("<true/>", indiceRunAtLoad);
        int indiceFalse = contenido.indexOf("<false/>", indiceRunAtLoad);
        return indiceTrue >= 0 && (indiceFalse < 0 || indiceTrue < indiceFalse);
    }

    private static String obtenerUidMacOS() {
        ResultadoProceso resultado = ejecutarMacOS(false, "id", "-u");
        if (resultado.codigoSalida == 0 && !resultado.salida.trim().equals("")) {
            return resultado.salida.trim();
        }
        return System.getProperty("user.name");
    }

    private static String escaparXml(String valor) {
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void ejecutarWindows(String... comando) {
        ResultadoProceso resultado = ejecutarWindows(true, comando);
        if (resultado.codigoSalida != 0) {
            String mensaje = resultado.error.trim().isEmpty() ? resultado.salida.trim() : resultado.error.trim();
            throw new JacobitusException("No se pudo configurar el inicio automático. " + mensaje);
        }
    }

    private static ResultadoProceso ejecutarMacOS(boolean registrarError, String... comando) {
        try {
            Process process = new ProcessBuilder(comando).redirectErrorStream(false).start();
            String salida = leer(process.getInputStream());
            String error = leer(process.getErrorStream());
            int codigoSalida = process.waitFor();
            return new ResultadoProceso(codigoSalida, salida, error);
        } catch (IOException ex) {
            if (registrarError) {
                LOGGER.log(Level.WARNING, "No se pudo ejecutar el comando de inicio automatico", ex);
            }
            return new ResultadoProceso(1, "", ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new JacobitusException("Se interrumpió la configuración de inicio automático.");
        }
    }

    private static ResultadoProceso ejecutarWindows(boolean registrarError, String... comando) {
        try {
            Process process = new ProcessBuilder(comando).redirectErrorStream(false).start();
            String salida = leer(process.getInputStream());
            String error = leer(process.getErrorStream());
            int codigoSalida = process.waitFor();
            return new ResultadoProceso(codigoSalida, salida, error);
        } catch (IOException ex) {
            if (registrarError) {
                LOGGER.log(Level.WARNING, "No se pudo ejecutar el comando de inicio automatico", ex);
            }
            throw new JacobitusException("No se pudo ejecutar el comando de configuración de Windows.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new JacobitusException("Se interrumpió la configuración de inicio automático.");
        }
    }

    private static String leer(java.io.InputStream inputStream) throws IOException {
        StringBuilder contenido = new StringBuilder();
        Charset charset = Charset.defaultCharset();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                contenido.append(linea).append(System.lineSeparator());
            }
        }
        return contenido.toString();
    }

    private static class ResultadoProceso {
        private final int codigoSalida;
        private final String salida;
        private final String error;

        private ResultadoProceso(int codigoSalida, String salida, String error) {
            this.codigoSalida = codigoSalida;
            this.salida = salida;
            this.error = error;
        }
    }
}
