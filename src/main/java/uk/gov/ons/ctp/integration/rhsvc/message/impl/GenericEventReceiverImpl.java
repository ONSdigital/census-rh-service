package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.integration.rhsvc.message.GenericEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;

/**
 * Service implementation responsible for receipt of Case Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class GenericEventReceiverImpl implements GenericEventReceiver {

  @Autowired private RespondentEventPublisher publisher;
  private static final Logger log = LoggerFactory.getLogger(GenericEventReceiverImpl.class);

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param event GenericEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptGenericEvent")
  public void acceptGenericEvent(GenericEvent event) {

    //      String eventType = "undefined";

    log.info("Receiving a GenericEvent from the Case.Gateway queue...");

    log.info("The event being received is: " + event.toString());
    //
    //      eventType = event.getEvent().getType();
    //
    //      log.info("The type of event received is: " + eventType);
    //
    //      publisher.sendEvent(event);
  }

  //  /**
  //   * Message end point for events from Response Management. At present sends straight to
  // publisher
  //   * to prove messaging setup.
  //   *
  //   * @param event CaseEvent message from Response Management
  //   */
  //  @ServiceActivator(inputChannel = "acceptCaseEvent")
  //  public void acceptCaseEvent(CaseEvent event) {
  //
  //    String eventType = "undefined";
  //
  //    log.info("Receiving a CaseEvent from the Case.Gateway queue...");
  //
  //    log.info("The event being received is: " + event.toString());
  //
  //    eventType = event.getEvent().getType();
  //
  //    log.info("The type of event received is: " + eventType);
  //
  //    log.info("Now store the event in Google Cloud..");
  //    storeCaseEvent(event);
  //    log.info("The event has been stored successfully");
  //
  //    publisher.sendEvent(event);
  //  }

  //  public void storeCaseEvent(CaseEvent caseEvent) {
  //
  //    String caseBucket = "case_bucket";
  //    String caseId = caseEvent.getPayload().getCollectionCase().getId();
  //
  //    log.info("The value of caseId is: " + caseId);
  //
  //    String caseContent = caseEvent.toString();
  //
  //    log.info("The value of caseContent is: " + caseContent);
  //
  //    CloudDataStore cloudDataStore = new GCSDataStore();
  //
  //    cloudDataStore.storeObject(caseBucket, caseId, caseContent);
  //
  //    Optional<String> value = cloudDataStore.retrieveObject(caseBucket, caseId);
  //
  //    log.info("The value retrieved from GCS is: " + value.get());
  //  }

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param event UACEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptUACEvent")
  public void acceptUacEvent(UACEvent event) {

    String eventType = "undefined";

    log.info("Receiving a UACEvent from the UAC.Gateway queue...");

    log.info("The event being received is: " + event.toString());
    //
    //    eventType = event.getEvent().getType();
    //
    //    log.info("The type of event received is: " + eventType);
    //
    //    log.info("Now store the event in Google Cloud..");
    //    storeUacEvent(event);
    //    log.info("The event has been stored successfully");
    //
    //    publisher.sendEvent(event);
  }

  public void storeUacEvent(UACEvent uacEvent) {

    String uacBucket = "uac_bucket";
    // String caseId = caseEvent.getPayload().getCollectionCase().getId();
    String caseId = uacEvent.getUacPayload().getUac().getCaseId();

    log.info("The value of caseId is: " + caseId);

    String uacContent = uacEvent.toString();

    log.info("The value of uacContent is: " + uacContent);

    // CloudDataStore cloudDataStore = new GCSDataStore();

    // cloudDataStore.storeObject(caseBucket, caseId, caseContent);
    // Optional<String> value = cloudDataStore.retrieveObject(caseBucket, caseId);

    // log.info("The value retrieved from GCS is: " + value.get());
  }
}
