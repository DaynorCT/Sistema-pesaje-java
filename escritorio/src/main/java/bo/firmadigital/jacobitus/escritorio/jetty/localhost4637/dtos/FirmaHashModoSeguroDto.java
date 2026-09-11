package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FirmaHashModoSeguroDto {
    private String ci;
    private String format;
    private String language;
    private String digest;

    @JsonDeserialize(using = FirmaHashItemListDeserializer.class)
    private List<FirmaHashItemDto> archivo;
}
