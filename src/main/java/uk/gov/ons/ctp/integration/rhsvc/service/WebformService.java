package uk.gov.ons.ctp.integration.rhsvc.service;

import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.Webform;

/** Service responsible for Webform requests */
@Validated
public interface WebformService {

  /**
   * Send a Webform event. If the rate limit is breached then this method throws
   * a ResponseStatusException with a 429 status.
   *
   * @param webform request information
   * @return the transaction ID of the sent event.
   * @throws CTPException if the webform data fails the Rate limiter client validation.
   */
  String sendWebformEvent(Webform webform) throws CTPException;

  /**
   * Send a Webform email request
   *
   * @param webform request information
   */
  void sendWebformEmail(@Valid Webform webform);
}
