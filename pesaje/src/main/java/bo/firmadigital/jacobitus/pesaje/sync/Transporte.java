package bo.firmadigital.jacobitus.pesaje.sync;

import java.util.Map;

/**
 * Transporte HTTP usado por la sincronizacion.
 *
 * Es una interfaz para que el motor se pueda probar sin red: las
 * pruebas inyectan un transporte falso que devuelve respuestas
 * preparadas.
 */
public interface Transporte {

    /**
     * Envia una peticion.
     *
     * @param metodo    metodo HTTP (GET, POST)
     * @param url       URL completa
     * @param cuerpo    cuerpo de la peticion; puede ir vacio
     * @param cabeceras cabeceras de la peticion; puede ir vacio
     * @return la respuesta, con codigo 0 si no hubo conexion
     */
    RespuestaHttp enviar(String metodo, String url, String cuerpo, Map<String, String> cabeceras);
}
