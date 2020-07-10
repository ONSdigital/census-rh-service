package uk.gov.ons.ctp.integration.rhsvc.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

/** Json serializer to return a String for the long value in a UniquePropertyReferenceNumber */
public class UniquePropertyReferenceNumberSerializer
    extends StdSerializer<UniquePropertyReferenceNumber> {

  private static final long serialVersionUID = -7510831289144949267L;

  public UniquePropertyReferenceNumberSerializer() {
    this(null);
  }

  public UniquePropertyReferenceNumberSerializer(Class<UniquePropertyReferenceNumber> t) {
    super(t);
  }

  @Override
  public void serialize(
      UniquePropertyReferenceNumber value, JsonGenerator gen, SerializerProvider arg2)
      throws IOException, JsonProcessingException {
    gen.writeString(Long.toString(value.getValue()));
  }
}
