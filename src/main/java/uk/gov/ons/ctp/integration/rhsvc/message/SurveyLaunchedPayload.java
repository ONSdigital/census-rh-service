package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyLaunchedPayload {

  private SurveyLaunchedResponse response = new SurveyLaunchedResponse();
}
