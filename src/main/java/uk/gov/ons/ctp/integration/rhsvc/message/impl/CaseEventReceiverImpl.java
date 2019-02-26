package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class CaseEventReceiverImpl implements CaseEventReceiver {

  @Autowired private RespondentEventPublisher publisher;

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param event CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent event) {
    publisher.sendEvent(event);
  }
}
