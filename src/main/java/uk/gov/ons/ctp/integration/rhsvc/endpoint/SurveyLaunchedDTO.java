package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import java.io.Serializable;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * The request object passed into the service when a survey is launched.
 */
@Data
@SuppressWarnings("serial")
public class SurveyLaunchedDTO implements Serializable {

  @NotNull
  private String questionnaireId;

  private UUID caseId;
}
