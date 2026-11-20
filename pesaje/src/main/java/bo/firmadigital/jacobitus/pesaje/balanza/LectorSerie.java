package bo.firmadigital.jacobitus.pesaje.balanza;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fazecast.jSerialComm.SerialPort;

/**
 * Lector de basculas por puerto serie (RS232) basado en jSerialComm.
 *
 * Reemplaza al bloque {@code TBlockSerial} del sistema original: abre el
 * puerto, aplica los parametros de la {@link ConfigBalanza} y entrega la
 * trama cruda que recibe. Un puerto inexistente, sin permisos o ya ocupado
 * no debe romper la aplicacion: en ese caso {@link #conectar()} devuelve
 * false, el motivo queda en {@link #ultimoError()} y el lector queda
 * desconectado.
 */
public class LectorSerie implements LectorPeso {

    private static final Logger LOGGER = Logger.getLogger(LectorSerie.class.getName());

    /** Tamano del buffer de lectura. */
    private static final int TAMANO_BUFFER = 512;
    /** Espera entre intentos de lectura, en milisegundos. */
    private static final long INTERVALO_LECTURA_MS = 20L;

    private final ConfigBalanza configuracion;
    private final RegistroTramas registroTramas;

    private SerialPort puerto;
    private String ultimoError = "";

    public LectorSerie(ConfigBalanza configuracion) {
        this(configuracion, new RegistroTramas());
    }

    /**
     * @param configuracion configuracion de la bascula; no puede ser nula
     * @param registroTramas registro donde quedan las tramas leidas
     */
    public LectorSerie(ConfigBalanza configuracion, RegistroTramas registroTramas) {
        if (configuracion == null) {
            throw new IllegalArgumentException("La configuracion de la bascula no puede ser nula.");
        }
        this.configuracion = configuracion;
        this.registroTramas = registroTramas == null ? new RegistroTramas() : registroTramas;
    }

    @Override
    public boolean conectar() {
        desconectar();
        ultimoError = "";

        String nombre = configuracion.getPuertoCom();
        if (nombre == null || nombre.trim().isEmpty()) {
            ultimoError = "No hay puerto serie configurado.";
            return false;
        }
        String puertoBuscado = nombre.trim();

        puerto = buscarPuerto(puertoBuscado);
        if (puerto == null) {
            ultimoError = "No se encontro el puerto serie '" + puertoBuscado + "' en este equipo.";
            LOGGER.log(Level.WARNING, ultimoError);
            return false;
        }

        if (!aplicarParametros()) {
            ultimoError = "No se pudieron aplicar los parametros de la bascula al puerto '"
                    + puertoBuscado + "'.";
            LOGGER.log(Level.WARNING, ultimoError);
            cerrarPuerto();
            return false;
        }

        if (!puerto.openPort()) {
            ultimoError = "No se pudo abrir el puerto '" + puertoBuscado
                    + "'. Verifique los permisos o que no este siendo usado por otro programa.";
            LOGGER.log(Level.WARNING, ultimoError);
            cerrarPuerto();
            return false;
        }

        int timeout = Math.max(1, configuracion.getTimeoutMs());
        puerto.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, 0);

        LOGGER.log(Level.INFO, "Conectado al puerto serie {0} ({1} baudios, {2} bits, paridad {3})",
                new Object[] { puertoBuscado, configuracion.getBaudRate(), configuracion.getDataBits(),
                        configuracion.getParidad() });
        return true;
    }

    @Override
    public void desconectar() {
        cerrarPuerto();
    }

    @Override
    public boolean estaConectado() {
        return puerto != null && puerto.isOpen();
    }

    @Override
    public String ultimoError() {
        return ultimoError;
    }

    @Override
    public String leerTrama() {
        if (!estaConectado()) {
            return "";
        }

        byte[] buffer = new byte[TAMANO_BUFFER];
        StringBuilder trama = new StringBuilder();
        long limite = System.currentTimeMillis() + Math.max(1, configuracion.getTimeoutMs());

        try {
            while (System.currentTimeMillis() < limite) {
                int disponibles = puerto.bytesAvailable();
                if (disponibles > 0) {
                    int leidos = puerto.readBytes(buffer, Math.min(disponibles, buffer.length));
                    if (leidos > 0) {
                        trama.append(new String(buffer, 0, leidos, StandardCharsets.US_ASCII));
                        break;
                    }
                } else {
                    Thread.sleep(INTERVALO_LECTURA_MS);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Error leyendo el puerto serie", ex);
            ultimoError = "Error leyendo el puerto serie: " + ex.getMessage();
        }

        String resultado = trama.toString();
        if (!resultado.isEmpty()) {
            registroTramas.registrar(resultado);
        }
        return resultado;
    }

    /** @return el registro donde quedan las tramas leidas. */
    public RegistroTramas getRegistroTramas() {
        return registroTramas;
    }

    private void cerrarPuerto() {
        if (puerto != null) {
            try {
                if (puerto.isOpen()) {
                    puerto.closePort();
                }
            } catch (RuntimeException ex) {
                LOGGER.log(Level.WARNING, "No se pudo cerrar el puerto serie", ex);
            }
            puerto = null;
        }
    }

    private SerialPort buscarPuerto(String nombre) {
        for (SerialPort disponible : SerialPort.getCommPorts()) {
            if (nombre.equalsIgnoreCase(disponible.getSystemPortName())
                    || nombre.equalsIgnoreCase(disponible.getDescriptivePortName())) {
                return disponible;
            }
        }
        return null;
    }

    private boolean aplicarParametros() {
        boolean ok = puerto.setBaudRate(configuracion.getBaudRate());
        ok &= puerto.setNumDataBits(configuracion.getDataBits());
        ok &= puerto.setParity(paridadJSerialComm(configuracion.getParidad()));
        ok &= puerto.setNumStopBits(stopBitsJSerialComm(configuracion.getStopBits()));
        ok &= puerto.setFlowControl(flowControlJSerialComm(configuracion.getFlowControl()));
        return ok;
    }

    /**
     * Traduce la paridad de la configuracion (N, E, O, M, S) a jSerialComm.
     */
    static int paridadJSerialComm(String paridad) {
        if (paridad == null) {
            return SerialPort.NO_PARITY;
        }
        switch (paridad.trim().toUpperCase(Locale.ROOT)) {
            case ConfigBalanza.PARIDAD_PAR:
                return SerialPort.EVEN_PARITY;
            case ConfigBalanza.PARIDAD_IMPAR:
                return SerialPort.ODD_PARITY;
            case "M":
                return SerialPort.MARK_PARITY;
            case "S":
                return SerialPort.SPACE_PARITY;
            case ConfigBalanza.PARIDAD_NINGUNA:
            default:
                return SerialPort.NO_PARITY;
        }
    }

    /**
     * Traduce los bits de parada de la configuracion a jSerialComm.
     *
     * Se conserva la codificacion del sistema original: 1 -&gt; un bit,
     * 2 -&gt; dos bits y 3 -&gt; un bit y medio.
     */
    static int stopBitsJSerialComm(int stopBits) {
        switch (stopBits) {
            case 2:
                return SerialPort.TWO_STOP_BITS;
            case 3:
                return SerialPort.ONE_POINT_FIVE_STOP_BITS;
            case 1:
            default:
                return SerialPort.ONE_STOP_BIT;
        }
    }

    /**
     * Traduce el control de flujo de la configuracion a jSerialComm.
     */
    static int flowControlJSerialComm(String flowControl) {
        if (flowControl == null) {
            return SerialPort.FLOW_CONTROL_DISABLED;
        }
        String normalizado = flowControl.trim().toUpperCase(Locale.ROOT);
        if (normalizado.contains("RTS") || normalizado.contains("CTS")) {
            return SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED;
        }
        if (normalizado.contains("XON") || normalizado.contains("XOFF")) {
            return SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED | SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED;
        }
        if (normalizado.contains("DTR")) {
            return SerialPort.FLOW_CONTROL_DTR_ENABLED;
        }
        if (normalizado.contains("DSR")) {
            return SerialPort.FLOW_CONTROL_DSR_ENABLED;
        }
        return SerialPort.FLOW_CONTROL_DISABLED;
    }
}
