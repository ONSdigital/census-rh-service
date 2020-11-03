package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.micrometer.core.annotation.Timed;
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
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** The REST controller to deal with Cases */
@Timed
@RestController
@RequestMapping(value = "/cases", produces = "application/json")
public class CaseEndpoint {
  private static final Logger log = LoggerFactory.getLogger(CaseEndpoint.class);

  @Autowired private CaseService caseService;

  /**
   * the POST end point to request the creation of a new case.
   *
   * @param requestBodyDTO contains the UPRN and address details for the new case.
   * @return a CaseDTO for the new case, or details of an existing case if it has the same UPRN as
   *     the request.
   * @throws CTPException if something goes wrong.
   */
  @RequestMapping(value = "/create", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<CaseDTO> createNewCase(@Valid @RequestBody CaseRequestDTO requestBodyDTO)
      throws CTPException {
    String methodName = "createNewCase";
    log.with("UPRN", requestBodyDTO.getUprn())
        .with("requestBody", requestBodyDTO)
        .info("Entering POST {}", methodName);

    CaseDTO caseToReturn = caseService.createNewCase(requestBodyDTO);

    log.with("UPRN", requestBodyDTO.getUprn()).debug("Exit POST {}", methodName);
    return ResponseEntity.ok(caseToReturn);
  }

  /**
   * the GET end point to return the latest valid Non HI Case by UPRN
   *
   * @param uprn the UPRN
   * @return Returned latest Non HI case with valid address for the UPRN
   * @throws CTPException something went wrong - thrown by case service
   */
  @RequestMapping(value = "/uprn/{uprn}", method = RequestMethod.GET)
  public ResponseEntity<CaseDTO> getCaseByUPRN(
      @PathVariable(value = "uprn") final UniquePropertyReferenceNumber uprn) throws CTPException {
    log.with("pathParam.uprn", uprn).info("Entering GET getLatestValidNonHICaseByUPRN");

    CaseDTO result = caseService.getLatestValidNonHICaseByUPRN(uprn);
    log.with("pathParam.uprn", uprn).debug("Exit GET getLatestValidNonHICaseByUPRN");
    return ResponseEntity.ok(result);
  }

  private void validateMatchingCaseId(UUID caseId, UUID dtoCaseId, String dtoName)
      throws CTPException {
    if (!caseId.equals(dtoCaseId)) {
      String message = "The path parameter caseId does not match the caseId in the request body";
      log.with("caseId", caseId).with("dtoName", dtoName).warn(message);
      throw new CTPException(Fault.BAD_REQUEST, message);
    }
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
    String methodName = "modifyAddress";
    log.with("pathParam.caseId", caseId)
        .with("requestBody", addressChange)
        .info("Entering PUT {}", methodName);

    validateMatchingCaseId(caseId, addressChange.getCaseId(), methodName);
    CaseDTO result = caseService.modifyAddress(addressChange);
    log.with("pathParam.caseId", caseId).debug("Exit {}", methodName);
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
    String methodName = "fulfilmentRequestBySMS";
    log.with("pathParam.caseId", caseId)
        .with("requestBody", requestBodyDTO)
        .info("Entering POST {}", methodName);

    validateMatchingCaseId(caseId, requestBodyDTO.getCaseId(), methodName);
    caseService.fulfilmentRequestBySMS(requestBodyDTO);
    log.with("pathParam.caseId", caseId).debug("Exit POST {}", methodName);
  }

  @RequestMapping(value = "/{caseId}/fulfilments/post", method = RequestMethod.POST)
  @ResponseStatus(value = HttpStatus.OK)
  public void fulfilmentRequestByPost(
      @PathVariable(value = "caseId") final UUID caseId,
      @Valid @RequestBody PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    String methodName = "fulfilmentRequestByPost";
    log.with("pathParam.caseId", caseId).info("Entering POST {}", methodName);

    validateMatchingCaseId(caseId, requestBodyDTO.getCaseId(), methodName);
    caseService.fulfilmentRequestByPost(requestBodyDTO);
    log.with("pathParam.caseId", caseId).debug("Exit POST {}", methodName);
  }
}
