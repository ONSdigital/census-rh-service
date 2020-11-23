package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import uk.gov.ons.ctp.common.event.model.WebformEvent;
import uk.gov.ons.ctp.integration.rhsvc.event.WebformEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;

/** Service implementation responsible for receipt of Webform Events. */
@MessageEndpoint
public class WebformEventReceiverImpl implements WebformEventReceiver {

  private static final Logger log = LoggerFactory.getLogger(WebformEventReceiverImpl.class);

  private WebformService webformService;

  /**
   * Constructor for WebformEventReceiverImpl
   *
   * @param webformService service responsible for webform request functionality
   */
  public WebformEventReceiverImpl(final WebformService webformService) {
    this.webformService = webformService;
  }

  @ServiceActivator(inputChannel = "acceptWebformEvent")
  public void acceptWebformEvent(WebformEvent event) {

    log.with("transactionId", event.getEvent().getTransactionId())
        .with("webform", event.getPayload().getWebform())
        .info("Entering acceptWebformEvent");
    webformService.sendWebformEmail(event.getPayload().getWebform());
  }
}
