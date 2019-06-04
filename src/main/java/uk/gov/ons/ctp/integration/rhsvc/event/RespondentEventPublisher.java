package uk.gov.ons.ctp.integration.rhsvc.event;

import uk.gov.ons.ctp.common.event.model.GenericEvent;

/** Service responsible for the publication of respondent events to the Events exchange. */
public interface RespondentEventPublisher {
  /**
   * Method to publish a respondent Event onto the events exchange.
   *
   * @param event is the event to publish.
   */
  void sendEvent(GenericEvent event);
}
