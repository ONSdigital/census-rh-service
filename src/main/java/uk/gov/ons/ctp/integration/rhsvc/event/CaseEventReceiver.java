package uk.gov.ons.ctp.integration.rhsvc.event;

import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.cloud.DataStoreContentionException;

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
   * @throws CTPException something went wrong
   * @throws DataStoreContentionException if the data store is overloaded and rejected the object
   *     until the backoff was exhausted.
   */
  public void acceptCaseEvent(CaseEvent event) throws CTPException, DataStoreContentionException;
}
