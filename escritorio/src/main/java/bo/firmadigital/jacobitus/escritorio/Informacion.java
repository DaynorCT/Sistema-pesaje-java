package bo.firmadigital.jacobitus.escritorio;

public class Informacion {
    public static final String NOMBRE = "jacobitus-escritorio";
    public static final String VERSION = System.getProperty("jacobitus.version", "0.0.0");
    public static final String PROFILE = System.getProperty("jacobitus.profile", "").trim();
    public static final String DESCRIPCION = System.getProperty("jacobitus.descripcion", "");
    public static final Long COMPILACION = Long.getLong("jacobitus.compilacion", 0L);

    private Informacion() {}
}
