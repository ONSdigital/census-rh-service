package uk.gov.ons.ctp.integration.rhsvc.event;

import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;

/**
 * Service responsible for the publication of respondent events to the Response Management System.
 */
public interface RespondentEventPublisher {
  /**
   * Method to publish a SurveyLauched Event.
   *
   * @param surveyLaunchedEvent is the event to publish.
   */
  void sendEvent(SurveyLaunchedEvent surveyLaunchedEvent);
}
