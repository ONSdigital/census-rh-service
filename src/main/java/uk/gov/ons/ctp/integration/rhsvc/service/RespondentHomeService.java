package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedDTO;

public interface RespondentHomeService {

  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO);
}
