package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.event.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.EventBuilder;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentHomeService;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Service
public class RespondentHomeServiceImpl implements RespondentHomeService {
  private static final Logger log = LoggerFactory.getLogger(RespondentHomeServiceImpl.class);

  @Autowired private RespondentEventPublisher publisher;

  @Override
  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) {

    log.debug(
        "Generating SurveyLaunched event for caseId: "
            + surveyLaunchedDTO.getCaseId()
            + ", questionnaireId: "
            + surveyLaunchedDTO.getQuestionnaireId());

    SurveyLaunchedResponse response =
        SurveyLaunchedResponse.builder()
            .questionnaireId(surveyLaunchedDTO.getQuestionnaireId())
            .caseId(surveyLaunchedDTO.getCaseId())
            .agentId(null)
            .build();

    SurveyLaunchedEvent event = EventBuilder.buildEvent(response);

    // Publish to Rabbit exchange
    publisher.sendEvent(event);

    log.debug(
        "SurveyLaunch event published for caseId: "
            + event.getPayload().getResponse().getCaseId()
            + ", transactionId: "
            + event.getEvent().getTransactionId());
  }
}
