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
}
