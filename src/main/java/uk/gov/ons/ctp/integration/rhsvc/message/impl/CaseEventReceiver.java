package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

// import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
// import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSDataStore;
// import java.util.Optional;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class CaseEventReceiver {

  @Autowired private RespondentEventPublisher publisher;
  @Autowired private RespondentDataServiceImpl cloud;
  private static final Logger log = LoggerFactory.getLogger(CaseEventReceiver.class);

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param event CaseEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptCaseEvent")
  public void acceptCaseEvent(CaseEvent event) {

    log.info("Receiving a CaseEvent from the Case.Gateway queue...");

    log.info("The event being received is: " + event.toString());

    log.info("Now store the event in Google Cloud..");
    storeCaseEvent(event);
    log.info("The event has been stored and retrieved successfully");

    //    publisher.sendEvent(event);
  }

  public void storeCaseEvent(CaseEvent caseEvent) {

    String caseBucket = "case_bucket";
    CollectionCase collectionCase = caseEvent.getPayload().getCollectionCase();
    //    String caseId = caseEvent.getPayload().getCollectionCase().getId();
    String caseId = collectionCase.getId();

    log.info("The value of caseId is: " + caseId);

    String caseContent = caseEvent.toString();

    log.info("The value of caseContent is: " + caseContent);

    try {
      cloud.writeCollectionCase(
          collectionCase); // need to catch uk.gov.ons.ctp.common.error.CTPException
      Optional<CollectionCase> collectionCaseOpt = cloud.readCollectionCase(caseId);
    } catch (CTPException ctpEx) {
      log.info("ERROR: " + ctpEx.getMessage());
    }

    //    CloudDataStore cloudDataStore = new GCSDataStore();
    //
    //    cloudDataStore.storeObject(caseBucket, caseId, caseContent);
    //
    //    Optional<String> value = cloudDataStore.retrieveObject(caseBucket, caseId);

    log.info("The JSON blob has been retrieved from the bucket");
    // log.info("The value retrieved from GCS is: " + value.get());
  }
}
