package json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;

/**
 * This class extends StdDeserializer<> to deserialize a java.time.LocalDate.
 *  
 * @author Rebecca Chandler
 *
 */
public class LocalDateDeserializer extends StdDeserializer<LocalDate> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 7673397715560419773L;

	protected LocalDateDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return LocalDate.parse(parser.readValueAs(String.class));
    }
}