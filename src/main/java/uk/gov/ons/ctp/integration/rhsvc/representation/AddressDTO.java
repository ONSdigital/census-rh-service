package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import javax.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.rhsvc.util.UniquePropertyReferenceNumberSerializer;

/** Representation of address data */
@Data
public class AddressDTO {

  @JsonSerialize(using = UniquePropertyReferenceNumberSerializer.class)
  @NotNull
  private UniquePropertyReferenceNumber uprn;

  @NotNull private String addressLine1;

  @LoggingScope(scope = Scope.SKIP)
  private String addressLine2;

  @LoggingScope(scope = Scope.SKIP)
  private String addressLine3;

  @LoggingScope(scope = Scope.SKIP)
  private String townName;

  @NotNull private String postcode;
}
