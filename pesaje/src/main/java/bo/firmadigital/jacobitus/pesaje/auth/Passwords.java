package bo.firmadigital.jacobitus.pesaje.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contrasenas con PBKDF2-HMAC-SHA256.
 *
 * Tambien verifica el formato heredado del sistema original:
 * {@code sha1:<sha1_hex_b64>:<salt_b64>} (SHA-1 de salt+password en hex).
 *
 * Formato nuevo: {@code pbkdf2:<iteraciones>:<salt_b64>:<hash_b64>}.
 */
public final class Passwords {

    public static final String PREFIJO = "pbkdf2";
    public static final String PREFIJO_SHA1 = "sha1";
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

    /**
     * @param almacenado valor de {@code password_hash} en la base
     * @return true si ya esta en PBKDF2
     */
    public static boolean esHashModerno(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO + ":");
    }

    /**
     * @param almacenado valor de {@code password_hash} en la base
     * @return true si el hash heredado SHA1 sigue vigente
     */
    public static boolean esHashLegadoSha1(String almacenado) {
        return almacenado != null && almacenado.startsWith(PREFIJO_SHA1 + ":");
    }

    public static boolean verificar(String contrasena, String almacenado) {
        if (contrasena == null || almacenado == null) {
            return false;
        }
        if (esHashModerno(almacenado)) {
            return verificarPbkdf2(contrasena, almacenado);
        }
        if (esHashLegadoSha1(almacenado)) {
            return verificarSha1Legado(contrasena, almacenado);
        }
        return false;
    }

    private static boolean verificarPbkdf2(String contrasena, String almacenado) {
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

    /**
     * Replica {@code TAuthService.VerifyPassword} del sistema Lazarus.
     */
    static boolean verificarSha1Legado(String contrasena, String almacenado) {
        String cuerpo = almacenado.substring((PREFIJO_SHA1 + ":").length());
        String[] partes = cuerpo.split(":", 2);
        if (partes.length != 2) {
            return false;
        }
        String hexEsperado;
        String salt;
        try {
            hexEsperado = new String(Base64.getDecoder().decode(partes[0]), StandardCharsets.UTF_8);
            salt = new String(Base64.getDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String hexCalculado = sha1Hex(salt + contrasena);
        return MessageDigest.isEqual(
                hexEsperado.getBytes(StandardCharsets.UTF_8),
                hexCalculado.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha1Hex(String texto) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digesto = sha1.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digesto.length * 2);
            for (byte b : digesto) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 no disponible", ex);
        }
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
