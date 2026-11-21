package bo.firmadigital.jacobitus.pesaje.sync;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

/**
 * Sesion autenticada contra el servidor web.
 *
 * El servidor entrega el token en el cuerpo (POST /api/auth/token) y lo
 * espera depois en la cookie `access_token`; el refresh se hace con la
 * cookie `refresh_token` (POST /api/auth/refresh). Es el mismo contrato
 * que usa el sistema original.
 *
 * La contrasena en claro solo se usa al pedir el token: la base local
 * guarda el hash PBKDF2, asi que no se puede recuperar de ahi. Por eso el
 * escritorio pide de nuevo las credenciales del usuario web.
 */
public final class SesionSync {

    private final Transporte transporte;
    private final String urlBase;
    private final String correo;
    private final String contrasena;

    private String accessToken;
    private String refreshToken;
    private String ultimoError = "";

    public SesionSync(Transporte transporte, String urlBase, String correo, String contrasena) {
        this.transporte = transporte;
        this.urlBase = urlBase;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    /**
     * Se asegura de tener un token valido: pide uno si no hay, o renueva
     * el que hay si el servidor lo rechazo.
     *
     * @return true si queda con un token utilizable
     */
    public boolean asegurarToken() {
        if (accessToken != null && !accessToken.isEmpty()) {
            return true;
        }
        return iniciarSesion();
    }

    /**
     * Pide un token nuevo con las credenciales del usuario.
     *
     * @return true si el servidor entrego el token
     */
    public boolean iniciarSesion() {
        Map<String, Object> cuerpo = Json.mapa();
        cuerpo.put("usuario", correo);
        cuerpo.put("contrasena", contrasena);

        RespuestaHttp respuesta = transporte.enviar("POST", urlBase + "/api/auth/token",
                Json.escribir(cuerpo), new HashMap<>());
        if (!respuesta.esExito()) {
            ultimoError = "No se pudo iniciar sesion en la web: " + respuesta;
            accessToken = null;
            refreshToken = null;
            return false;
        }
        JsonNode raiz = Json.leer(respuesta.getCuerpo());
        String token = Json.texto(raiz, "access_token");
        if (token == null || token.isEmpty()) {
            ultimoError = "La web no devolvio un token de acceso.";
            return false;
        }
        accessToken = token;
        String refresco = Json.texto(raiz, "refresh_token");
        refreshToken = refresco != null ? refresco : "";
        ultimoError = "";
        return true;
    }

    /**
     * Renueva el token con el refresh token.
     *
     * @return true si el servidor entrego un token nuevo
     */
    public boolean renovar() {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return iniciarSesion();
        }
        Map<String, String> cabeceras = new HashMap<>();
        cabeceras.put("Cookie", "refresh_token=" + refreshToken);
        RespuestaHttp respuesta = transporte.enviar("POST", urlBase + "/api/auth/refresh",
                "", cabeceras);
        JsonNode raiz = Json.leer(respuesta.getCuerpo());
        String token = Json.texto(raiz, "access_token");
        if (respuesta.esExito() && token != null && !token.isEmpty()) {
            accessToken = token;
            ultimoError = "";
            return true;
        }
        // El refresh token tambien expiro: se pide uno nuevo con la clave.
        accessToken = null;
        return iniciarSesion();
    }

    /**
     * Descarta el token para que la proxima peticion lo renueve.
     */
    public void invalidar() {
        accessToken = null;
    }

    /**
     * Cabeceras con el token de acceso.
     *
     * @return cabeceras para las peticiones a la web
     */
    public Map<String, String> cabeceras() {
        Map<String, String> cabeceras = new HashMap<>();
        if (accessToken != null && !accessToken.isEmpty()) {
            cabeceras.put("Cookie", "access_token=" + accessToken);
        }
        return cabeceras;
    }

    public boolean tieneToken() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * @return URL base del servidor web
     */
    public String getUrlBase() {
        return urlBase;
    }

    public String getUltimoError() {
        return ultimoError;
    }
}
