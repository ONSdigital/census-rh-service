package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** The REST endpoint controller for UAC requests. */
@RestController
@RequestMapping(value = "/uacs", produces = "application/json")
public class UniqueAccessCodeEndpoint {

  private static final Logger log = LoggerFactory.getLogger(UniqueAccessCodeEndpoint.class);

  @Autowired private UniqueAccessCodeService uacService;

  /**
   * the GET end-point to get RH details for a claim
   *
   * @param uacHash the hashed UAC
   * @return the claim details
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/{uacHash}", method = RequestMethod.GET)
  public ResponseEntity<UniqueAccessCodeDTO> getUACClaimContext(
      @PathVariable("uacHash") final String uacHash) throws CTPException {

    log.info("Entering GET getUACClaimContext");
    UniqueAccessCodeDTO uacDTO = uacService.getAndAuthenticateUAC(uacHash);

    return ResponseEntity.ok(uacDTO);
  }
}
