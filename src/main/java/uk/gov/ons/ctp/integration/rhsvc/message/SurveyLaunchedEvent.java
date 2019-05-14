package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.message.GenericEvent;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SurveyLaunchedEvent extends GenericEvent {

  private SurveyLaunchedPayload payload = new SurveyLaunchedPayload();
}
