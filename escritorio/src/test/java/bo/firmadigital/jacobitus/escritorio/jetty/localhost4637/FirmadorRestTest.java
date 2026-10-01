package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashModoSeguroRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashCancelacionRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaModoSeguroRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.ErrorRespuestaDto;

import javax.ws.rs.core.Response;

public class FirmadorRestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void signDevuelve500ConErrorTextoCuandoElJsonEsInvalido() throws Exception {
        assertErrorNoNulo(new FirmadorRest().sign("{"));
    }

    @Test
    public void signHashDevuelve500ConErrorTextoCuandoElJsonEsInvalido() throws Exception {
        assertErrorNoNulo(new FirmadorRest().signHash("{"));
    }

    @Test
    public void respuestaNulaDelServicioDevuelve500ConErrorTexto() throws Exception {
        Response response = new FirmadorRest().respuestaServicio(
                objectMapper, null, FirmaHashModoSeguroRespuestaDto.class);

        assertErrorNoNulo(response);
    }

    @Test
    public void respuestasExitosasDeFirmaExponenLosCamposDelContrato() throws Exception {
        assertErrorNulo(objectMapper.writeValueAsString(new FirmaModoSeguroRespuestaDto()));
        JsonNode respuestaHash = objectMapper.readTree(
                objectMapper.writeValueAsString(new FirmaHashModoSeguroRespuestaDto()));
        assertFalse(respuestaHash.has("error"));
        assertEquals(4, respuestaHash.size());
        assertTrue(respuestaHash.has("files"));
        assertTrue(respuestaHash.has("certificate"));
        assertTrue(respuestaHash.has("certificateChain"));
        assertTrue(respuestaHash.has("encryptionAlgorithm"));
    }

    @Test
    public void respuestaDeFirmaComoEnMasterConservaCancelacionYDevuelve200() throws Exception {
        FirmaModoSeguroRespuestaDto respuesta = new FirmaModoSeguroRespuestaDto();
        respuesta.setMessage("Se canceló la firma del documento");
        respuesta.setFiles(Collections.emptyList());

        Response response = new FirmadorRest().respuestaServicio(
                objectMapper, respuesta, FirmaModoSeguroRespuestaDto.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode json = objectMapper.readTree((String) response.getEntity());
        assertEquals("Se canceló la firma del documento", json.get("message").getTextValue());
        assertTrue(json.get("files").isArray());
        assertEquals(0, json.get("files").size());
        assertTrue(json.get("error").isNull());
        assertEquals(3, json.size());
    }

    @Test
    public void respuestaDeFirmaHashAlCancelarDevuelve200ConErrorNulo() throws Exception {
        FirmaHashCancelacionRespuestaDto respuesta = new FirmaHashCancelacionRespuestaDto();
        respuesta.setMessage("Se canceló la firma del documento");
        respuesta.setFiles(Collections.emptyList());

        Response response = new FirmadorRest().respuestaServicio(
                objectMapper, respuesta, FirmaHashModoSeguroRespuestaDto.class);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode json = objectMapper.readTree((String) response.getEntity());
        assertEquals("Se canceló la firma del documento", json.get("message").getTextValue());
        assertTrue(json.get("error").isNull());
        assertTrue(json.get("files").isArray());
        assertEquals(0, json.get("files").size());
        assertEquals(3, json.size());
    }

    @Test
    public void respuestaDeErrorDelServicioSinDetalleDevuelve500ConErrorTexto() throws Exception {
        ErrorRespuestaDto respuesta = new ErrorRespuestaDto();
        respuesta.setMessage("No se pudo firmar el documento");

        Response response = new FirmadorRest().respuestaServicio(
                objectMapper, respuesta, FirmaHashModoSeguroRespuestaDto.class);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertErrorNoNulo(response);
    }

    @Test
    public void firmaConErrorNoNuloConservaElCampoYDevuelve500() throws Exception {
        FirmaModoSeguroRespuestaDto respuesta = new FirmaModoSeguroRespuestaDto();
        respuesta.setMessage("No se pudo firmar el documento");
        respuesta.setError("error original");

        Response response = new FirmadorRest().respuestaServicio(
                objectMapper, respuesta, FirmaModoSeguroRespuestaDto.class);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        JsonNode json = objectMapper.readTree((String) response.getEntity());
        assertEquals(2, json.size());
        assertEquals("No se pudo firmar el documento", json.get("message").getTextValue());
        assertEquals("error original", json.get("error").getTextValue());
    }

    @Test
    public void errorDelServicioSeDevuelveSinModificarloConEstado500() throws Exception {
        ErrorRespuestaDto respuesta = new ErrorRespuestaDto();
        respuesta.setMessage("mensaje de error");
        respuesta.setError("detalle original");

        Response response = new FirmadorRest().respuestaServicio(
                objectMapper, respuesta, FirmaHashModoSeguroRespuestaDto.class);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        JsonNode json = objectMapper.readTree((String) response.getEntity());
        assertEquals("mensaje de error", json.get("message").getTextValue());
        assertEquals("detalle original", json.get("error").getTextValue());
    }

    private void assertErrorNoNulo(Response response) throws Exception {
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        JsonNode json = objectMapper.readTree((String) response.getEntity());
        assertNotNull(json.get("message"));
        assertTrue(json.get("message").isTextual());
        assertFalse(json.get("message").getTextValue().trim().isEmpty());
        assertNotNull(json.get("error"));
        assertTrue(json.get("error").isTextual());
        assertFalse(json.get("error").getTextValue().trim().isEmpty());
    }

    private void assertErrorNulo(String response) throws Exception {
        JsonNode json = objectMapper.readTree(response);
        assertNotNull(json.get("error"));
        assertTrue(json.get("error").isNull());
    }
}
