package bo.firmadigital.jacobitus.pesaje.balanza;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registro de las tramas crudas leidas de la bascula.
 *
 * Guarda las ultimas tramas recibidas (con un tope configurable) para poder
 * revisar a mano lo que envio el equipo, igual que hacia el memo de datos
 * del sistema original. Las tramas tambien quedan en el log de la
 * aplicacion a nivel FINE.
 */
public class RegistroTramas {

    private static final Logger LOGGER = Logger.getLogger(RegistroTramas.class.getName());

    /** Cantidad maxima de tramas que se conservan por defecto. */
    public static final int MAXIMO_TRAMAS_POR_DEFECTO = 100;

    private final int maximoTramas;
    private final LinkedList<String> tramas = new LinkedList<String>();

    public RegistroTramas() {
        this(MAXIMO_TRAMAS_POR_DEFECTO);
    }

    /**
     * @param maximoTramas cantidad maxima de tramas que se conservan
     */
    public RegistroTramas(int maximoTramas) {
        this.maximoTramas = maximoTramas > 0 ? maximoTramas : MAXIMO_TRAMAS_POR_DEFECTO;
    }

    /**
     * Agrega una trama al registro.
     *
     * @param trama trama cruda recibida; se ignora si es nula
     */
    public void registrar(String trama) {
        if (trama == null) {
            return;
        }
        synchronized (tramas) {
            tramas.addLast(trama);
            while (tramas.size() > maximoTramas) {
                tramas.removeFirst();
            }
        }
        LOGGER.log(Level.FINE, "Trama de bascula: {0}", trama);
    }

    /** @return las tramas registradas, de la mas antigua a la mas reciente. */
    public List<String> tramas() {
        synchronized (tramas) {
            return new ArrayList<String>(tramas);
        }
    }

    /** @return la ultima trama registrada o null si no hay ninguna. */
    public String ultimaTrama() {
        synchronized (tramas) {
            return tramas.isEmpty() ? null : tramas.getLast();
        }
    }

    /** Descarta todas las tramas registradas. */
    public void limpiar() {
        synchronized (tramas) {
            tramas.clear();
        }
    }
}
