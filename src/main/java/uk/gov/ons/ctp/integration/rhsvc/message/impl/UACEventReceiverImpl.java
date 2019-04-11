package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.message.UACEvent;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

/**
 * Service implementation responsible for receipt of UAC Events. See Spring Integration flow for
 * details of in bound queue.
 */
@MessageEndpoint
public class UACEventReceiverImpl {

  @Autowired private RespondentDataServiceImpl respondentDataServiceImpl;
  private static final Logger log = LoggerFactory.getLogger(UACEventReceiverImpl.class);

  /**
   * Message end point for events from Response Management. At present sends straight to publisher
   * to prove messaging setup.
   *
   * @param uacEvent UACEvent message from Response Management
   */
  @ServiceActivator(inputChannel = "acceptUACEvent")
  public void acceptUACEvent(UACEvent uacEvent) {

    UAC uac;

    log.info("The event being received is: " + uacEvent.toString());

    uac = uacEvent.getPayload().getUac();

    try {
      respondentDataServiceImpl.writeUAC(
          uac); // need to catch uk.gov.ons.ctp.common.error.CTPException
    } catch (CTPException ctpEx) {
      log.with(ctpEx.getMessage()).info("ERROR");
    }
  }
}
