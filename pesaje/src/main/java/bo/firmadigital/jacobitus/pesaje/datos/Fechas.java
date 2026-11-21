package bo.firmadigital.jacobitus.pesaje.datos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Fechas en el formato usado por la base SQLite del sistema. */
public final class Fechas {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Fechas() {
    }

    public static String ahora() {
        return LocalDateTime.now().format(FORMATO);
    }

    /**
     * Parte de fecha para mostrar en grillas (dd/MM/yyyy).
     *
     * @param fechaHora valor guardado como {@code yyyy-MM-dd HH:mm:ss}
     * @return fecha corta o cadena vacía
     */
    public static String soloFecha(String fechaHora) {
        if (fechaHora == null || fechaHora.length() < 10) {
            return "";
        }
        if (fechaHora.length() >= 16) {
            return fechaHora.substring(8, 10) + "/"
                    + fechaHora.substring(5, 7) + "/"
                    + fechaHora.substring(0, 4);
        }
        return fechaHora.substring(0, 10);
    }

    /**
     * Parte de hora para mostrar en grillas (HH:mm).
     *
     * @param fechaHora valor guardado como {@code yyyy-MM-dd HH:mm:ss}
     * @return hora corta o cadena vacía
     */
    public static String soloHora(String fechaHora) {
        if (fechaHora == null || fechaHora.length() < 16) {
            return "";
        }
        return fechaHora.substring(11, 16);
    }
}
