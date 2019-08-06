package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import javax.validation.constraints.NotNull;
import lombok.Data;

/** Representation of address data */
@Data
public class AddressDTO {

  @JsonUnwrapped @NotNull private UniquePropertyReferenceNumber uprn;
  @NotNull private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  @NotNull private String postcode;
}
