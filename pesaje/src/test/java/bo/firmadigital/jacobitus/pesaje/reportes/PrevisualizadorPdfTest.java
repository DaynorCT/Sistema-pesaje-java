package bo.firmadigital.jacobitus.pesaje.reportes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import bo.firmadigital.jacobitus.pesaje.datos.Database;

/**
 * Pruebas de la vista previa de PDF.
 *
 * La clase {@link PrevisualizadorPdf} no usa JavaFX a proposito: asi el
 * render y la apertura con el visor del sistema se pueden probar sin
 * levantar la toolkit grafica.
 *
 * El PDF valido se arma con {@link BoletaPdf} sobre una base temporal,
 * igual que en {@code PdfReportesTest}.
 */
public class PrevisualizadorPdfTest {

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private Database base;

    @Before
    public void prepararBaseTemporal() {
        System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA,
                new File(carpeta.getRoot(), Database.ARCHIVO_BASE).getAbsolutePath());
        Database.cerrarInstancia();
        base = Database.obtenerInstancia();
    }

    @After
    public void cerrarBase() {
        Database.cerrarInstancia();
        System.clearProperty(Database.PROPIEDAD_SISTEMA_RUTA);
    }

    @Test
    public void renderizaLaPrimeraPaginaDeLaBoleta() throws Exception {
        File pdf = generarBoleta();

        BufferedImage imagen = PrevisualizadorPdf.renderizarPrimeraPagina(pdf);

        assertNotNull("La vista previa deberia renderizar la primera pagina", imagen);
        assertTrue("El ancho de la vista previa deberia ser mayor a cero",
                imagen.getWidth() > 0);
        assertTrue("El alto de la vista previa deberia ser mayor a cero",
                imagen.getHeight() > 0);
    }

    @Test
    public void unArchivoQueNoEsPdfNoRenderiza() throws Exception {
        File texto = carpeta.newFile("no-es-pdf.txt");
        Files.write(texto.toPath(), "esto no es un PDF".getBytes(StandardCharsets.UTF_8));

        assertNull("Un archivo que no es PDF no deberia renderizar",
                PrevisualizadorPdf.renderizarPrimeraPagina(texto));
    }

    @Test
    public void unArchivoInexistenteNoRenderiza() {
        File inexistente = new File(carpeta.getRoot(), "no-existe.pdf");

        assertNull("Un archivo inexistente no deberia renderizar",
                PrevisualizadorPdf.renderizarPrimeraPagina(inexistente));
    }

    @Test
    public void unArchivoNuloNoRenderiza() {
        assertNull("Un archivo nulo no deberia renderizar",
                PrevisualizadorPdf.renderizarPrimeraPagina(null));
    }

    @Test
    public void unArchivoInexistenteNoSeAbreConElVisor() {
        File inexistente = new File(carpeta.getRoot(), "no-existe.pdf");

        assertFalse("Un archivo inexistente no se puede abrir con el visor",
                PrevisualizadorPdf.abrirConVisor(inexistente));
    }

    @Test
    public void unArchivoNuloNoSeAbreConElVisor() {
        assertFalse("Un archivo nulo no se puede abrir con el visor",
                PrevisualizadorPdf.abrirConVisor(null));
    }

    @Test
    public void elComandoParaAbrirUsaElVisorDelSistema() throws Exception {
        File pdf = generarBoleta();

        List<String> comando = PrevisualizadorPdf.comandoParaAbrir(pdf);

        assertTrue("El comando deberia traer la ruta del archivo",
                comando.contains(pdf.getAbsolutePath()));
        String sistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String esperado;
        if (sistema.contains("win")) {
            esperado = "rundll32";
        } else if (sistema.contains("mac") || sistema.contains("darwin")) {
            esperado = "open";
        } else {
            esperado = "xdg-open";
        }
        assertEquals("El comando deberia usar el visor del sistema", esperado, comando.get(0));
    }

    /**
     * Genera la boleta de un pesaje de prueba y la guarda en la carpeta
     * temporal.
     *
     * @return archivo PDF con la boleta
     * @throws Exception si la boleta no se puede generar
     */
    private File generarBoleta() throws Exception {
        var sql = base.sql();
        long vehiculo = sql.insertar(
                "INSERT INTO vehiculos (placa, tipo_vehiculo, tara, estado)"
                        + " VALUES ('VGI-233', 'VOLVO', 3030, 'ACTIVO')");
        long persona = sql.insertar(
                "INSERT INTO personas (nombre, apellido_paterno, ci, estado)"
                        + " VALUES ('Juan', 'Perez', '1234', 'ACTIVO')");
        long chofer = sql.insertar(
                "INSERT INTO choferes (persona_id, licencia, estado) VALUES (?, ?, 'ACTIVO')",
                persona, "LIC-99");
        long producto = sql.insertar(
                "INSERT INTO productos (nombre, descripcion, estado) VALUES ('Mineral', 'Hierro', 'ACTIVO')");

        bo.firmadigital.jacobitus.pesaje.modelo.Pesaje pesaje =
                new bo.firmadigital.jacobitus.pesaje.modelo.Pesaje();
        pesaje.setGuia("PESO-2026-000123");
        pesaje.setVehiculoId((int) vehiculo);
        pesaje.setChoferId((int) chofer);
        pesaje.setProductoId((int) producto);
        pesaje.setPesoBruto(12000);
        pesaje.setTara(3030);
        pesaje.setEstadoBalanza(
                bo.firmadigital.jacobitus.pesaje.modelo.EstadoBalanza.FINALIZADO);
        pesaje.setFechaCreacion("2026-11-20 09:35:00");
        pesaje.setPesadorId((int) persona);
        new bo.firmadigital.jacobitus.pesaje.datos.RepositorioPesaje().guardar(pesaje, 1);

        File destino = new File(carpeta.getRoot(), "boleta.pdf");
        BoletaPdf.guardar(pesaje.getId(), destino);
        return destino;
    }
}
