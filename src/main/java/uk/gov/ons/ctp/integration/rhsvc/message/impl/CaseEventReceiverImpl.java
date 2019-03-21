package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.integration.rhsvc.message.GenericEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class CaseEventReceiverImpl implements GenericEventReceiver {

  @Autowired private RespondentEventPublisher publisher;
  private static final Logger log = LoggerFactory.getLogger(CaseEventReceiverImpl.class);

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param event CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(GenericEvent event) {

    String eventType = "undefined";

    log.info("Receiving a GenericEvent from the Case.Gateway queue...");

    log.info("The event being received is: " + event.toString());

    eventType = event.getEvent().getType();

    log.info("The type of event received is: " + eventType);

    publisher.sendEvent(event);
  }

  //  @ServiceActivator(inputChannel = "acceptCaseEvent")
  //  public void acceptCaseEvent(CaseEvent event) {
  //    publisher.sendEvent(event);
  //  }
}
