package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.integration.rhsvc.endpoint.SurveyLaunchedDTO;

public interface RespondentHomeService {

  void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO);
}
