package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;

/** Service responsible for SurveyLaunched requests */
public interface SurveyLaunchedService {

  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) throws CTPException;
}
