package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACLinkRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;

/** Service responsible for UAC requests */
public interface UniqueAccessCodeService {

  /**
   * Retrieve the data for a hashed UAC
   *
   * @param uacHash hashed unique access code for which to retrieve data
   * @return UniqueAccessCodeDTO representing data for UAC
   * @throws CTPException something went wrong
   */
  UniqueAccessCodeDTO getAndAuthenticateUAC(String uacHash) throws CTPException;

  /**
   * Link a UAC to a case and return a representation of the UAC.
   *
   * @param uacHash hashed unique access code for which to retrieve data.
   * @param request contains the address detail to link to.
   * @return UniqueAccessCodeDTO representing data for UAC.
   * @throws CTPException something went wrong.
   */
  UniqueAccessCodeDTO linkUACCase(String uacHash, UACLinkRequestDTO request) throws CTPException;
}
