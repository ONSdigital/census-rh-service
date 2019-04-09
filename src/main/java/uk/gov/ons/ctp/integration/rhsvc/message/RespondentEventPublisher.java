package uk.gov.ons.ctp.integration.rhsvc.message;

import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.GenericCaseEvent;

/**
 * Service responsible for the publication of respondent events to the Response Management System.
 */
public interface RespondentEventPublisher {

  /**
   * Method to send event to Response Management
   *
   * @param event CaseEvent to publish.
   */
  void sendEvent(CaseEvent event);
  
  void sendEvent(GenericCaseEvent event);
}
