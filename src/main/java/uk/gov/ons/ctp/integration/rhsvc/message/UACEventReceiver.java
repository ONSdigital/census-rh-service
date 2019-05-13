package uk.gov.ons.ctp.integration.rhsvc.message;

import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;

/**
 * Service implementation responsible for receipt of UAC Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public interface UACEventReceiver {

  /**
   * Message end point for events from Response Management.
   *
   * @param event UACEvent message from Response Management
   * @throws CTPException something went wrong
   */
  public void acceptUACEvent(UACEvent event) throws CTPException;
}
