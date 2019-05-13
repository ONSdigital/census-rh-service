package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.message.Header;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.message.SurveyLaunchedEvent;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentHomeService;
import uk.gov.ons.ctp.integration.rhsvc.utility.Constants;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Service
public class RespondentHomeServiceImpl implements RespondentHomeService {
  private static final Logger log = LoggerFactory.getLogger(RespondentHomeServiceImpl.class);

  @Autowired private RespondentEventPublisher publisher;

  @Override
  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) {
    UUID transactionId = UUID.randomUUID();
    log.debug(
        "Generating SurveyLaunched event for questionnaireId: "
            + surveyLaunchedDTO.getQuestionnaireId()
            + ", caseId: "
            + surveyLaunchedDTO.getCaseId()
            + ". Using transactionId: "
            + transactionId);

    // Build key parts of Survey Launched event
    Header eventData =
        Header.builder()
            .type(Constants.MESSAGE_NAME_SURVEY_LAUNCHED)
            .source(Constants.RH_SERVICE_NAME)
            .channel(Constants.RH_CHANNEL_NAME)
            .dateTime(new Date())
            .transactionId(transactionId.toString())
            .build();

    SurveyLaunchedResponse response =
        SurveyLaunchedResponse.builder()
            .questionnaireId(surveyLaunchedDTO.getQuestionnaireId())
            .caseId(surveyLaunchedDTO.getCaseId())
            .agentId(null)
            .build();

    // Concatenate parts to create Survey Launched event
    SurveyLaunchedEvent surveyLaunchedEvent = new SurveyLaunchedEvent();
    surveyLaunchedEvent.setEvent(eventData);
    surveyLaunchedEvent.getPayload().setResponse(response);

    // Publish to Rabbit exchange
    publisher.sendEvent(surveyLaunchedEvent);

    log.debug("SurveyLaunch event published for transactionId: " + transactionId);
  }
}
