
package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos;

import java.util.List;

import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.comun.IRespuestaDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FirmaHashModoSeguroRespuestaDto implements IRespuestaDto {
    private List<FirmaHashItemRespuestaDto> files;
    private String certificate;
    private List<String> certificateChain;
    private String encryptionAlgorithm;
}
