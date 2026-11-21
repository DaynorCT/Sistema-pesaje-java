package bo.firmadigital.jacobitus.pesaje.sync;

import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Lectura y escritura del JSON que viaja con la sincronizacion.
 *
 * Se usa Jackson en su version 1.9 (la que ya depende el proyecto); los
 * campos desconocidos se ignoran para que una version mas nueva del
 * servidor no rompa al escritorio.
 */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private Json() {
    }

    /**
     * Convierte un mapa en texto JSON.
     *
     * @param valores mapa con los valores (el orden se respeta)
     * @return texto JSON
     */
    public static String escribir(Map<String, Object> valores) {
        try {
            return MAPPER.writeValueAsString(valores);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar el JSON de sincronizacion", e);
        }
    }

    /**
     * Lee el cuerpo de una respuesta.
     *
     * @param cuerpo texto JSON
     * @return nodo raiz, o null si el cuerpo no es JSON valido
     */
    public static JsonNode leer(String cuerpo) {
        if (cuerpo == null || cuerpo.trim().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readTree(cuerpo);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lee un texto de un nodo.
     *
     * @param nodo    nodo padre
     * @param campo   nombre del campo
     * @return el texto, o null si no existe
     */
    public static String texto(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        return valor.asText();
    }

    /**
     * Lee un entero de un nodo.
     *
     * @param nodo  nodo padre
     * @param campo nombre del campo
     * @return el valor, o null si no existe
     */
    public static Integer entero(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (valor.isNumber()) {
            return valor.asInt();
        }
        try {
            return Integer.valueOf(valor.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Lee un entero largo de un nodo.
     *
     * @param nodo  nodo padre
     * @param campo nombre del campo
     * @return el valor, o null si no existe
     */
    public static Long enteroLargo(JsonNode nodo, String campo) {
        if (nodo == null) {
            return null;
        }
        JsonNode valor = nodo.get(campo);
        if (valor == null || valor.isNull()) {
            return null;
        }
        if (valor.isNumber()) {
            return valor.asLong();
        }
        try {
            return Long.valueOf(valor.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Crea un mapa ordenado, util para armar payloads legibles.
     *
     * @return mapa vacio
     */
    public static Map<String, Object> mapa() {
        return new LinkedHashMap<>();
    }
}
