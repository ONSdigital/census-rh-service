package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;

/**
 * This class contains business level logic for handling case related functionality for the case
 * endpoint.
 */
public interface CaseService {

  public void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException;
}
