package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentHomeService;

/**
 * The REST endpoint controller for the Respondent Home service. This class covers top level
 * endpoints.
 */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class RespondentHomeEndpoint {

  private static final Logger log = LoggerFactory.getLogger(RespondentHomeEndpoint.class);
  private RespondentHomeService respondentHomeService;

  @Autowired
  public RespondentHomeEndpoint(final RespondentHomeService respondentHomeService) {
    this.respondentHomeService = respondentHomeService;
  }

  @RequestMapping(value = "/surveyLaunched", method = RequestMethod.POST)
  public void surveyLaunched(@Valid @RequestBody SurveyLaunchedDTO surveyLaunchedDTO)
      throws CTPException {

    log.with("requestBody", surveyLaunchedDTO).info("Entering POST surveyLaunched");
    respondentHomeService.surveyLaunched(surveyLaunchedDTO);
  }
}
