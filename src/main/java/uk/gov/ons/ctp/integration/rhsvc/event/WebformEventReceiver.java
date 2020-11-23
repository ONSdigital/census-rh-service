package uk.gov.ons.ctp.integration.rhsvc.event;

import uk.gov.ons.ctp.common.event.model.WebformEvent;

/** Interface for the receipt of webform events from inbound message queue. */
public interface WebformEventReceiver {

  /**
   * Message end point for Webform requests
   *
   * @param event WebformEvent request
   */
  void acceptWebformEvent(WebformEvent event);
}
