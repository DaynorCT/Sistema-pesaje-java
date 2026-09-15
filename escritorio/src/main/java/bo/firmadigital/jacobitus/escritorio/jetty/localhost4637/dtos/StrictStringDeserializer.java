package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;

public class StrictStringDeserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(parser, "El campo debe ser de tipo texto");
        }
        return parser.getText();
    }
}
