package bo.firmadigital.jacobitus.pesaje.sync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Transporte HTTP real, sobre HttpURLConnection.
 *
 * El tiempo de espera sale de la configuracion del sistema: sin senal,
 * cada peticion se comia el timeout entero y el escritorio quedaba
 * congelado.
 */
public final class TransporteHttp implements Transporte {

    private final int timeout;

    public TransporteHttp(int timeout) {
        this.timeout = timeout > 0 ? timeout : 4000;
    }

    @Override
    public RespuestaHttp enviar(String metodo, String url, String cuerpo,
            Map<String, String> cabeceras) {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(url).openConnection();
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(timeout);
            conexion.setReadTimeout(timeout);
            conexion.setRequestProperty("Accept", "application/json");
            if (cabeceras != null) {
                for (Map.Entry<String, String> cabecera : cabeceras.entrySet()) {
                    conexion.setRequestProperty(cabecera.getKey(), cabecera.getValue());
                }
            }
            if (cuerpo != null && !cuerpo.isEmpty()) {
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json");
                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
                }
            }
            int codigo = conexion.getResponseCode();
            return RespuestaHttp.deCodigo(codigo, leer(conexion, codigo));
        } catch (IOException e) {
            return RespuestaHttp.deFallo(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    private String leer(HttpURLConnection conexion, int codigo) {
        try (InputStream entrada = codigo >= 400
                ? conexion.getErrorStream()
                : conexion.getInputStream()) {
            if (entrada == null) {
                return "";
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = entrada.read(buffer)) > 0) {
                salida.write(buffer, 0, leidos);
            }
            return new String(salida.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
