package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class FirmaHashItemListDeserializer
        extends JsonDeserializer<List<FirmaHashItemDto>> {

    @Override
    public List<FirmaHashItemDto> deserialize(
            JsonParser parser,
            DeserializationContext context) throws IOException {

        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = mapper.readTree(parser);

        if (node.isArray()) {
            return mapper.convertValue(
                    node,
                    new TypeReference<List<FirmaHashItemDto>>() {}
            );
        }

        if (node.isObject()) {
            FirmaHashItemDto item =
                    mapper.treeToValue(node, FirmaHashItemDto.class);

            return Collections.singletonList(item);
        }

        throw JsonMappingException.from(
                parser,
                "El campo archivo debe ser un objeto o un array"
        );
    }
}
