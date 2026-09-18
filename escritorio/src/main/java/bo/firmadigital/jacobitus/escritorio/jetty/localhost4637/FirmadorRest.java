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

import bo.firmadigital.jacobitus.escritorio.jetty.JettyHelper;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.ErrorRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashModoSeguroDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaModoSeguroDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.comun.IRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.servicios.FirmadorServicio;

@Path("/")
public class FirmadorRest {
    @POST
    @Path("/sign")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String sign(String body) {
        FirmadorServicio servicio = new FirmadorServicio();
        ObjectMapper om = new ObjectMapper();
        try {
            FirmaModoSeguroDto objetoDto = om.readValue(body, FirmaModoSeguroDto.class);
            return om.writeValueAsString(servicio.firmarModoSeguro(objetoDto));
        } catch (Exception e) {
            return JettyHelper.generarRespuesta(e.getMessage());
        }
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
            String json = om.writeValueAsString(respuesta);
            if (respuesta instanceof ErrorRespuestaDto) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(json).build();
            }
            return Response.ok(json).build();
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
                return errorInternoSignHash(om, "El campo '" + ruta + "' tiene un valor con formato incorrecto.");
            }
            return errorInternoSignHash(om, e.getMessage());
        }
    }

    private Response errorInternoSignHash(ObjectMapper om, String error) {
        ErrorRespuestaDto errorDto = new ErrorRespuestaDto();
        errorDto.setMessage("Error en la petición");
        errorDto.setError(error);
        try {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(om.writeValueAsString(errorDto)).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(JettyHelper.generarRespuesta(error)).build();
        }
    }
}
