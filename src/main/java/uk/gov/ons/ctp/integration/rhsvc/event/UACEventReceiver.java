package uk.gov.ons.ctp.integration.rhsvc.event;

import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.integration.rhsvc.cloud.DataStoreContentionException;

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
   * @throws DataStoreContentionException if the data store is overloaded and rejected the object
   *     until the backoff was exhausted.
   */
  public void acceptUACEvent(UACEvent event) throws CTPException, DataStoreContentionException;
}
