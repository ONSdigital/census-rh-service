package uk.gov.ons.ctp.integration.rhsvc.representation;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

/** This is a request object which holds data that can be used to create a new case. */
@Data
@NoArgsConstructor
public class CaseRequestDTO {

  @NotNull private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  @NotNull private String townName;
  @NotNull private Region region;
  @NotNull private String postcode;

  @NotNull private UniquePropertyReferenceNumber uprn;

  @NotNull private String estabType;

  @NotNull private AddressType addressType;
}
