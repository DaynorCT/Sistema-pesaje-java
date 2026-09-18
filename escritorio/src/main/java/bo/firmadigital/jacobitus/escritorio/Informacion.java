package bo.firmadigital.jacobitus.escritorio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Informacion {
    public static final String NOMBRE = "jacobitus-escritorio";
    public static final String VERSION = System.getProperty("jacobitus.version", "0.0.0");
    public static final String PROFILE = System.getProperty("jacobitus.profile", "").trim();
    public static final String DESCRIPCION = obtenerPropiedad("descripcion");
    public static final Long COMPILACION = Long.getLong("jacobitus.compilacion", 0L);

    private Informacion() {}

    private static String obtenerPropiedad(String nombre) {
        String valor = System.getProperty("jacobitus." + nombre);
        if (valor != null) {
            return valor;
        }
        Properties propiedades = new Properties();
        try (InputStream inputStream = Informacion.class.getClassLoader()
                .getResourceAsStream("jacobitus.properties")) {
            if (inputStream == null) {
                return "";
            }
            propiedades.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return propiedades.getProperty(nombre, "");
        } catch (IOException ex) {
            return "";
        }
    }
}
