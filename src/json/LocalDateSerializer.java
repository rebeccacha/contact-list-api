package json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * This class extends StdSerializer<> to serialize a java.time.LocalDate.
 *  
 * @author Rebecca Chandler
 *
 */
public class LocalDateSerializer extends StdSerializer<LocalDate> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8424922601735087613L;

	public LocalDateSerializer() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(LocalDate value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
}