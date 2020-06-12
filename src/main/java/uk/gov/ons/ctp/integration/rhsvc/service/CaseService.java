package uk.gov.ons.ctp.integration.rhsvc.service;

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
   * @return Case details for address UPRN
   * @throws CTPException if anything went wrong.
   */
  CaseDTO getLatestValidNonHICaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException;

  /**
   * Change address for a Case
   *
   * @param address for change of address
   * @return Case details for changed address
   * @throws CTPException if anything went wrong.
   */
  CaseDTO modifyAddress(final AddressChangeDTO address) throws CTPException;

  void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException;
}
