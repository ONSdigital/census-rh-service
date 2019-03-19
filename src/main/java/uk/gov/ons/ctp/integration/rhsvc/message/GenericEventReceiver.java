package uk.gov.ons.ctp.integration.rhsvc.message;

import uk.gov.ons.ctp.integration.rhsvc.message.impl.GenericEvent;

/**
 * Interface for the receipt of Case Events. See Spring Integration flow for details of in bound
 * queue.
 */
public interface GenericEventReceiver {

  /**
   * Method called with the deserialised message received from the Case service
   *
   * @param event CasesEvent object received
   */
  void acceptCaseEvent(GenericEvent event);
}
