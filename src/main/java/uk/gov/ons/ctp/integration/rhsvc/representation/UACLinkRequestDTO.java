package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/** This is a request object which holds details for a UAC link to case request. */
@Data
@NoArgsConstructor
public class UACLinkRequestDTO {

  @NotNull private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  @NotNull private String townName;
  @NotNull private String region;
  @NotNull private String postcode;

  @JsonUnwrapped @NotNull private UniquePropertyReferenceNumber uprn;

  @NotNull private String estabType;

  @NotNull private String addressType;
}
