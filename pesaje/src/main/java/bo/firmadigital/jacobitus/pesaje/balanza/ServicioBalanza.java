package bo.firmadigital.jacobitus.pesaje.balanza;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio de lectura en vivo del peso de la bascula.
 *
 * La lectura del puerto serie es bloqueante, asi que corre en un hilo
 * propio: el hilo de la interfaz recibe el peso por observador y no se
 * bloquea nunca. El servicio no depende de JavaFX, de modo que se puede
 * ejercitar en pruebas inyectando otra {@link LectorPeso}.
 *
 * El servicio no crea hilos si no se llama a {@link #conectar()}; al
 * cerrarlo se detiene el planificador.
 */
public final class ServicioBalanza implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(ServicioBalanza.class.getName());

    /** Espera minima entre lecturas, en milisegundos. */
    public static final long INTERVALO_LECTURA_MS = 200L;

    /**
     * Receptor de las lecturas del servicio.
     */
    public interface Observador {

        /**
         * Se invoca cuando hay una lectura de peso valida.
         *
         * @param peso peso en kilos
         */
        void pesoActualizado(int peso);

        /**
         * Se invoca cuando cambia el estado de la conexion.
         *
         * @param conectado true si la bascula esta conectada
         * @param mensaje   descripcion del estado o del error
         */
        void estadoCambiado(boolean conectado, String mensaje);
    }

    private final LectorPeso lector;
    private final ParserPeso parser;
    private final CopyOnWriteArrayList<Observador> observadores = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService planificador;

    private volatile ScheduledFuture tarea;
    private volatile int ultimoPeso;
    private volatile boolean conectado;

    /**
     * Crea el servicio con la lectura real por puerto serie.
     *
     * @param configuracion configuracion de la bascula
     */
    public ServicioBalanza(ConfigBalanza configuracion) {
        this(new LectorSerie(configuracion), new ParserPeso(configuracion));
    }

    /**
     * Crea el servicio con un lector inyectado (para pruebas).
     *
     * @param lector  lector de peso
     * @param parser  parser de la trama
     */
    public ServicioBalanza(LectorPeso lector, ParserPeso parser) {
        if (lector == null || parser == null) {
            throw new IllegalArgumentException("El lector y el parser no pueden ser nulos.");
        }
        this.lector = lector;
        this.parser = parser;
        this.planificador = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "pesaje-balanza");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    /**
     * Registra un observador de las lecturas.
     *
     * @param observador observador a registrar
     */
    public void agregarObservador(Observador observador) {
        if (observador != null) {
            observadores.addIfAbsent(observador);
        }
    }

    /**
     * Quita un observador.
     *
     * @param observador observador a quitar
     */
    public void quitarObservador(Observador observador) {
        observadores.remove(observador);
    }

    /**
     * Conecta la bascula y empieza a leer.
     *
     * @return true si la conexion quedo establecida
     */
    public boolean conectar() {
        if (conectado) {
            return true;
        }
        boolean ok = lector.conectar();
        conectado = ok;
        if (!ok) {
            avisarEstado(false, lector.ultimoError());
            return false;
        }
        tarea = planificador.scheduleWithFixedDelay(this::leerUnaVez, 0,
                INTERVALO_LECTURA_MS, TimeUnit.MILLISECONDS);
        avisarEstado(true, "Balanza conectada.");
        return true;
    }

    /**
     * Desconecta la bascula y detiene la lectura.
     */
    public void desconectar() {
        ScheduledFuture actual = tarea;
        if (actual != null) {
            actual.cancel(false);
            tarea = null;
        }
        lector.desconectar();
        conectado = false;
        avisarEstado(false, "Balanza desconectada.");
    }

    /** @return true si la bascula esta conectada. */
    public boolean estaConectado() {
        return conectado && lector.estaConectado();
    }

    /** @return el ultimo peso leido, en kilos. */
    public int getUltimoPeso() {
        return ultimoPeso;
    }

    /** @return el ultimo error informado por el lector. */
    public String ultimoError() {
        return lector.ultimoError();
    }

    /** @return el parser usado por el servicio. */
    public ParserPeso getParser() {
        return parser;
    }

    private void leerUnaVez() {
        if (!estaConectado()) {
            return;
        }
        try {
            String trama = lector.leerTrama();
            if (trama == null || trama.isEmpty()) {
                return;
            }
            Optional<Integer> peso = parser.parsear(trama);
            if (peso.isPresent()) {
                ultimoPeso = peso.get();
                for (Observador observador : observadores) {
                    observador.pesoActualizado(ultimoPeso);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Error en la lectura del peso", e);
            desconectar();
            avisarEstado(false, "Se perdió la conexión con la balanza: " + e.getMessage());
        }
    }

    private void avisarEstado(boolean estaConectado, String mensaje) {
        for (Observador observador : observadores) {
            observador.estadoCambiado(estaConectado, mensaje);
        }
    }

    /**
     * Detiene el servicio y libera el hilo de lectura.
     */
    @Override
    public void close() {
        desconectar();
        planificador.shutdownNow();
        observadores.clear();
    }
}
