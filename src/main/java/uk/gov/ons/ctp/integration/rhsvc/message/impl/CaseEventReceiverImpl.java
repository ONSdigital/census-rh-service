package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@MessageEndpoint
public class CaseEventReceiverImpl implements CaseEventReceiver {

  @Autowired private RespondentDataService respondentDataService;
  private static final Logger log = LoggerFactory.getLogger(CaseEventReceiverImpl.class);

  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent caseEvent) throws CTPException {

    CollectionCase collectionCase;
    String caseType = caseEvent.getEvent().getType();
    String caseTransactionId = caseEvent.getEvent().getTransactionId();

    log.with(caseType)
        .with(caseTransactionId)
        .info("Now receiving case event with transactionId and type as shown here");

    collectionCase = caseEvent.getPayload().getCollectionCase();

    try {
      respondentDataService.writeCollectionCase(collectionCase);
    } catch (CTPException ctpEx) {
      log.with(caseTransactionId)
          .with(ctpEx.getMessage())
          .error("ERROR: The event processing, for this transactionId, has failed");
      throw new CTPException(ctpEx.getFault());
    }
  }
}
