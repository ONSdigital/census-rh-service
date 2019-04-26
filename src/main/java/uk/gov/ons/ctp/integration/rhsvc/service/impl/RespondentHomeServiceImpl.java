package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.message.SurveyLaunchedEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Header;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentHomeService;

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

    Header eventData =
        Header.builder()
            .type("SURVEY_LAUNCHED")
            .source("CONTACT_CENTRE_API")
            .channel("CC")
            .dateTime(DateTimeUtil.getCurrentDateTimeInJsonFormat())
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
