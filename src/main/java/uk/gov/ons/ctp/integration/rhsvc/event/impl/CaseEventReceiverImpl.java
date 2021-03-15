package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.event.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@Data
@AllArgsConstructor
@MessageEndpoint
public class CaseEventReceiverImpl implements CaseEventReceiver {

  private static final Logger log = LoggerFactory.getLogger(CaseEventReceiverImpl.class);

  @Autowired private RespondentDataRepository respondentDataRepo;
  
  public CaseEventReceiverImpl() {
    log.info("PMB: Start CaseEventReceiverImpl");
  }
  
  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   * @throws CTPException something went wrong
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent caseEvent) throws CTPException {

    CollectionCase collectionCase = caseEvent.getPayload().getCollectionCase();
    String caseTransactionId = caseEvent.getEvent().getTransactionId();

    log.with("transactionId", caseTransactionId)
        .with("caseId", collectionCase.getId())
        .info("Entering acceptCaseEvent");

    try {
      respondentDataRepo.writeCollectionCase(collectionCase);
    } catch (CTPException ctpEx) {
      log.with("caseTransactionId", caseTransactionId).error(ctpEx, "Case Event processing failed");
      throw new CTPException(ctpEx.getFault());
    }
  }
}
