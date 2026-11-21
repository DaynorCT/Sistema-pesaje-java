package bo.firmadigital.jacobitus.pesaje.datos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Manejo del logo de la empresa.
 *
 * El logo se guarda como data URL (base64) porque asi queda en la columna
 * {@code logo} de la tabla {@code empresas} y asi lo lee la boleta en PDF.
 * El tipo de la imagen se deduce de la firma binaria del contenido (PNG o
 * JPEG), nunca de la extension del archivo.
 */
public final class LogoEmpresa {

    /** Tope del archivo de logo: 2 MB. */
    public static final long LIMITE_BYTES = 2L * 1024 * 1024;

    /** Firma binaria de un archivo PNG. */
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};

    /** Firma binaria de un archivo JPEG. */
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /** Marca que separa el prefijo del data URL de su contenido base64. */
    private static final String MARCA_BASE64 = ";base64,";

    /** Prefijo del data URL de un PNG. */
    private static final String PREFIJO_PNG = "data:image/png;base64,";

    /** Prefijo del data URL de un JPEG. */
    private static final String PREFIJO_JPEG = "data:image/jpeg;base64,";

    /**
     * Logo del sistema, en los recursos del modulo.
     *
     * Viene de {@code assets/images/logo-pesaje.jpg} del repositorio y es
     * la misma imagen que muestra el login, la cabecera, el modo bascula y
     * la boleta.
     */
    public static final String RECURSO_SISTEMA = "/logo-pesaje.jpg";

    private LogoEmpresa() {
    }

    /**
     * Logo del sistema como data URL.
     *
     * @return data URL del logo del sistema, o cadena vacia si el recurso
     *         no esta disponible
     */
    public static String dataUrlDelSistema() {
        try (InputStream entrada = LogoEmpresa.class.getResourceAsStream(RECURSO_SISTEMA)) {
            if (entrada == null) {
                return "";
            }
            byte[] bytes = entrada.readAllBytes();
            if (bytes.length == 0) {
                return "";
            }
            if (empiezaCon(bytes, FIRMA_PNG)) {
                return PREFIJO_PNG + Base64.getEncoder().encodeToString(bytes);
            }
            if (empiezaCon(bytes, FIRMA_JPEG)) {
                return PREFIJO_JPEG + Base64.getEncoder().encodeToString(bytes);
            }
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Lee un archivo de imagen y lo devuelve como data URL.
     *
     * @param archivo archivo del logo
     * @return data URL con la imagen codificada en base64
     * @throws IOException si el archivo no se puede leer
     */
    public static String dataUrl(Path archivo) throws IOException {
        if (Files.size(archivo) > LIMITE_BYTES) {
            throw new IllegalArgumentException("El archivo debe ser menor a 2MB.");
        }
        byte[] bytes = Files.readAllBytes(archivo);
        String prefijo;
        if (empiezaCon(bytes, FIRMA_PNG)) {
            prefijo = PREFIJO_PNG;
        } else if (empiezaCon(bytes, FIRMA_JPEG)) {
            prefijo = PREFIJO_JPEG;
        } else {
            throw new IllegalArgumentException("El logo debe ser una imagen PNG o JPEG.");
        }
        return prefijo + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Indica si el valor es un data URL con contenido base64.
     *
     * @param valor valor a revisar
     * @return true si el valor es un data URL
     */
    public static boolean esDataUrl(String valor) {
        return valor != null && valor.contains(MARCA_BASE64);
    }

    /**
     * Decodifica el contenido base64 de un data URL.
     *
     * @param dataUrl data URL a decodificar
     * @return bytes de la imagen, o un arreglo vacio si el valor no sirve
     */
    public static byte[] decodificar(String dataUrl) {
        if (dataUrl == null) {
            return new byte[0];
        }
        int marca = dataUrl.indexOf(MARCA_BASE64);
        if (marca < 0) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(dataUrl.substring(marca + MARCA_BASE64.length()).trim());
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }

    /**
     * Compara el comienzo del contenido con una firma binaria.
     *
     * @param bytes contenido del archivo
     * @param firma firma esperada
     * @return true si el contenido empieza con la firma
     */
    private static boolean empiezaCon(byte[] bytes, byte[] firma) {
        if (bytes.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (bytes[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
