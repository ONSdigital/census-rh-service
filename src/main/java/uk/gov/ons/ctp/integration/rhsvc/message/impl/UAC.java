package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UAC {

  private String uacHash =
      "72C84BA99D77EE766E9468A0DE36433A44888E5DEC4AFB84F8019777800B7364"; // SHA-256 hash of UAC
  private String active = "true";
  private String questionnaireId = "1110000009";
  private String caseType = "H";
  private String region = "E";
  private String caseId = "c45de4dc-3c3b-11e9-b210-d663bd873d93";
  private String collectionExerciseId = "n66de4dc-3c3b-11e9-b210-d663bd873d93";
}
