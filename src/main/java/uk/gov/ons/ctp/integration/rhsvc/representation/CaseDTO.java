package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;
import lombok.Data;

/** Representation of a Case */
@Data
public class CaseDTO {

  private UUID id;

  private String caseRef;

  private String addressType;

  private String state;

  private String addressLine1;

  private String addressLine2;

  private String addressLine3;

  private String townName;

  private String region;

  private String postcode;

  @JsonUnwrapped private UniquePropertyReferenceNumber uprn;
}
