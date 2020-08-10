package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.Optional;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;

/**
 * This class contains business level logic for handling case related functionality for the case
 * endpoint.
 */
public interface CaseService {

  /**
   * Retrieve the data relating to non-HI latest valid Case by address UPRN
   *
   * @param uprn of address for which latest valid non-HI case details are requested
   * @return Case details for address UPRN
   * @throws CTPException if anything went wrong.
   */
  CaseDTO getLatestValidNonHICaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException;

  /**
   * Attempts to read the latest case from cloud storage by UPRN.
   *
   * @param uprn is the uprn we need to search by.
   * @return Optional CaseDTO with any found case.
   * @throws CTPException if anything went wrong.
   */
  Optional<CaseDTO> getLatestCaseByUPRN(UniquePropertyReferenceNumber uprn) throws CTPException;

  /**
   * Change address for a Case
   *
   * @param address for change of address
   * @return Case details for changed address
   * @throws CTPException if anything went wrong.
   */
  CaseDTO modifyAddress(final AddressChangeDTO address) throws CTPException;

  void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException;

  void fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO) throws CTPException;
}
