package bo.firmadigital.jacobitus.pesaje.reportes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;

import bo.firmadigital.jacobitus.pesaje.datos.Database;

/**
 * Generador del reporte de pesajes en PDF.
 *
 * Replica el sistema original: hoja carta vertical, un bloque por
 * pesaje con los datos en dos columnas y el total de registros al pie.
 * Acepta los mismos filtros: rango de fechas y placa.
 */
public final class ReportePdf {

    private static final double MM = 72.0 / 25.4;
    private static final DateTimeFormatter FECHA_REPORTE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReportePdf() {
    }

    /**
     * Genera el reporte.
     *
     * @param fechaDesde fecha inicial (yyyy-MM-dd) o vacia
     * @param fechaHasta fecha final (yyyy-MM-dd) o vacia
     * @param placa     parte de la placa o vacia
     * @return bytes del PDF
     * @throws IOException si el PDF no se puede escribir
     */
    public static byte[] generar(String fechaDesde, String fechaHasta, String placa)
            throws IOException {
        List<DatosBoleta> datos = DatosBoleta.cargarReporte(fechaDesde, fechaHasta, placa);
        String nombreEmpresa = nombreEmpresa();

        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.LETTER);
            documento.addPage(pagina);
            double alto = pagina.getMediaBox().getHeight();

            PDPageContentStream contenido = new PDPageContentStream(documento, pagina);
            try {
                contenido.beginText();
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                escribir(contenido, 75, alto, 25, "REPORTE DE PESAJE");
                if (!nombreEmpresa.isEmpty()) {
                    contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                    escribir(contenido, 60, alto, 32, nombreEmpresa.toUpperCase());
                }
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                escribir(contenido, 80, alto, 38, "Fecha: " + LocalDateTime.now().format(FECHA_REPORTE));

                double y = 50;
                for (int i = 0; i < datos.size(); i++) {
                    DatosBoleta fila = datos.get(i);
                    if (y > alto - mm(60)) {
                        // La pagina se llena: se abre otra y se sigue.
                        contenido.endText();
                        contenido.close();
                        pagina = new PDPage(PDRectangle.LETTER);
                        documento.addPage(pagina);
                        alto = pagina.getMediaBox().getHeight();
                        y = 25;
                        contenido = new PDPageContentStream(documento, pagina);
                        contenido.beginText();
                        contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    }
                    y = escribirFila(contenido, alto, y, i + 1, fila);
                }

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 11);
                escribir(contenido, 15, alto, Math.min(y + 6, alto - mm(15)),
                        "Total registros: " + datos.size());
                contenido.endText();
            } finally {
                contenido.close();
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /**
     * Escribe el reporte en un archivo.
     *
     * @param fechaDesde fecha inicial (yyyy-MM-dd) o vacia
     * @param fechaHasta fecha final (yyyy-MM-dd) o vacia
     * @param placa     parte de la placa o vacia
     * @param destino   archivo de destino
     * @throws IOException si el PDF no se puede escribir
     */
    public static void guardar(String fechaDesde, String fechaHasta, String placa, File destino)
            throws IOException {
        byte[] pdf = generar(fechaDesde, fechaHasta, placa);
        try (OutputStream salida = new FileOutputStream(destino)) {
            salida.write(pdf);
        }
    }

    private static double escribirFila(PDPageContentStream contenido, double alto, double y,
            int numero, DatosBoleta fila) throws IOException {
        contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        par(contenido, alto, y, "Nro. " + numero, "GUIA: " + fila.getGuia(), "COSTO",
                "Bs " + numero(fila.getCostoBs()));
        par(contenido, alto, y + 5, "CHOFER", fila.getChoferNombre(), "FLETE",
                "Bs " + numero(fila.getFleteBs()));
        par(contenido, alto, y + 10, "VEHICULO",
                fila.getVehiculoTipo() + " - " + fila.getVehiculoPlaca(), "", "");
        par(contenido, alto, y + 15, "NRO. DOC.", fila.getChoferCi(), "", "");
        par(contenido, alto, y + 20, "LICENCIA", fila.getChoferLicencia(), "", "");
        par(contenido, alto, y + 25, "PROVEEDOR", fila.getProveedorNombre(), "", "");
        par(contenido, alto, y + 30, "PRODUCTO", fila.getProductoNombre(), "", "");
        par(contenido, alto, y + 35, "ORIGEN", fila.getOrigenNombre(), "", "");
        par(contenido, alto, y + 40, "DESTINO", fila.getDestinoNombre(), "", "");
        par(contenido, alto, y + 45, "P. BRUTO", numero(fila.getPesoBruto()) + " kg", "", "");
        par(contenido, alto, y + 50, "TARA", numero(fila.getTara()) + " kg", "", "");
        par(contenido, alto, y + 55, "PESO NETO", numero(fila.getPesoNeto()) + " kg",
                "FECHA/HORA", fila.getFecha() + " " + fila.getHora());
        return y + 62;
    }

    private static void par(PDPageContentStream contenido, double alto, double y,
            String etiquetaIzq, String valorIzq, String etiquetaDer, String valorDer)
            throws IOException {
        escribir(contenido, 15, alto, y, etiquetaIzq);
        escribir(contenido, 45, alto, y, valorIzq);
        escribir(contenido, 110, alto, y, etiquetaDer);
        escribir(contenido, 145, alto, y, valorDer);
    }

    private static void escribir(PDPageContentStream contenido, double xMm, double alto,
            double yMm, String texto) throws IOException {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        contenido.setTextMatrix(Matrix.getTranslateInstance((float) mm(xMm), (float) (alto - mm(yMm))));
        contenido.showText(BoletaPdf.sinAcentos(texto));
    }

    private static double mm(double milimetros) {
        return milimetros * MM;
    }

    private static String numero(int valor) {
        return String.format("%,d", valor);
    }

    private static String nombreEmpresa() {
        List<String> nombres = Database.obtenerInstancia().sql().consultar(
                "SELECT nombre_empresa FROM empresas WHERE estado = 'ACTIVO' ORDER BY id DESC LIMIT 1",
                fila -> fila.getString("nombre_empresa"));
        return nombres.isEmpty() || nombres.get(0) == null ? "" : nombres.get(0);
    }
}
