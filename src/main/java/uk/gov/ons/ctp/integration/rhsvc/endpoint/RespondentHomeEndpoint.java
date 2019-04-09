package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentHomeServiceImpl;

/** The REST endpoint controller for the Response Handler service */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class RespondentHomeEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(RespondentHomeEndpoint.class);

  private RespondentHomeServiceImpl respondentHomeService;

  @Autowired
  public RespondentHomeEndpoint(final RespondentHomeServiceImpl respondentHomeService) {
    this.respondentHomeService = respondentHomeService;
  }

  /**
   * This is a POST endpoint which will be invoked when a survey is launched.
   * It sends a survey launched event using RabbitMQ.
   */
  @RequestMapping(value = "/surveyLaunched", method = RequestMethod.POST)
  public void surveyLaunched(@Valid @RequestBody SurveyLaunchedDTO surveyLaunchedDTO) {
    String helloTeam = "Hello Census Integration!";

    respondentHomeService.surveyLaunched(surveyLaunchedDTO); 
  }
}
