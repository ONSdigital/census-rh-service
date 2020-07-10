package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import javax.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

/** Representation of address data */
@Data
public class AddressDTO {

  @NotNull private UniquePropertyReferenceNumber uprn;
  @NotNull private String addressLine1;

  @LoggingScope(scope = Scope.SKIP)
  private String addressLine2;

  @LoggingScope(scope = Scope.SKIP)
  private String addressLine3;

  @LoggingScope(scope = Scope.SKIP)
  private String townName;

  @NotNull private String postcode;
}
