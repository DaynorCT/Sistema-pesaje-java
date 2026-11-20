package bo.firmadigital.jacobitus.pesaje.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordsTest {

    @Test
    public void verificaLaContrasenaConSuPropioHash() {
        String hash = Passwords.hash("admin123");

        assertTrue(Passwords.verificar("admin123", hash));
        assertFalse(Passwords.verificar("admin124", hash));
        assertFalse(Passwords.verificar("", hash));
    }

    @Test
    public void generaSaltedDistintosParaLaMismaContrasena() {
        String primero = Passwords.hash("admin123");
        String segundo = Passwords.hash("admin123");

        assertNotEquals(primero, segundo);
        assertTrue(Passwords.verificar("admin123", primero));
        assertTrue(Passwords.verificar("admin123", segundo));
    }

    @Test
    public void elFormatoEsElDocumentado() {
        String hash = Passwords.hash("admin123");
        String[] partes = hash.split(":");

        assertTrue(hash.startsWith(Passwords.PREFIJO + ":"));
        assertTrue(partes.length == 4);
        assertTrue(Integer.parseInt(partes[1]) >= Passwords.ITERACIONES);
    }

    @Test
    public void rechazaHashesInvalidos() {
        assertFalse(Passwords.verificar("admin123", null));
        assertFalse(Passwords.verificar("admin123", ""));
        assertFalse(Passwords.verificar("admin123", "sha1:abc:def"));
        assertFalse(Passwords.verificar("admin123", "pbkdf2:abc:def:ghi"));
        assertFalse(Passwords.verificar("admin123", "pbkdf2:0:def:ghi"));
    }
}
