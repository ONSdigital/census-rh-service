package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** The REST controller to deal with Cases */
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public class CaseEndpoint {

  private static final Logger log = LoggerFactory.getLogger(CaseEndpoint.class);

  @Autowired private CaseService caseService;

  /**
   * the GET end point to get a List of HH Cases by UPRN
   *
   * @param uprn the UPRN
   * @return List of returned HH cases for the UPRN
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/uprn/{uprn}", method = RequestMethod.GET)
  public ResponseEntity<List<CaseDTO>> getHHCaseByUPRN(
      @PathVariable(value = "uprn") final UniquePropertyReferenceNumber uprn) throws CTPException {
    log.with("uprn", uprn).info("Entering GET getHHCaseByUPRN");

    List<CaseDTO> results = caseService.getHHCaseByUPRN(uprn);

    if (results.isEmpty()) {
      throw new CTPException(
          CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve UPRN: " + uprn.getValue());
    }

    return ResponseEntity.ok(results);
  }
}
