package bo.firmadigital.jacobitus.pesaje.balanza;

import java.util.Random;

/**
 * Fuente de peso SIMULADA.
 *
 * Es un doble de prueba: entrega tramas con el mismo formato que usa la
 * bascula real para poder ejercitar el flujo de pesaje sin hardware. En el
 * sistema original la simulacion estaba incrustada en la pantalla de pesaje
 * (un peso aleatorio en produccion); aqui vive en una clase aparte que
 * <strong>no debe instanciarse desde el codigo de produccion</strong>: la
 * aplicacion usa {@link LectorSerie} y las pruebas inyectan esta fuente.
 *
 * El rango de pesos (1000 a 5000 kg) es el mismo que usaba el original.
 */
public class FuentePesoSimulada implements LectorPeso {

    /** Peso minimo simulado, en kilos. */
    public static final int PESO_MINIMO = 1000;
    /** Peso maximo simulado, en kilos. */
    public static final int PESO_MAXIMO = 5000;

    private final Random aleatorio;
    private boolean conectada;
    private int ultimoPeso;

    public FuentePesoSimulada() {
        this(new Random());
    }

    /**
     * @param semilla semilla del generador; con la misma semilla la serie de
     *                pesos es siempre la misma
     */
    public FuentePesoSimulada(long semilla) {
        this(new Random(semilla));
    }

    public FuentePesoSimulada(Random aleatorio) {
        this.aleatorio = aleatorio == null ? new Random() : aleatorio;
    }

    @Override
    public boolean conectar() {
        conectada = true;
        return true;
    }

    @Override
    public void desconectar() {
        conectada = false;
    }

    @Override
    public boolean estaConectado() {
        return conectada;
    }

    @Override
    public String leerTrama() {
        if (!conectada) {
            return "";
        }
        ultimoPeso = PESO_MINIMO + aleatorio.nextInt(PESO_MAXIMO - PESO_MINIMO + 1);
        return "ST: " + ultimoPeso + " kg";
    }

    /** @return el ultimo peso simulado, en kilos. */
    public int getUltimoPeso() {
        return ultimoPeso;
    }
}
