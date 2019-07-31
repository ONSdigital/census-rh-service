package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * If this class is included Jackson serialisation then you may want to simplify the generated
 * string by removing an extra layer of output, so that the generated string can contain a 'uprn'
 * value instead of showing the hierarchy of ownership. To do this annotate references to this class
 * with '@JsonUnwrapped'.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class UniquePropertyReferenceNumber {

  private static final long UPRN_MIN = 0L;
  private static final long UPRN_MAX = 999999999999L;

  @JsonCreator
  public UniquePropertyReferenceNumber(@JsonProperty(value = "uprn", required = true) String str) {
    if (!StringUtils.isBlank(str)) {
      try {
        Long uprn = Long.parseLong(str);
        if (uprn.longValue() >= UPRN_MIN && uprn.longValue() <= UPRN_MAX) {
          this.value = uprn;
        } else {
          throw new IllegalArgumentException("String '" + uprn + "' is not a valid UPRN");
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException();
      }
    }
  }

  @JsonProperty("uprn")
  @JsonSerialize(using = ToStringSerializer.class)
  private long value;
}
