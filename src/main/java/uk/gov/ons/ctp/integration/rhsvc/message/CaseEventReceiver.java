package uk.gov.ons.ctp.integration.rhsvc.message;

import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.CaseEvent;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public interface CaseEventReceiver {

  /**
   * Message end point for events from Response Management.
   *
   * @param event CaseEvent message from Response Management
   */
  public void acceptCaseEvent(CaseEvent event);
}
