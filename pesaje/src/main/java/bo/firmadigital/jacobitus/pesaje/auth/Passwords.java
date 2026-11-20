package bo.firmadigital.jacobitus.pesaje.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contrasenas con PBKDF2-HMAC-SHA256.
 *
 * El sistema original guardaba SHA1 con salt, que hoy es inadequate para
 * guardar contrasenas: aqui se usa una funcion de derivacion lenta con
 * salt por usuario e iteraciones fijas.
 *
 * Formato almacenado: `pbkdf2:<iteraciones>:<salt_b64>:<hash_b64>`.
 */
public final class Passwords {

    public static final String PREFIJO = "pbkdf2";
    public static final int ITERACIONES = 120000;
    public static final int LONGITUD_SALT = 16;
    public static final int LONGITUD_HASH = 32;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Passwords() {
    }

    public static String hash(String contrasena) {
        byte[] salt = new byte[LONGITUD_SALT];
        ALEATORIO.nextBytes(salt);
        byte[] derivado = derivar(contrasena, salt, ITERACIONES);
        return PREFIJO + ":" + ITERACIONES + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(derivado);
    }

    public static boolean verificar(String contrasena, String almacenado) {
        if (contrasena == null || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split(":");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        int iteraciones;
        try {
            iteraciones = Integer.parseInt(partes[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        if (iteraciones <= 0) {
            return false;
        }
        byte[] salt;
        byte[] esperado;
        try {
            salt = Base64.getDecoder().decode(partes[2]);
            esperado = Base64.getDecoder().decode(partes[3]);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        byte[] derivado = derivar(contrasena, salt, iteraciones);
        return MessageDigest.isEqual(derivado, esperado);
    }

    private static byte[] derivar(String contrasena, byte[] salt, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), salt, iteraciones, LONGITUD_HASH * 8);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (java.security.NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("No se pudo derivar la clave de la contrasena.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
