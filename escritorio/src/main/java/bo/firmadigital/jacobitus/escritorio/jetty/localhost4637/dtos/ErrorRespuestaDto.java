package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos;

import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.comun.IRespuestaDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorRespuestaDto implements IRespuestaDto {
    private String message;
    private String error;
}
