package uk.gov.ons.ctp.integration.rhsvc.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UACContext implements Serializable {
  private String universalAccessCode;
  private String questionnaireId;
  private String transactionId;
  private String caseType;
  private String region;
  private String caseId;
  private String timestamp;
}
