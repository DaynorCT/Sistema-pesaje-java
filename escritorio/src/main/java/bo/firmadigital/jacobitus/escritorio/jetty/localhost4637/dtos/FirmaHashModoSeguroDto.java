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
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String ci;
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String format;
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String language;
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String digest;
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String pin;
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String alias;

    @JsonDeserialize(using = FirmaHashItemListDeserializer.class)
    private List<FirmaHashItemDto> archivo;
}
