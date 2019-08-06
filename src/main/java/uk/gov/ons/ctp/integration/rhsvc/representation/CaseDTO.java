package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;
import lombok.Data;

/** Representation of a Case */
@Data
public class CaseDTO {

  private UUID caseId;

  private String caseRef;

  private String addressType;

  private String state;

  @JsonUnwrapped private AddressDTO address;

  private String region;
}
