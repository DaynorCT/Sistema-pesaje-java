package bo.firmadigital.jacobitus.escritorio.utilidades;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.utilidades.SistemaOperativoHelper;

public final class AutoInicioHelper {
    private static final Logger LOGGER = Logger.getLogger(AutoInicioHelper.class.getName());
    private static final String WINDOWS_RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String WINDOWS_VALUE_NAME = "Jacobitus Escritorio";

    private AutoInicioHelper() {}

    public static boolean estaSoportado() {
        return SistemaOperativoHelper.esWindows();
    }

    public static boolean estaActivado() {
        if (!estaSoportado()) {
            return false;
        }
        try {
            return obtenerComandoRegistradoWindows()
                    .map(comando -> normalizarComandoWindows(comando).equals(normalizarComandoWindows(obtenerComandoInicioWindows())))
                    .orElse(false);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "No se pudo consultar el inicio automatico", ex);
            return false;
        }
    }

    public static void activar() {
        if (!estaSoportado()) {
            throw new JacobitusException("El inicio automático aún no está disponible para este sistema operativo.");
        }
        ejecutarWindows("reg", "add", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE_NAME, "/t", "REG_SZ", "/d",
                obtenerComandoInicioWindows(), "/f");
    }

    public static void desactivar() {
        if (!estaSoportado()) {
            throw new JacobitusException("El inicio automático aún no está disponible para este sistema operativo.");
        }
        Optional<String> comandoRegistrado = obtenerComandoRegistradoWindows();
        if (!comandoRegistrado.isPresent()) {
            return;
        }
        ejecutarWindows("reg", "delete", WINDOWS_RUN_KEY, "/v", WINDOWS_VALUE_NAME, "/f");
    }

    private static String obtenerComandoInicioWindows() {
        File ejecutable = obtenerEjecutableActual();
        return "\"" + ejecutable.getAbsolutePath() + "\"";
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
            String[] partes = lineaLimpia.split("\\s+REG_SZ\\s+", 2);
            if (partes.length == 2) {
                return Optional.of(partes[1].trim());
            }
        }
        return Optional.empty();
    }

    private static String normalizarComandoWindows(String comando) {
        return comando.trim().replace('/', '\\').replaceAll("^\"|\"$", "").toLowerCase(Locale.ROOT);
    }

    private static void ejecutarWindows(String... comando) {
        ResultadoProceso resultado = ejecutarWindows(true, comando);
        if (resultado.codigoSalida != 0) {
            String mensaje = resultado.error.trim().isEmpty() ? resultado.salida.trim() : resultado.error.trim();
            throw new JacobitusException("No se pudo configurar el inicio automático. " + mensaje);
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
