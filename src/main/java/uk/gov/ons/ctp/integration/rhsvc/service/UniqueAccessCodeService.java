package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;

/** Service responsible for UAC requests */
public interface UniqueAccessCodeService {

  /**
   * Retrieve the data for a UAC
   *
   * @param uac unique access code for which to retrieve data
   * @return UniqueAccessCodeDTO representing data for UAC
   * @throws CTPException something wernt wrong
   */
  UniqueAccessCodeDTO getAndAuthenticateUAC(String uac) throws CTPException;
}
