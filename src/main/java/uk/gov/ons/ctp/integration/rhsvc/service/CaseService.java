package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.List;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

/** Service responsible for UAC requests */
public interface CaseService {

  /**
   * Retrieve the data relating to a Case
   *
   * @param uprn of a Case
   * @return List of Case details for a UPRN
   * @throws CTPException
   */
  List<CaseDTO> getHHCaseByUPRN(final UniquePropertyReferenceNumber uprn) throws CTPException;
}
