package bo.firmadigital.jacobitus.pesaje.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Configuracion del sistema, persistida en un `config.json` junto al
 * ejecutable.
 *
 * Cuando la carpeta del ejecutable no es escribible (instaladores en
 * /opt, AppImage, etc.) la configuracion se guarda en la carpeta de datos
 * del usuario, de modo que un fallo de escritura no se pierda en silencio.
 *
 * <p>La clave {@code modo_operacion} no define un segundo lanzador: la app
 * arranca siempre con {@code ./gradlew pesaje:run} o el binario
 * {@code sistema-pesaje}; el login abre el modo integrado y el shell
 * completo se alcanza desde el menu de esa ventana.
 */
public class ConfigSistema {

    private static final Logger LOG = Logger.getLogger(ConfigSistema.class.getName());

    public static final String ARCHIVO_CONFIG = "config.json";
    public static final String URL_POR_DEFECTO = "http://localhost:3000";
    public static final int TIMEOUT_HTTP_POR_DEFECTO = 4000;
    public static final int CICLO_CATALOGOS_POR_DEFECTO = 5;

    private String url = URL_POR_DEFECTO;
    private String apiKey = "";
    private boolean autoEnviar = true;
    private ModoOperacion modoOperacion = ModoOperacion.INTEGRADO;
    private boolean alwaysOnTop = false;
    private int timeoutHttp = TIMEOUT_HTTP_POR_DEFECTO;
    private int cicloCatalogos = CICLO_CATALOGOS_POR_DEFECTO;

    private boolean cargado = false;

    public ConfigSistema() {
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url == null ? URL_POR_DEFECTO : normalizarUrl(url);
    }

    @JsonProperty("apikey")
    public String getApiKey() {
        return apiKey;
    }

    @JsonProperty("apikey")
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @JsonProperty("autoenviar")
    public boolean isAutoEnviar() {
        return autoEnviar;
    }

    @JsonProperty("autoenviar")
    public void setAutoEnviar(boolean autoEnviar) {
        this.autoEnviar = autoEnviar;
    }

    @JsonProperty("modo_operacion")
    public ModoOperacion getModoOperacion() {
        return modoOperacion;
    }

    @JsonProperty("modo_operacion")
    public void setModoOperacion(ModoOperacion modoOperacion) {
        this.modoOperacion = modoOperacion == null ? ModoOperacion.INTEGRADO : modoOperacion;
    }

    @JsonProperty("alwaysontop")
    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    @JsonProperty("alwaysontop")
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    @JsonProperty("timeout_http")
    public int getTimeoutHttp() {
        return timeoutHttp;
    }

    @JsonProperty("timeout_http")
    public void setTimeoutHttp(int timeoutHttp) {
        this.timeoutHttp = timeoutHttp > 0 ? timeoutHttp : TIMEOUT_HTTP_POR_DEFECTO;
    }

    @JsonProperty("ciclo_catalogos")
    public int getCicloCatalogos() {
        return cicloCatalogos;
    }

    @JsonProperty("ciclo_catalogos")
    public void setCicloCatalogos(int cicloCatalogos) {
        this.cicloCatalogos = cicloCatalogos > 0 ? cicloCatalogos : CICLO_CATALOGOS_POR_DEFECTO;
    }

    @JsonIgnore
    public boolean isCargado() {
        return cargado;
    }

    /**
     * Carga la configuracion desde el archivo de configuracion. Si el
     * archivo no existe o esta corrupto se conservan los valores por
     * defecto y `cargado` queda en false.
     */
    public void cargar() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        File archivo = archivoConfiguracion();
        if (archivo == null || !archivo.exists()) {
            return;
        }
        try (InputStream entrada = new FileInputStream(archivo)) {
            ConfigSistema leida = mapper.readValue(entrada, ConfigSistema.class);
            aplicar(leida);
            cargado = true;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "No se pudo leer la configuracion: {0}", ex.getMessage());
        }
    }

    public void guardar() {
        ObjectMapper mapper = new ObjectMapper();
        File destino = archivoConfiguracionEscritura();
        if (destino == null) {
            return;
        }
        try {
            File carpeta = destino.getParentFile();
            if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
                LOG.log(Level.WARNING, "No se pudo crear la carpeta {0}", carpeta.getAbsolutePath());
                return;
            }
            try (OutputStream salida = new FileOutputStream(destino)) {
                mapper.writeValue(salida, this);
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "No se pudo guardar la configuracion: {0}", ex.getMessage());
        }
    }

    private void aplicar(ConfigSistema leida) {
        setUrl(leida.getUrl());
        setApiKey(leida.getApiKey());
        setAutoEnviar(leida.isAutoEnviar());
        setModoOperacion(leida.getModoOperacion());
        setAlwaysOnTop(leida.isAlwaysOnTop());
        setTimeoutHttp(leida.getTimeoutHttp());
        setCicloCatalogos(leida.getCicloCatalogos());
    }

    private static String normalizarUrl(String valor) {
        String resultado = valor.trim();
        while (resultado.endsWith("/")) {
            resultado = resultado.substring(0, resultado.length() - 1);
        }
        return resultado.isEmpty() ? URL_POR_DEFECTO : resultado;
    }

    /**
     * Ubicacion del archivo de configuracion. Se busca junto al ejecutable
     * y, en macOS, tambien dentro del bundle `.app`.
     */
    public static File archivoConfiguracion() {
        File juntoAlEjecutable = new File(carpetaEjecutable(), ARCHIVO_CONFIG);
        if (juntoAlEjecutable.exists()) {
            return juntoAlEjecutable;
        }
        File enBundle = archivoEnBundle();
        if (enBundle != null && enBundle.exists()) {
            return enBundle;
        }
        return juntoAlEjecutable;
    }

    private static File archivoConfiguracionEscritura() {
        File juntoAlEjecutable = new File(carpetaEjecutable(), ARCHIVO_CONFIG);
        if (esEscribible(juntoAlEjecutable)) {
            return juntoAlEjecutable;
        }
        File enBundle = archivoEnBundle();
        if (enBundle != null && esEscribible(enBundle)) {
            return enBundle;
        }
        File enUsuario = new File(carpetaUsuario(), ARCHIVO_CONFIG);
        return enUsuario;
    }

    private static File archivoEnBundle() {
        String rutaEjecutable = rutaEjecutable();
        int indice = rutaEjecutable.indexOf(".app/Contents/MacOS/");
        if (indice < 0) {
            return null;
        }
        String padre = rutaEjecutable.substring(0, indice);
        return new File(padre, ARCHIVO_CONFIG);
    }

    private static boolean esEscribible(File archivo) {
        File carpeta = archivo.getParentFile();
        return carpeta != null && carpeta.exists() && carpeta.canWrite();
    }

    private static String rutaEjecutable() {
        String ruta = System.getProperty("pesaje.ejecutable");
        if (ruta != null && !ruta.isEmpty()) {
            return ruta;
        }
        return System.getProperty("java.home") == null ? "" : System.getProperty("user.dir");
    }

    private static String carpetaEjecutable() {
        String ruta = rutaEjecutable();
        int indice = ruta.lastIndexOf(File.separatorChar);
        return indice < 0 ? ruta : ruta.substring(0, indice);
    }

    private static String carpetaUsuario() {
        String home = System.getProperty("user.home", ".");
        String so = System.getProperty("os.name", "").toLowerCase();
        if (so.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData != null ? appData + File.separator + "SistemaPesaje" : home + File.separator + "SistemaPesaje";
        }
        if (so.contains("mac") || so.contains("darwin")) {
            return home + File.separator + "Library" + File.separator + "Application Support" + File.separator + "SistemaPesaje";
        }
        return home + File.separator + ".local" + File.separator + "share" + File.separator + "SistemaPesaje";
    }
}
