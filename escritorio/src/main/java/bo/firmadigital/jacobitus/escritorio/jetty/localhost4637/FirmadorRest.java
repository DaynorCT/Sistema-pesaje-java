package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.ErrorRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashCancelacionRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashModoSeguroDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashModoSeguroRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaModoSeguroDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaModoSeguroRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.comun.IRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.servicios.FirmadorServicio;

@Path("/")
public class FirmadorRest {
    private static final String ERROR_GENERICO = "No se pudo completar la solicitud de firma.";
    private static final String ERROR_RESPUESTA_FALLIDA_JSON =
            "{\"message\":\"" + ERROR_GENERICO + "\",\"error\":\"" + ERROR_GENERICO + "\"}";

    @POST
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sign(String body) {
        FirmadorServicio servicio = new FirmadorServicio();
        ObjectMapper om = new ObjectMapper();
        try {
            FirmaModoSeguroDto objetoDto = om.readValue(body, FirmaModoSeguroDto.class);
            IRespuestaDto respuesta = servicio.firmarModoSeguro(objetoDto);
            return respuestaServicio(om, respuesta, FirmaModoSeguroRespuestaDto.class);
        } catch (Exception e) {
            return errorInterno(om, "No se pudo firmar el documento", obtenerDetalleError(e));
        }
    }

    Response respuestaServicio(ObjectMapper om, IRespuestaDto respuesta, Class<?> tipoRespuestaExitosa) throws Exception {
        if (respuesta == null) {
            return errorInterno(om, null, null);
        }
        boolean respuestaEsperada = tipoRespuestaExitosa.isInstance(respuesta)
                || (tipoRespuestaExitosa == FirmaHashModoSeguroRespuestaDto.class
                        && respuesta instanceof FirmaHashCancelacionRespuestaDto);
        if (!(respuesta instanceof ErrorRespuestaDto) && !respuestaEsperada) {
            return errorInterno(om, null, null);
        }

        if (respuesta instanceof ErrorRespuestaDto) {
            ErrorRespuestaDto errorDto = (ErrorRespuestaDto) respuesta;
            if (errorDto.getError() == null) {
                return errorInterno(om, errorDto.getMessage(), null);
            }
            return respuestaError(om, errorDto);
        }

        String error = obtenerError(respuesta);
        if (error != null) {
            ErrorRespuestaDto errorDto = new ErrorRespuestaDto();
            errorDto.setMessage(obtenerMensaje(respuesta));
            errorDto.setError(error);
            return respuestaError(om, errorDto);
        }

        String json = om.writeValueAsString(respuesta);
        return Response.ok(json).type(MediaType.APPLICATION_JSON).build();
    }

    private String obtenerError(IRespuestaDto respuesta) {
        if (respuesta instanceof ErrorRespuestaDto) {
            return ((ErrorRespuestaDto) respuesta).getError();
        }
        if (respuesta instanceof FirmaModoSeguroRespuestaDto) {
            return ((FirmaModoSeguroRespuestaDto) respuesta).getError();
        }
        if (respuesta instanceof FirmaHashCancelacionRespuestaDto) {
            return ((FirmaHashCancelacionRespuestaDto) respuesta).getError();
        }
        return null;
    }

    private String obtenerMensaje(IRespuestaDto respuesta) {
        if (respuesta instanceof FirmaModoSeguroRespuestaDto) {
            return ((FirmaModoSeguroRespuestaDto) respuesta).getMessage();
        }
        if (respuesta instanceof FirmaHashCancelacionRespuestaDto) {
            return ((FirmaHashCancelacionRespuestaDto) respuesta).getMessage();
        }
        return null;
    }

    private Response errorInterno(ObjectMapper om, String message, String error) {
        ErrorRespuestaDto errorDto = new ErrorRespuestaDto();
        errorDto.setMessage(message);
        errorDto.setError(error);
        return respuestaError(om, errorDto);
    }

    private Response respuestaError(ObjectMapper om, ErrorRespuestaDto errorDto) {
        if (errorDto.getMessage() == null) {
            errorDto.setMessage(ERROR_GENERICO);
        }
        if (errorDto.getError() == null) {
            errorDto.setError(ERROR_GENERICO);
        }
        try {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(om.writeValueAsString(errorDto))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(ERROR_RESPUESTA_FALLIDA_JSON)
                    .build();
        }
    }

    private String obtenerDetalleError(Throwable error) {
        if (error == null) {
            return null;
        }
        return error.getMessage();
    }

    @POST
    @Path("/sign-hash")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response signHash(String body) {
        FirmadorServicio servicio = new FirmadorServicio();
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            FirmaHashModoSeguroDto objetoDto = om.readValue(body, FirmaHashModoSeguroDto.class);
            IRespuestaDto respuesta = servicio.firmarHashModoSeguro(objetoDto);
            return respuestaServicio(om, respuesta, FirmaHashModoSeguroRespuestaDto.class);
        } catch (Exception e) {
            StringBuilder ruta = new StringBuilder();
            for (Throwable causa = e; causa != null; causa = causa.getCause()) {
                if (causa instanceof JsonMappingException) {
                    for (JsonMappingException.Reference ref : ((JsonMappingException) causa).getPath()) {
                        if (ruta.length() > 0) {
                            ruta.append(".");
                        }
                        if (ref.getFieldName() != null) {
                            ruta.append(ref.getFieldName());
                        } else {
                            ruta.append("[").append(ref.getIndex()).append("]");
                        }
                    }
                }
            }
            if (ruta.length() > 0) {
                return errorInterno(om, "Error en la petición", "El campo '" + ruta + "' tiene un valor con formato incorrecto.");
            }
            return errorInterno(om, "Error en la petición", obtenerDetalleError(e));
        }
    }
}
