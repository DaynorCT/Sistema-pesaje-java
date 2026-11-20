package bo.firmadigital.jacobitus.pesaje.balanza;

import java.util.Locale;

/**
 * Metodo de lectura del peso sobre la trama cruda de la bascula.
 *
 * AUTO: el parser escanea la trama y toma la primera secuencia numerica
 * utilizable (ver {@link ParserPeso}).
 *
 * POSICION: el parser toma la subcadena indicada por la configuracion
 * ({@link ConfigBalanza#getPosicionInicio()} y
 * {@link ConfigBalanza#getPosicionLongitud()}), con la misma base 1 que
 * usaba el sistema original.
 */
public enum MetodoLectura {

    AUTO,
    POSICION;

    /** Metodo aplicado cuando la configuracion no indica uno valido. */
    public static final MetodoLectura POR_DEFECTO = AUTO;

    /**
     * Traduce el texto persistido (AUTO / POSICION) al enumerado.
     *
     * @param texto texto de la configuracion; puede ser nulo
     * @return el metodo correspondiente o {@link #POR_DEFECTO} si no se reconoce
     */
    public static MetodoLectura desdeTexto(String texto) {
        if (texto == null) {
            return POR_DEFECTO;
        }
        String normalizado = texto.trim().toUpperCase(Locale.ROOT);
        for (MetodoLectura metodo : values()) {
            if (metodo.name().equals(normalizado)) {
                return metodo;
            }
        }
        return POR_DEFECTO;
    }

    /** @return el texto con el que se persiste el metodo. */
    public String texto() {
        return name();
    }
}
