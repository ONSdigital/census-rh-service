package uk.gov.ons.ctp.integration.rhsvc.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UAC {

  private String uacHash;
  private String active;
  private String questionnaireId;
  private String caseType;
  private String region;
  private String caseId;
  private String collectionExerciseId;
}
