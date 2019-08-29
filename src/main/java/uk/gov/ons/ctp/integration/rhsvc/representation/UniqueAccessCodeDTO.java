package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.UUID;
import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import lombok.Data;

/** Representation of a UAC claim request */
@Data
public class UniqueAccessCodeDTO {

  /** enum for valid case status */
  public enum CaseStatus {
    OK,
    UNKNOWN,
    NOT_FOUND
  }

  @LoggingScope(scope = Scope.SKIP)
  private String uac;
  private boolean active;
  private CaseStatus caseStatus;
  private String questionnaireId;
  private String caseType;
  private String region;
  private UUID caseId;
  private UUID collectionExerciseId;
  private AddressDTO address;
}
