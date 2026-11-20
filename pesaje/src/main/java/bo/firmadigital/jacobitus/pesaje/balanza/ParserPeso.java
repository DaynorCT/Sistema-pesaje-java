package bo.firmadigital.jacobitus.pesaje.balanza;

import java.util.Optional;

/**
 * Parser unico del peso leido de la bascula.
 *
 * Recibe la trama cruda y devuelve el peso en kilos enteros. El metodo de
 * lectura sale siempre de la {@link ConfigBalanza}: no hay heuristicas
 * internas ni metodos que se apliquen por debajo de la configuracion.
 *
 * <h2>Metodo AUTO</h2>
 * Escanea la trama de izquierda a derecha y toma la primera secuencia
 * numerica utilizable: una tanda de digitos, con una parte decimal opcional
 * (un punto seguido de digitos). Se ignoran las secuencias de un solo
 * digito porque no son una lectura de peso creible (mismo criterio de
 * descarte que usaba el metodo automatico del sistema original).
 *
 * <h2>Metodo POSICION</h2>
 * Toma la ventana de {@link ConfigBalanza#getPosicionInicio()} (base 1,
 * igual que el original) con longitud
 * {@link ConfigBalanza#getPosicionLongitud()}. Si la ventana se sale de la
 * trama o no contiene un numero, no hay lectura.
 *
 * <h2>Conversion a kilos</h2>
 * El peso se expresa siempre en kilos enteros, como en el modelo de datos
 * original (<code>peso_bruto</code>, <code>tara</code> y <code>peso_neto</code>
 * son INTEGER). Cuando la trama trae decimales se conserva la parte entera.
 * El signo se ignora: se toma la magnitud del peso, igual que hacia el
 * sistema original.
 */
public class ParserPeso {

    /** Numero minimo de digitos para considerar que una secuencia es un peso. */
    static final int MINIMO_DIGITOS = 2;

    private final ConfigBalanza configuracion;

    /**
     * @param configuracion configuracion de la bascula; no puede ser nula
     */
    public ParserPeso(ConfigBalanza configuracion) {
        if (configuracion == null) {
            throw new IllegalArgumentException("La configuracion de la bascula no puede ser nula.");
        }
        this.configuracion = configuracion;
    }

    public ConfigBalanza getConfiguracion() {
        return configuracion;
    }

    /**
     * Interpreta la trama cruda segun el metodo de lectura configurado.
     *
     * @param trama trama recibida de la bascula; puede ser nula
     * @return el peso en kilos o vacio si la trama no contiene una lectura
     */
    public Optional<Integer> parsear(String trama) {
        if (trama == null) {
            return Optional.empty();
        }
        if (configuracion.getMetodoLectura() == MetodoLectura.POSICION) {
            return parsearPorPosicion(trama);
        }
        return parsearAutomatico(trama);
    }

    /**
     * Devuelve el peso en kilos de la ultima trama parseada, o 0 si no hay
     * lectura valida.
     *
     * @param trama trama recibida de la bascula; puede ser nula
     * @return el peso en kilos o 0
     */
    public int pesoEnKilos(String trama) {
        return parsear(trama).orElse(0);
    }

    /**
     * Lectura automatica: primera secuencia numerica utilizable de la trama.
     */
    Optional<Integer> parsearAutomatico(String trama) {
        int indice = 0;
        while (indice < trama.length()) {
            if (!esDigito(trama.charAt(indice))) {
                indice++;
                continue;
            }
            int fin = indice;
            while (fin < trama.length() && esDigito(trama.charAt(fin))) {
                fin++;
            }
            // Parte decimal opcional: un punto seguido de al menos un digito.
            if (fin < trama.length() && trama.charAt(fin) == '.'
                    && fin + 1 < trama.length() && esDigito(trama.charAt(fin + 1))) {
                fin++;
                while (fin < trama.length() && esDigito(trama.charAt(fin))) {
                    fin++;
                }
            }
            String secuencia = trama.substring(indice, fin);
            if (cuentaDigitos(secuencia) >= MINIMO_DIGITOS) {
                return aKilos(secuencia);
            }
            indice = fin;
        }
        return Optional.empty();
    }

    /**
     * Lectura por posicion: ventana fija dentro de la trama.
     */
    Optional<Integer> parsearPorPosicion(String trama) {
        int inicio = configuracion.getPosicionInicio();
        int longitud = configuracion.getPosicionLongitud();
        if (inicio < 1 || longitud < 1) {
            return Optional.empty();
        }
        if (inicio + longitud - 1 > trama.length()) {
            return Optional.empty();
        }
        String ventana = trama.substring(inicio - 1, inicio - 1 + longitud).trim();
        return aKilos(ventana);
    }

    /**
     * Convierte un texto numerico a kilos enteros.
     *
     * @param token texto candidato; puede ser nulo
     * @return el peso en kilos o vacio si el texto no es un numero valido
     */
    static Optional<Integer> aKilos(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String valor = token.trim();
        if (valor.isEmpty()) {
            return Optional.empty();
        }
        int punto = valor.indexOf('.');
        String enteros = punto >= 0 ? valor.substring(0, punto) : valor;
        String decimales = punto >= 0 ? valor.substring(punto + 1) : "";
        if (enteros.isEmpty() || !esSoloDigitos(enteros)) {
            return Optional.empty();
        }
        if (!decimales.isEmpty() && !esSoloDigitos(decimales)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(enteros));
        } catch (NumberFormatException ex) {
            // Valor fuera del rango de un entero de 32 bits: no hay lectura.
            return Optional.empty();
        }
    }

    private static boolean esDigito(char caracter) {
        return caracter >= '0' && caracter <= '9';
    }

    private static boolean esSoloDigitos(String texto) {
        if (texto.isEmpty()) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            if (!esDigito(texto.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int cuentaDigitos(String texto) {
        int digitos = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (esDigito(texto.charAt(i))) {
                digitos++;
            }
        }
        return digitos;
    }
}
