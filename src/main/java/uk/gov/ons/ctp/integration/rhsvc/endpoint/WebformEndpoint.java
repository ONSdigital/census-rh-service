package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.micrometer.core.annotation.Timed;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;

/** The REST endpoint controller for the webform endpoint. */
@Timed
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class WebformEndpoint {

  private static final Logger log = LoggerFactory.getLogger(WebformEndpoint.class);

  @Autowired private WebformService webformService;

  @RequestMapping(value = "/webform", method = RequestMethod.POST)
  public void webformCapture(@Valid @RequestBody WebformDTO webform) throws CTPException {
    log.with("requestBody", webform).info("Entering POST webformCapture");
    UUID notificationId = webformService.sendWebformEmail(webform);
    log.with("notificationId", notificationId).info("Exit POST webformCapture");
  }
}
