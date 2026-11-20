package bo.firmadigital.jacobitus.pesaje.sync;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Politica de reintentos y clasificacion de errores de la cola local de
 * sincronizacion.
 *
 * Es la replica de la politica del sistema original (Lazarus) con las
 * mismas reglas: espera creciente con tope, jitter para que varias
 * terminales no reintenten en el mismo instante, separacion entre
 * errores permanentes y reintentables, y resolucion de conflictos por
 * version (LWW) donde gana el servidor.
 */
public final class SyncPolicy {

    public static final int ESPERA_BASE_SEGUNDOS = 30;
    public static final int ESPERA_MAXIMA_SEGUNDOS = 300;
    public static final int ESPERA_MAXIMA_EXPONENTE = 6;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SyncPolicy() {
    }

    /**
     * Espera, en segundos, antes de reintentar un evento que fallo.
     * Crece (30s, 1m, 2m, 4m...) y tiene tope de 5 minutos.
     */
    public static int esperaBackoffSegundos(int intentos) {
        int exponente = Math.min(Math.max(intentos, 0), ESPERA_MAXIMA_EXPONENTE);
        int espera = ESPERA_BASE_SEGUNDOS * (1 << exponente);
        return Math.min(espera, ESPERA_MAXIMA_SEGUNDOS);
    }

    /**
     * Momento del proximo intento. El jitter se suma para que varias
     * terminales no reintenten todas en el mismo instante.
     */
    public static String calcularProximoIntento(int intentos, LocalDateTime ahora, int jitterSegundos) {
        long espera = esperaBackoffSegundos(intentos) + Math.max(jitterSegundos, 0);
        return FORMATO.format(ahora.plusSeconds(espera));
    }

    /**
     * Un error es permanente cuando reintentar no va a cambiar el
     * resultado: el servidor rechazo el dato. Esos quedan para que el
     * operador los revise en vez de quedar reintentando para siempre.
     * 401 se reintenta renovando el token y 429 es "mucha carga".
     */
    public static boolean esErrorPermanente(int codigo) {
        return codigo >= 400 && codigo < 500 && codigo != 401 && codigo != 429;
    }

    /** Un codigo 0 es un fallo de red: no hubo respuesta del servidor. */
    public static boolean esFalloDeRed(int codigo) {
        return codigo == 0;
    }

    /**
     * Los catalogos no hace falta bajarlos en cada vuelta: bajarlos en
     * cada vuelta multiplica el tiempo de espera cuando no hay senal.
     */
    public static boolean debeSincronizarCatalogos(int contador, int cada) {
        if (cada <= 1) {
            return true;
        }
        return (contador % cada) == 0;
    }

    /**
     * Que hay que hacer con una fila de catalogo que vino del servidor.
     *
     * NADA: el local es mas nuevo o igual, se respeta el local.
     * APLICAR: el servidor tiene una version mas nueva, se aplica.
     * CONFLICTO: el local tiene un cambio sin enviar y el servidor tambien
     * movio la fila; gana el servidor (regla del usuario) y el cambio local
     * queda registrado para que el operador lo vea.
     */
    public static AccionVersion resolverVersion(int versionServidor, int versionLocal, boolean pendientePush) {
        if (versionLocal < 0) {
            return AccionVersion.APLICAR;
        }
        if (versionServidor > versionLocal) {
            return pendientePush ? AccionVersion.CONFLICTO : AccionVersion.APLICAR;
        }
        return AccionVersion.NADA;
    }
}
