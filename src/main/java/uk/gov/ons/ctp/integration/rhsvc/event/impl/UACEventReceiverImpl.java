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
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
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

  private static final Logger log = LoggerFactory.getLogger(UACEventReceiverImpl.class);

  @Autowired private RespondentDataRepository respondentDataRepo;

  @Autowired private AppConfig appConfig;

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param uacEvent UACEvent message (either created or updated type)from Response Management
   * @throws CTPException something went wrong
   */
  @ServiceActivator(inputChannel = "acceptUACEvent")
  public void acceptUACEvent(UACEvent uacEvent) throws CTPException {

    UAC uac = uacEvent.getPayload().getUac();
    String uacTransactionId = uacEvent.getEvent().getTransactionId();

    log.with("transactionId", uacTransactionId)
        .with("caseId", uac.getCaseId())
        .info("Entering acceptUACEvent");

    String qid = uac.getQuestionnaireId();
    if (isFilteredByQid(qid)) {
      log.with("transactionId", uacTransactionId)
          .with("caseId", uac.getCaseId())
          .with("questionnaireId", qid)
          .info("Filtering UAC Event because of questionnaire ID prefix");
      return;
    }

    try {
      respondentDataRepo.writeUAC(uac);
    } catch (CTPException ctpEx) {
      log.with("uacTransactionId", uacTransactionId).with(ctpEx.getMessage()).error("UAC Event processing failed");
      throw new CTPException(ctpEx.getFault());
    }
  }

  private boolean isFilteredByQid(String qid) {
    return qid != null
        && qid.length() > 2
        && appConfig.getQueueConfig().getQidFilterPrefixes().contains(qid.substring(0, 2));
  }
}
