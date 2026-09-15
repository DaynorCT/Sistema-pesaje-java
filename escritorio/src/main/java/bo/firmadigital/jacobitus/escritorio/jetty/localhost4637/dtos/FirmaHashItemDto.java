package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FirmaHashItemDto {
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String id = null;
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String base64;
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String date;
}
