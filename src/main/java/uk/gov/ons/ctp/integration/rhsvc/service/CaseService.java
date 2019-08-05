package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.List;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

/**
 * This class contains business level logic for handling case related functionality for the case
 * endpoint.
 */
public interface CaseService {

  /**
   * Retrieve the data relating to HH Cases by address UPRN
   *
   * @param uprn of address for which HH case details are requested
   * @return List of HH Case details for address UPRN
   * @throws CTPException
   */
  List<CaseDTO> getHHCaseByUPRN(final UniquePropertyReferenceNumber uprn) throws CTPException;

  /**
   * Change address for a Case
   *
   * @param AddressChangeDTO for change of address
   * @return Case details for changed address
   * @throws CTPException
   */
  CaseDTO modifyAddress(final AddressChangeDTO address) throws CTPException;

  void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException;
}
