package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.event.model.Webform;

/** Service responsible for Webform requests */
public interface WebformService {

  /**
   * Send a Webform event
   *
   * @param webform request information
   * @return the transaction ID of the sent event.
   */
  String sendWebformEvent(Webform webform);
}
