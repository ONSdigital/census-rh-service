package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.UUID;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;

/** Service responsible for Webform requests */
@Validated
public interface WebformService {

  /**
   * Send a Webform email request
   *
   * @param webform request information
   * @return notification Id
   */
  UUID sendWebformEmail(@Valid WebformDTO webform);
}
