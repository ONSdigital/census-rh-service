package uk.gov.ons.ctp.integration.rhsvc.message;

import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.CaseEvent;

// import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
// import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSDataStore;
// import java.util.Optional;

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
