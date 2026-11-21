package bo.firmadigital.jacobitus.pesaje.ui.comun;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Abre una URL con el navegador por defecto del sistema.
 *
 * Se evita {@code java.awt.Desktop} porque el modulo de pesaje puede correr
 * en contextos donde el render de PDF deja el entorno en modo headless.
 */
public final class NavegadorSistema {

    private NavegadorSistema() {
    }

    /**
     * @param url direccion http(s) a abrir
     * @return true si el comando del sistema se lanzo correctamente
     */
    public static boolean abrirUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            new ProcessBuilder(comandoParaAbrir(url.trim())).start();
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static List<String> comandoParaAbrir(String url) {
        URI uri = URI.create(url);
        String destino = uri.toASCIIString();
        String sistema = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (sistema.contains("win")) {
            return Arrays.asList("rundll32", "url.dll,FileProtocolHandler", destino);
        }
        if (sistema.contains("mac") || sistema.contains("darwin")) {
            return Arrays.asList("open", destino);
        }
        return Arrays.asList("xdg-open", destino);
    }
}