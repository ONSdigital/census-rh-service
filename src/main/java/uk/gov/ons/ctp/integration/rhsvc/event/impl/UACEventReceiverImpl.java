package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.integration.rhsvc.cloud.DataStoreContentionException;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/**
 * Service implementation responsible for receipt of UAC Events. See Spring Integration flow for
 * details of in bound queue.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class UACEventReceiverImpl {

  @Autowired private RespondentDataRepository respondentDataRepo;
  private static final Logger log = LoggerFactory.getLogger(UACEventReceiverImpl.class);

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param uacEvent UACEvent message from Response Management
   * @throws CTPException something went wrong
   * @throws DataStoreContentionException if the data store is overloaded and rejected the object
   *     until the backoff was exhausted.
   */
  @ServiceActivator(inputChannel = "acceptUACEvent")
  public void acceptUACEvent(UACEvent uacEvent) throws CTPException, DataStoreContentionException {

    UAC uac = uacEvent.getPayload().getUac();
    String uacTransactionId = uacEvent.getEvent().getTransactionId();

    log.with("transactionId", uacTransactionId)
        .with("caseId", uac.getCaseId())
        .info("Entering acceptUACEvent");

    try {
      respondentDataRepo.writeUAC(uac);
    } catch (CTPException ctpEx) {
      log.with(uacTransactionId).with(ctpEx.getMessage()).error("UAC Event processing failed");
      throw new CTPException(ctpEx.getFault());
    }
  }
}
