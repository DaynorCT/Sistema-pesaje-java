package bo.firmadigital.jacobitus.pesaje.reportes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import bo.firmadigital.jacobitus.pesaje.datos.Database;

/**
 * Pruebas de la generacion de PDF.
 *
 * Se comprueba que el PDF se arma y que trae los datos del pesaje: el
 * texto se puede leer con el extractor de PDFBox.
 */
public class PdfReportesTest {

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

    private int crearPesaje() {
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
        return pesaje.getId();
    }

    @Test
    public void generaLaBoletaConLosDatosDelPesaje() throws Exception {
        int id = crearPesaje();
        byte[] pdf = BoletaPdf.generar(id);
        assertTrue("El PDF no deberia estar vacio", pdf.length > 500);
        String texto = leerTexto(pdf);
        assertTrue(texto.contains("PESO-2026-000123"));
        assertTrue(texto.contains("VGI-233"));
        assertTrue(texto.contains("12,000 kg"));
        assertTrue(texto.contains("PESO NETO:"));
    }

    @Test
    public void generaElReporteConElTotalDeRegistros() throws Exception {
        crearPesaje();
        byte[] pdf = ReportePdf.generar("", "", "");
        String texto = leerTexto(pdf);
        assertTrue(texto.contains("REPORTE DE PESAJE"));
        assertTrue(texto.contains("Total registros: 1"));
    }

    @Test
    public void elReporteFiltraPorPlaca() throws Exception {
        crearPesaje();
        byte[] pdf = ReportePdf.generar("", "", "ZZZ-999");
        String texto = leerTexto(pdf);
        assertTrue(texto.contains("Total registros: 0"));
    }

    @Test
    public void guardaElArchivoEnDisco() throws Exception {
        int id = crearPesaje();
        File destino = new File(carpeta.getRoot(), "boleta.pdf");
        BoletaPdf.guardar(id, destino);
        assertTrue(destino.exists());
        assertTrue(Files.size(destino.toPath()) > 500);
    }

    private String leerTexto(byte[] pdf) throws Exception {
        try (org.apache.pdfbox.pdmodel.PDDocument documento =
                org.apache.pdfbox.Loader.loadPDF(pdf)) {
            return new org.apache.pdfbox.text.PDFTextStripper().getText(documento);
        }
    }
}
