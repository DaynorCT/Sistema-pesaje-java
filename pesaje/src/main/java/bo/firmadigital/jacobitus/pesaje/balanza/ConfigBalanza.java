package bo.firmadigital.jacobitus.pesaje.balanza;

/**
 * Configuracion de la bascula.
 *
 * Es un POJO plano: no depende de la base de datos ni de JavaFX. El
 * mapeo hacia la tabla <code>config_balanza</code> es responsabilidad de la
 * capa de datos; aqui solo se.modelan los valores y sus valores por
 * defecto, que son los mismos que usaba el sistema original.
 *
 * Los valores por defecto son: COM1, 9600 baudios, 8 bits de datos,
 * paridad N (ninguna), 1 bit de parada, control de flujo None, timeout de
 * 1000 ms, metodo AUTO, posicion inicial 8 y longitud 5.
 */
public class ConfigBalanza {

    public static final String PUERTO_COM_POR_DEFECTO = "COM1";
    public static final int BAUD_RATE_POR_DEFECTO = 9600;
    public static final int DATA_BITS_POR_DEFECTO = 8;
    public static final String PARIDAD_POR_DEFECTO = "N";
    public static final int STOP_BITS_POR_DEFECTO = 1;
    public static final String FLOW_CONTROL_POR_DEFECTO = "None";
    public static final int TIMEOUT_MS_POR_DEFECTO = 1000;
    public static final int POSICION_INICIO_POR_DEFECTO = 8;
    public static final int POSICION_LONGITUD_POR_DEFECTO = 5;

    /** Paridad: ninguna. */
    public static final String PARIDAD_NINGUNA = "N";
    /** Paridad: par. */
    public static final String PARIDAD_PAR = "E";
    /** Paridad: impar. */
    public static final String PARIDAD_IMPAR = "O";

    private String puertoCom = PUERTO_COM_POR_DEFECTO;
    private int baudRate = BAUD_RATE_POR_DEFECTO;
    private int dataBits = DATA_BITS_POR_DEFECTO;
    private String paridad = PARIDAD_POR_DEFECTO;
    private int stopBits = STOP_BITS_POR_DEFECTO;
    private String flowControl = FLOW_CONTROL_POR_DEFECTO;
    private int timeoutMs = TIMEOUT_MS_POR_DEFECTO;
    private MetodoLectura metodoLectura = MetodoLectura.POR_DEFECTO;
    private int posicionInicio = POSICION_INICIO_POR_DEFECTO;
    private int posicionLongitud = POSICION_LONGITUD_POR_DEFECTO;

    public ConfigBalanza() {
        // Usa los valores por defecto.
    }

    /** Copia de otra configuracion. */
    public ConfigBalanza(ConfigBalanza otra) {
        if (otra != null) {
            this.puertoCom = otra.puertoCom;
            this.baudRate = otra.baudRate;
            this.dataBits = otra.dataBits;
            this.paridad = otra.paridad;
            this.stopBits = otra.stopBits;
            this.flowControl = otra.flowControl;
            this.timeoutMs = otra.timeoutMs;
            this.metodoLectura = otra.metodoLectura;
            this.posicionInicio = otra.posicionInicio;
            this.posicionLongitud = otra.posicionLongitud;
        }
    }

    /** @return una copia independiente de esta configuracion. */
    public ConfigBalanza copiar() {
        return new ConfigBalanza(this);
    }

    public String getPuertoCom() {
        return puertoCom;
    }

    public void setPuertoCom(String puertoCom) {
        this.puertoCom = puertoCom;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public String getParidad() {
        return paridad;
    }

    public void setParidad(String paridad) {
        this.paridad = paridad;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public String getFlowControl() {
        return flowControl;
    }

    public void setFlowControl(String flowControl) {
        this.flowControl = flowControl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public MetodoLectura getMetodoLectura() {
        return metodoLectura;
    }

    public void setMetodoLectura(MetodoLectura metodoLectura) {
        this.metodoLectura = metodoLectura == null ? MetodoLectura.POR_DEFECTO : metodoLectura;
    }

    /**
     * Fija el metodo de lectura a partir del texto persistido (AUTO / POSICION).
     *
     * <p>No se llama {@code setMetodoLectura(String)} a proposito: una
     * sobrecarga con {@code String} haria ambigua la llamada con un null.
     */
    public void setMetodoLecturaTexto(String metodoLectura) {
        this.metodoLectura = MetodoLectura.desdeTexto(metodoLectura);
    }

    public int getPosicionInicio() {
        return posicionInicio;
    }

    public void setPosicionInicio(int posicionInicio) {
        this.posicionInicio = posicionInicio;
    }

    public int getPosicionLongitud() {
        return posicionLongitud;
    }

    public void setPosicionLongitud(int posicionLongitud) {
        this.posicionLongitud = posicionLongitud;
    }

    @Override
    public String toString() {
        return "ConfigBalanza{puertoCom=" + puertoCom
                + ", baudRate=" + baudRate
                + ", dataBits=" + dataBits
                + ", paridad=" + paridad
                + ", stopBits=" + stopBits
                + ", flowControl=" + flowControl
                + ", timeoutMs=" + timeoutMs
                + ", metodoLectura=" + metodoLectura
                + ", posicionInicio=" + posicionInicio
                + ", posicionLongitud=" + posicionLongitud
                + "}";
    }
}
