package bo.firmadigital.jacobitus.pesaje.reportes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioBoleta;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioBoleta.BoletaFila;

/**
 * Generador de la boleta de pesaje en PDF.
 *
 * Replica el diseno del sistema original (fppdf): hoja carta apaisada,
 * encabezado de tres columnas con el logo de la empresa, dos columnas de
 * datos y el bloque de pesos al pie.
 *
 * Las coordenadas estan en milimetros, igual que en el original; PDFBox
 * trabaja en puntos y mide desde abajo, asi que se invierte el eje
 * vertical.
 */
public final class BoletaPdf {

    /** Milimetros por punto de PDF. */
    private static final double MM = 72.0 / 25.4;

    private BoletaPdf() {
    }

    /**
     * Genera la boleta de un pesaje.
     *
     * @param pesajeId id del pesaje
     * @return bytes del PDF
     * @throws IOException si el PDF no se puede escribir
     */
    public static byte[] generar(int pesajeId) throws IOException {
        DatosBoleta datos = DatosBoleta.cargar(pesajeId);
        if (datos == null) {
            throw new IOException("El pesaje " + pesajeId + " no existe.");
        }
        return generar(datos, configuracion(), logoEmpresa());
    }

    /**
     * Genera la boleta con los datos indicados.
     *
     * @param datos      datos del pesaje
     * @param boleta     configuracion de la boleta
     * @param logoBase64 logo de la empresa en base64, o cadena vacia
     * @return bytes del PDF
     * @throws IOException si el PDF no se puede escribir
     */
    public static byte[] generar(DatosBoleta datos, BoletaFila boleta, String logoBase64)
            throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDRectangle carta = PDRectangle.LETTER;
            PDPage pagina = new PDPage(new PDRectangle(carta.getHeight(), carta.getWidth()));
            documento.addPage(pagina);
            double alto = pagina.getMediaBox().getHeight();

            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                contenido.beginText();

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                escribir(contenido, 15, alto, 20, enMayusculas(boleta.salida));
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 15);
                escribir(contenido, 95, alto, 20, enMayusculas(boleta.tituloSuperior));
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);
                escribir(contenido, 215, alto, 20, "ACREDITADO POR:");

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                escribir(contenido, 15, alto, 25, enMayusculas(boleta.direccion));
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22);
                escribir(contenido, 110, alto, 30, enMayusculas(boleta.marca));
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                escribir(contenido, 15, alto, 30, "Cel: " + boleta.celular1);
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 15);
                escribir(contenido, 108, alto, 40, enMayusculas(boleta.tituloDocumento));
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                escribir(contenido, 22, alto, 34, boleta.celular2);
                escribir(contenido, 15, alto, 38, enMayusculas(boleta.ciudad));
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                escribir(contenido, 15, alto, 56, "Guia: " + enMayusculas(datos.getGuia()));

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
                escribir(contenido, 15, alto, 64, "DATOS DEL VEHICULO:");
                escribir(contenido, 155, alto, 64, "DATOS DEL PESAJE:");

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                fila(contenido, alto, 71, "Placa:", 80, datos.getVehiculoPlaca(),
                        "Producto:", 220, datos.getProductoNombre());
                fila(contenido, alto, 78, "Chofer:", 80, datos.getChoferNombre(),
                        "Costo Bs.:", 220, datos.getCostoBs() + " Bs");
                fila(contenido, alto, 85, "Licencia:", 80, datos.getChoferLicencia(),
                        "Origen:", 220, datos.getOrigenNombre());
                fila(contenido, alto, 92, "Proveedor:", 80, datos.getProveedorNombre(),
                        "Destino:", 220, datos.getDestinoNombre());
                fila(contenido, alto, 99, "Tipo Vehiculo:", 80, datos.getVehiculoTipo(),
                        "Flete Bs.:", 220, datos.getFleteBs() + " Bs");

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                String fechaHora = "Fecha/Hora (Pes): " + datos.getFecha() + " " + datos.getHora();
                escribir(contenido, 15, alto, 107, fechaHora);
                escribir(contenido, 155, alto, 107, "Fecha/Hora (Imp): " + datos.getFecha()
                        + " " + datos.getHora());

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                escribir(contenido, 15, alto, 116, "PESO BRUTO:");
                escribir(contenido, 115, alto, 116, "PESO TARA:");
                escribir(contenido, 205, alto, 116, "PESO NETO:");
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                escribir(contenido, 15, alto, 122, numero(datos.getPesoBruto()) + " kg");
                escribir(contenido, 115, alto, 122, numero(datos.getTara()) + " kg");
                escribir(contenido, 205, alto, 122, numero(datos.getPesoNeto()) + " kg");

                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                escribir(contenido, 60, alto, 160, "Chofer o Productor");
                escribir(contenido, 187, alto, 160, "Operador de balanza");
                if (boleta.acreditacion != null && !boleta.acreditacion.isEmpty()) {
                    contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                    escribir(contenido, 215, alto, 58, boleta.acreditacion);
                }

                contenido.endText();

                linea(contenido, alto, 59, 15, 266);
                linea(contenido, alto, 102, 15, 266);
                linea(contenido, alto, 111, 15, 266);
                linea(contenido, alto, 126, 15, 266);
                linea(contenido, alto, 156, 25, 125);
                linea(contenido, alto, 156, 155, 190);
            }

            dibujarLogo(documento, pagina, alto, logoBase64);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /**
     * Escribe el PDF en un archivo.
     *
     * @param pesajeId id del pesaje
     * @param destino  archivo de destino
     * @throws IOException si el PDF no se puede escribir
     */
    public static void guardar(int pesajeId, File destino) throws IOException {
        byte[] pdf = generar(pesajeId);
        try (OutputStream salida = new FileOutputStream(destino)) {
            salida.write(pdf);
        }
    }

    private static void fila(PDPageContentStream contenido, double alto, double y,
            String etiquetaIzq, double xValorIzq, String valorIzq,
            String etiquetaDer, double xValorDer, String valorDer) throws IOException {
        escribir(contenido, 15, alto, y, etiquetaIzq);
        escribir(contenido, xValorIzq, alto, y, valorIzq);
        escribir(contenido, 155, alto, y, etiquetaDer);
        escribir(contenido, xValorDer, alto, y, valorDer);
    }

    private static void escribir(PDPageContentStream contenido, double xMm, double alto,
            double yMm, String texto) throws IOException {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        contenido.setTextMatrix(Matrix.getTranslateInstance((float) mm(xMm), (float) (alto - mm(yMm))));
        contenido.showText(sinAcentos(texto));
    }

    private static void linea(PDPageContentStream contenido, double alto, double yMm,
            double xDesde, double xHasta) throws IOException {
        contenido.setLineWidth(0.3f);
        contenido.moveTo((float) mm(xDesde), (float) (alto - mm(yMm)));
        contenido.lineTo((float) mm(xHasta), (float) (alto - mm(yMm)));
        contenido.stroke();
    }

    private static void dibujarLogo(PDDocument documento, PDPage pagina, double alto,
            String logoBase64) throws IOException {
        if (logoBase64 == null || logoBase64.isEmpty()) {
            return;
        }
        int marca = logoBase64.indexOf(";base64,");
        if (marca < 0) {
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(logoBase64.substring(marca + 8).trim());
        } catch (IllegalArgumentException e) {
            return;
        }
        if (bytes.length == 0) {
            return;
        }
        PDImageXObject imagen;
        try {
            imagen = LosslessFactory.createFromImage(documento, leerImagen(bytes));
        } catch (IOException e) {
            return;
        }
        double maxAncho = mm(50);
        double maxAlto = mm(35);
        double proporcion = imagen.getWidth() / (double) imagen.getHeight();
        double anchoDibujo;
        double altoDibujo;
        if (imagen.getWidth() / maxAncho > imagen.getHeight() / maxAlto) {
            anchoDibujo = maxAncho;
            altoDibujo = maxAncho / proporcion;
        } else {
            altoDibujo = maxAlto;
            anchoDibujo = maxAlto * proporcion;
        }
        try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
            contenido.drawImage(imagen, (float) mm(215), (float) (alto - mm(59)),
                    (float) anchoDibujo, (float) altoDibujo);
        }
    }

    /**
     * Configuracion de la boleta guardada en la base.
     *
     * @return configuracion, o los valores por defecto si no hay ninguna
     */
    private static BoletaFila configuracion() {
        BoletaFila fila = new RepositorioBoleta().obtenerActiva();
        if (fila != null) {
            return fila;
        }
        BoletaFila porDefecto = new BoletaFila();
        porDefecto.salida = "Salida a Potosi";
        porDefecto.direccion = "Tarija km 2";
        porDefecto.celular1 = "2782323";
        porDefecto.celular2 = "1234343";
        porDefecto.ciudad = "POTOSI - BOLIVIA";
        porDefecto.tituloSuperior = "BALANZA DE PESAJE DIGITAL";
        porDefecto.marca = "PRIMAVERA";
        porDefecto.tituloDocumento = "BOLETA DE PESAJE";
        return porDefecto;
    }

    /**
     * Logo de la empresa activa, en base64.
     *
     * @return logo en base64, o cadena vacia
     */
    private static String logoEmpresa() {
        List<String> logos = Database.obtenerInstancia().sql().consultar(
                "SELECT logo FROM empresas WHERE estado = 'ACTIVO' ORDER BY id DESC LIMIT 1",
                fila -> fila.getString("logo"));
        return logos.isEmpty() || logos.get(0) == null ? "" : logos.get(0);
    }

    /**
     * Lee los bytes de la imagen.
     *
     * @param bytes contenido del archivo de imagen
     * @return imagen decodificada
     * @throws IOException si la imagen no se puede leer
     */
    private static java.awt.image.BufferedImage leerImagen(byte[] bytes) throws IOException {
        return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
    }

    private static double mm(double milimetros) {
        return milimetros * MM;
    }

    private static String enMayusculas(String texto) {
        return texto == null ? "" : texto.toUpperCase();
    }

    /**
     * Formatea un numero con separador de miles, como el original.
     *
     * @param valor numero a formatear
     * @return numero con comas
     */
    static String numero(int valor) {
        return String.format("%,d", valor);
    }

    /**
     * Las fuentes base de PDFBox no tienen acentos: se reemplazan por su
     * equivalente ASCII para que el texto no se corte.
     *
     * @param texto texto a convertir
     * @return texto sin acentos
     */
    static String sinAcentos(String texto) {
        if (texto == null) {
            return "";
        }
        String[][] reemplazos = {
                {"á", "a"}, {"é", "e"}, {"í", "i"}, {"ó", "o"}, {"ú", "u"},
                {"Á", "A"}, {"É", "E"}, {"Í", "I"}, {"Ó", "O"}, {"Ú", "U"},
                {"ñ", "n"}, {"Ñ", "N"}};
        String resultado = texto;
        for (String[] par : reemplazos) {
            resultado = resultado.replace(par[0], par[1]);
        }
        return resultado;
    }
}
