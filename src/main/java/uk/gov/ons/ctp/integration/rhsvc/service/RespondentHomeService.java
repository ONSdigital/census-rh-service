package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;

public interface RespondentHomeService {

  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) throws CTPException;
}
