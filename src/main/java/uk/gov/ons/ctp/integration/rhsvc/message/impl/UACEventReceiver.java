package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSDataStore;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;

/**
 * Service implementation responsible for receipt of UAC Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class UACEventReceiver {

  @Autowired private RespondentEventPublisher publisher;
  private static final Logger log = LoggerFactory.getLogger(UACEventReceiver.class);

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param event UACEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptUACEvent")
  public void acceptUACEvent(UACEvent event) {

    String eventType = "undefined";

    log.info("Receiving a UACEvent from the UAC.Gateway queue...");

    log.info("The event being received is: " + event.toString());

    eventType = event.getEvent().getType();

    log.info("The type of event received is: " + eventType);

    log.info("Now store the event in Google Cloud..");
    storeUACEvent(event);
    log.info("The event has been stored successfully");

    publisher.sendEvent(event);
  }

  public void storeUACEvent(UACEvent uacEvent) {

    String uacBucket = "uac_bucket";
    String caseId = uacEvent.getPayload().getUac().getCaseId();

    log.info("The value of caseId is: " + caseId);

    String uacContent = uacEvent.toString();

    log.info("The value of uacContent is: " + uacContent);

    CloudDataStore cloudDataStore = new GCSDataStore();

    cloudDataStore.storeObject(uacBucket, caseId, uacContent);

    Optional<String> value = cloudDataStore.retrieveObject(uacBucket, caseId);

    log.info("The blob has been retrieved from the bucket");
    // log.info("The value retrieved from GCS is: " + value.get());
  }
}
