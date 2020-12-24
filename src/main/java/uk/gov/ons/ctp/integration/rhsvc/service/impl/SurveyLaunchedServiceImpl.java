package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyLaunchedService;

/** This is a service layer class, which performs RH business level logic for the endpoints. */
@Service
public class SurveyLaunchedServiceImpl implements SurveyLaunchedService {
  private static final Logger log = LoggerFactory.getLogger(SurveyLaunchedServiceImpl.class);

  private EventPublisher eventPublisher;
  private RateLimiterClient rateLimiterClient;
  private AppConfig appConfig;

  @Autowired
  public SurveyLaunchedServiceImpl(
      EventPublisher eventPublisher, RateLimiterClient rateLimiterClient, AppConfig appConfig) {
    this.eventPublisher = eventPublisher;
    this.rateLimiterClient = rateLimiterClient;
    this.appConfig = appConfig;
  }

  @Override
  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) throws CTPException {
    log.with("surveyLaunchedDTO", surveyLaunchedDTO).info("Generating SurveyLaunched event");

    checkRateLimit(surveyLaunchedDTO.getClientIP());

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

    log.with("caseId", response.getCaseId())
        .with("transactionId", transactionId)
        .debug("SurveyLaunch event published");
  }

  private void checkRateLimit(String ipAddress) throws CTPException {
    if (appConfig.getRateLimiter().isEnabled()) {
      int modulus = appConfig.getLoadshedding().getModulus();
      log.with("ipAddress", ipAddress)
          .with("loadshedding.modulus", modulus)
          .debug("Invoking rate limiter for survey launched");
      rateLimiterClient.checkEqLaunchLimit(Domain.RH, ipAddress, modulus);
    } else {
      log.info("Rate limiter client is disabled");
    }
  }
}
