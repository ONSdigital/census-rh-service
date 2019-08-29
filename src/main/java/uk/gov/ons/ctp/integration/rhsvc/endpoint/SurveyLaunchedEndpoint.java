package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyLaunchedService;

/**
 * The REST endpoint controller for the Respondent Home service. This class covers top level
 * endpoints.
 */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class SurveyLaunchedEndpoint {

  @Autowired private SurveyLaunchedService surveyLaunchedService;

  @RequestMapping(value = "/surveyLaunched", method = RequestMethod.POST)
  public void surveyLaunched(@Valid @RequestBody SurveyLaunchedDTO surveyLaunchedDTO)
      throws CTPException {

    surveyLaunchedService.surveyLaunched(surveyLaunchedDTO);
  }
}
