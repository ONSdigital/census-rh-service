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

/**
 * Service implementation responsible for receipt of UAC Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class UACEventReceiver {

  @Autowired private RespondentEventPublisher publisher;
  @Autowired private RespondentDataServiceImpl cloud;
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

    log.info("Now store the event in Google Cloud..");
    storeUACEvent(event);
    log.info("The event has been stored and retrieved successfully");

    //    publisher.sendEvent(event);
  }

  public void storeUACEvent(UACEvent uacEvent) {

    String uacBucket = "uac_bucket";
    UAC uac = uacEvent.getPayload().getUac();

    String uacHash = uac.getUacHash();

    log.info("The hash code for the uac is: " + uacHash);

    String uacContent = uacEvent.toString();

    log.info("The value of uacContent is: " + uacContent);

    try {
      cloud.writeUAC(uac); // need to catch uk.gov.ons.ctp.common.error.CTPException
      Optional<UAC> uacOpt = cloud.readUAC(uacHash);
    } catch (CTPException ctpEx) {
      log.info("ERROR: " + ctpEx.getMessage());
    }

    log.info("The blob has been retrieved from the bucket");
  }
}
