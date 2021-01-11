package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.io.Serializable;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/** This is a request object which holds details about a launched survey. */
@Data
@NoArgsConstructor
@SuppressWarnings("serial")
public class SurveyLaunchedDTO implements Serializable {

  @NotNull private String questionnaireId;

  @NotNull private UUID caseId;

  @NotNull private String agentId;

  private String clientIP;
}
