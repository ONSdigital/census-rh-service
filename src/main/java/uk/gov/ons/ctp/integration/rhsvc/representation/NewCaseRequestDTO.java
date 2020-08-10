package uk.gov.ons.ctp.integration.rhsvc.representation;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Region;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;

/** The request object when the creation of a new case is requested. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCaseRequestDTO {
  @NotNull String addressLine1;
  String addressLine2;
  String addressLine3;
  @NotNull String townName;
  @NotNull Region region;
  @NotNull String postcode;
  @NotNull UniquePropertyReferenceNumber uprn;
  @NotNull String estabType;
}
