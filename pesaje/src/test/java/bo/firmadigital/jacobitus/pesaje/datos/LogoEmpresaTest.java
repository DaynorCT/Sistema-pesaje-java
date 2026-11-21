package bo.firmadigital.jacobitus.pesaje.datos;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Pruebas del manejo del logo de la empresa.
 *
 * El logo se guarda como data URL (base64) porque asi lo lee la boleta en
 * PDF. El tipo de la imagen se deduce de la firma binaria del contenido,
 * nunca de la extension del archivo.
 */
public class LogoEmpresaTest {

    /** PNG de 1x1 pixel transparente. */
    private static final byte[] PNG_UN_PIXEL = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    /** JPEG de 1x1 pixel. */
    private static final byte[] JPEG_UN_PIXEL = Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQAASABIAAD/4QBMRXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAABAAEAAKAC"
                    + "AAQAAAABAAAAAaADAAQAAAABAAAAAQAAAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAAA4QklNBCUAAAAAABDU"
                    + "HYzZjwCyBOmACZjs+EJ+/8AAEQgAAQABAwEiAAIRAQMRAf/EAB8AAAEFAQEBAQEBAAAAAAAAAAABAgMEBQYHCAkK"
                    + "C//EALUQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJ"
                    + "SYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqa"
                    + "qys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+v/EAB8BAAMBAQEBAQEBAQEAAAAAAA"
                    + "ABAgMEBQYHCAkKC//EALURAAIBAgQEAwQHBQQEAAECdwABAgMRBAUhMQYSQVEHYXETIjKBCBRCkaGxwQkjM1LwFWJ"
                    + "y0QoWJDThJfEXGBkaJicoKSo1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoKDhIWGh4iJipKTlJWWl"
                    + "5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uLj5OXm5+jp6vLz9PX29/j5+v/bAEMAAgICAgICAwI"
                    + "CAwUDAwMFBgUFBQUGCAYGBgYGCAoICAgICAgKCgoKCgoKCgwMDAwMDA4ODg4ODw8PDw8PDw8PD//bAEMBAgICBAQEBwQE"
                    + "BxALCQsQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEP/dAAQAAf/aAAwDAQAC"
                    + "EQMRAD8A/Syiiiv5XP4rP//Z");

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    @Test
    public void codificaUnPngComoDataUrl() throws Exception {
        // El archivo lleva extension JPEG a proposito: el tipo se deduce
        // de la firma del contenido, no del nombre.
        Path archivo = escribir("logo.jpg", PNG_UN_PIXEL);

        String dataUrl = LogoEmpresa.dataUrl(archivo);

        assertTrue("El data URL debe indicar el tipo PNG: " + dataUrl,
                dataUrl.startsWith("data:image/png;base64,"));
        assertTrue("El data URL debe traer la marca base64: " + dataUrl,
                dataUrl.contains(";base64,"));
    }

    @Test
    public void decodificaLosBytesOriginales() throws Exception {
        Path archivo = escribir("logo.png", PNG_UN_PIXEL);

        byte[] decodificado = LogoEmpresa.decodificar(LogoEmpresa.dataUrl(archivo));

        assertArrayEquals("Se deben recuperar los bytes del archivo", PNG_UN_PIXEL, decodificado);
    }

    @Test
    public void rechazaUnArchivoMayorADosMegas() throws Exception {
        Path archivo = escribir("grande.png", new byte[(int) LogoEmpresa.LIMITE_BYTES + 1]);

        try {
            LogoEmpresa.dataUrl(archivo);
            fail("Se esperaba un rechazo por el tamano del archivo");
        } catch (IllegalArgumentException e) {
            assertEquals("El archivo debe ser menor a 2MB.", e.getMessage());
        }
    }

    @Test
    public void rechazaUnArchivoQueNoEsUnaImagen() throws Exception {
        Path archivo = escribir("notas.txt",
                "esto no es una imagen".getBytes(StandardCharsets.UTF_8));

        try {
            LogoEmpresa.dataUrl(archivo);
            fail("Se esperaba un rechazo por el tipo de imagen");
        } catch (IllegalArgumentException e) {
            assertEquals("El logo debe ser una imagen PNG o JPEG.", e.getMessage());
        }
    }

    @Test
    public void codificaUnJpegComoDataUrl() throws Exception {
        Path archivo = escribir("logo.jpg", JPEG_UN_PIXEL);

        String dataUrl = LogoEmpresa.dataUrl(archivo);

        assertTrue("El data URL debe indicar el tipo JPEG: " + dataUrl,
                dataUrl.startsWith("data:image/jpeg;base64,"));
    }

    @Test
    public void reconoceUnDataUrl() {
        assertTrue(LogoEmpresa.esDataUrl("data:image/png;base64,AAAA"));
        assertFalse(LogoEmpresa.esDataUrl(null));
        assertFalse(LogoEmpresa.esDataUrl("data:image/png,AAAA"));
    }

    @Test
    public void decodificaUnValorInvalidoComoArregloVacio() {
        assertEquals(0, LogoEmpresa.decodificar(null).length);
        assertEquals(0, LogoEmpresa.decodificar("no es base64").length);
        assertEquals(0, LogoEmpresa.decodificar("data:image/png;base64,%%%").length);
    }

    @Test
    public void elLogoDelSistemaVieneComoDataUrl() {
        String dataUrl = LogoEmpresa.dataUrlDelSistema();

        assertTrue("El logo del sistema deberia estar disponible", dataUrl.startsWith("data:image/"));
        assertTrue("El logo del sistema deberia traer la marca base64", dataUrl.contains(";base64,"));
    }

    private Path escribir(String nombre, byte[] contenido) throws IOException {
        Path archivo = carpeta.getRoot().toPath().resolve(nombre);
        Files.write(archivo, contenido);
        return archivo;
    }
}
