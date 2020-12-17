package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.UUID;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;

/** Service responsible for Webform requests */
@Validated
public interface WebformService {

  /**
   * Send a Webform email request. If the rate limit is breached then this method throws a
   * ResponseStatusException with a 429 status.
   *
   * @param webform request information
   * @return notification Id
   * @throws CTPException 
   */
  UUID sendWebformEmail(@Valid WebformDTO webform) throws CTPException;
}
