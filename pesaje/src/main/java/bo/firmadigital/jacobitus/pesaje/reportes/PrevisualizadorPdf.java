package bo.firmadigital.jacobitus.pesaje.reportes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Vista previa de un PDF y apertura con el visor del sistema.
 *
 * Esta clase no usa JavaFX a proposito: el dialogo de vista previa vive
 * en la capa de UI, pero el render y la apertura con el visor quedan
 * aqui para poder probarlos sin levantar la toolkit grafica.
 *
 * Ninguno de los dos metodos publica excepciones: un PDF roto o un
 * escritorio sin visor son condiciones esperadas en la operacion diaria
 * y la UI solo necesita saber si pudo o no.
 */
public final class PrevisualizadorPdf {

    /** DPI con el que se renderiza la pagina para la vista previa. */
    private static final float DPI = 100f;

    private PrevisualizadorPdf() {
    }

    /**
     * Renderiza la primera pagina de un PDF.
     *
     * @param pdf archivo PDF a renderizar
     * @return imagen de la primera pagina, o null si el archivo no existe,
     *         no es un PDF valido o el render falla
     */
    public static BufferedImage renderizarPrimeraPagina(File pdf) {
        if (pdf == null || !pdf.isFile()) {
            return null;
        }
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFRenderer renderizador = new PDFRenderer(documento);
            return renderizador.renderImageWithDPI(0, DPI);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Abre un archivo con la aplicacion por defecto del sistema.
     *
     * Se usa el comando nativo y no {@code java.awt.Desktop}: la app corre
     * en modo headless (por el render del PDF) y en ese modo el Desktop no
     * esta soportado.
     *
     * @param archivo archivo a abrir
     * @return true si el visor lo abrio, false si el comando no se pudo
     *         lanzar
     */
    public static boolean abrirConVisor(File archivo) {
        if (archivo == null || !archivo.isFile()) {
            return false;
        }
        try {
            new ProcessBuilder(comandoParaAbrir(archivo)).start();
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /**
     * Comando del sistema para abrir un archivo.
     *
     * @param archivo archivo a abrir
     * @return comando y argumentos para lanzarlo
     */
    public static List<String> comandoParaAbrir(File archivo) {
        String ruta = archivo.getAbsolutePath();
        String sistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (sistema.contains("win")) {
            return Arrays.asList("rundll32", "url.dll,FileProtocolHandler", ruta);
        }
        if (sistema.contains("mac") || sistema.contains("darwin")) {
            return Arrays.asList("open", ruta);
        }
        return Arrays.asList("xdg-open", ruta);
    }
}
