package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyLaunchedService;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Service
public class SurveyLaunchedServiceImpl implements SurveyLaunchedService {
  private static final Logger log = LoggerFactory.getLogger(SurveyLaunchedServiceImpl.class);

  @Autowired private EventPublisher eventPublisher;

  @Value("${queueconfig.response-authentication-routing-key}")
  private String routingKey;

  @Override
  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) throws CTPException {

    log.debug(
        "Generating SurveyLaunched event for caseId: "
            + surveyLaunchedDTO.getCaseId()
            + ", questionnaireId: "
            + surveyLaunchedDTO.getQuestionnaireId());

    SurveyLaunchedResponse response =
        SurveyLaunchedResponse.builder()
            .questionnaireId(surveyLaunchedDTO.getQuestionnaireId())
            .caseId(surveyLaunchedDTO.getCaseId())
            .agentId(surveyLaunchedDTO.getAgentId())
            .build();

    Channel channel = Channel.RH;
    if (!StringUtils.isEmpty(surveyLaunchedDTO.getAgentId())) {
      channel = Channel.AD;
    }

    String transactionId =
        eventPublisher.sendEvent(
            EventType.SURVEY_LAUNCHED, Source.RESPONDENT_HOME, channel, response);

    log.debug(
        "SurveyLaunch event published for caseId: "
            + response.getCaseId()
            + ", transactionId: "
            + transactionId);
  }
}
