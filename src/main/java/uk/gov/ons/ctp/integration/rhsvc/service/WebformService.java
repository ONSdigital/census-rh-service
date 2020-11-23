package uk.gov.ons.ctp.integration.rhsvc.service;

import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.event.model.Webform;

/** Service responsible for Webform requests */
@Validated
public interface WebformService {

  /**
   * Send a Webform event
   *
   * @param webform request information
   * @return the transaction ID of the sent event.
   */
  String sendWebformEvent(Webform webform);

  /**
   * Send a Webform email request
   *
   * @param webform request information
   */
  void sendWebformEmail(@Valid Webform webform);
}
