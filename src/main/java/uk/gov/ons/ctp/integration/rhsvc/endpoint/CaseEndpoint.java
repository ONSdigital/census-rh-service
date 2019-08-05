package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
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

  /**
   * the PUT end point to notify of an address change
   *
   * @param caseId UUID for case.
   * @param addressChange AddressChangeDTO new address details for case, note needs to be complete
   *     address not just changed elements.
   * @return CaseDTO case details with changed address.
   * @throws CTPException something went wrong.
   */
  @RequestMapping(value = "/{caseId}/address", method = RequestMethod.PUT)
  public ResponseEntity<CaseDTO> modifyAddress(
      @PathVariable("caseId") final UUID caseId, @Valid @RequestBody AddressChangeDTO addressChange)
      throws CTPException {
    log.with("CaseId", caseId).info("Entering PUT modifyAddress");

    if (!caseId.equals(addressChange.getCaseId())) {
      String message = "The caseid in the URL does not match the caseid in the request body";
      log.with(caseId).warn(message);
      throw new CTPException(Fault.BAD_REQUEST, message);
    }

    CaseDTO result = caseService.modifyAddress(addressChange);
    return ResponseEntity.ok(result);
  }

  /**
   * the POST end point to request an SMS fulfilment for a case.
   *
   * @param caseId is the id for the case.
   * @param requestBodyDTO contains the request body, which specifies the case id, telephone number,
   *     etc.
   * @throws CTPException if the case is not found, or the product cannot be found, or if something
   *     else went wrong.
   */
  @RequestMapping(value = "/{caseId}/fulfilments/sms", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public void fulfilmentRequestBySMS(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody SMSFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {

    log.with("caseId", caseId)
        .with("request", requestBodyDTO.getFulfilmentCode())
        .info("Entering POST fulfilmentRequestBySMS");

    if (!caseId.equals(requestBodyDTO.getCaseId())) {
      String message = "The caseid in the URL does not match the caseid in the request body";
      log.with(caseId).warn(message);
      throw new CTPException(Fault.BAD_REQUEST, message);
    }

    caseService.fulfilmentRequestBySMS(requestBodyDTO);
  }
}
