package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.ons.ctp.common.domain.EstabType;

/** Representation of a Case */
@Data
public class CaseDTO {

  private UUID caseId;

  @LoggingScope(scope = Scope.HASH)
  private String caseRef;

  private String caseType;

  private String addressType;

  @JsonUnwrapped private AddressDTO address;

  private String region;

  private String addressLevel;

  @NotNull private EstabType estabType = EstabType.OTHER;
}
