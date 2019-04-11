package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class CaseEventReceiverImpl implements CaseEventReceiver {

  @Autowired private RespondentDataServiceImpl respondentDataServiceImpl;
  private static final Logger log = LoggerFactory.getLogger(CaseEventReceiverImpl.class);

  /**
   * Message end point for events from Response Management.
   *
   * @param caseEvent CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent caseEvent) {

    CollectionCase collectionCase;

    log.info("The event being received is: " + caseEvent.toString());

    collectionCase = caseEvent.getPayload().getCollectionCase();

    try {
      respondentDataServiceImpl.writeCollectionCase(
          collectionCase); // need to catch uk.gov.ons.ctp.common.error.CTPException
    } catch (CTPException ctpEx) {
      log.with(ctpEx.getMessage()).info("ERROR");
    }
  }
}