package uk.gov.ons.ctp.integration.rhsvc.message;

import uk.gov.ons.ctp.integration.rhsvc.message.impl.CaseEvent;

/**
 * Interface for the receipt of Case Events. See Spring Integration flow for details of in bound
 * queue.
 */
public interface CaseEventReceiver {

  /**
   * Method called with the deserialised message received from the Case service
   *
   * @param event CasesEvent object received
   */
  void acceptCaseEvent(CaseEvent event);
}
