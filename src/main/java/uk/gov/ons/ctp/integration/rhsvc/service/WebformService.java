package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.UUID;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;

/** Service responsible for Webform requests */
public interface WebformService {

  public UUID webformCapture(WebformDTO webformDTO) throws CTPException;
}
