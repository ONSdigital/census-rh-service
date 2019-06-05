package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;

public class EventBuilder {

  @Getter
  public enum EventType {
    SURVEY_LAUNCHED("RESPONDENT_HOME", "RH"),
    RESPONDENT_AUTHENTICATED("RESPONDENT_HOME", "RH");

    private String source;
    private String channel;

    EventType(String source, String channel) {
      this.source = source;
      this.channel = channel;
    }
  }

  public static SurveyLaunchedEvent buildEvent(SurveyLaunchedResponse payload) {

    Header header = buildHeader(EventType.SURVEY_LAUNCHED);
    SurveyLaunchedEvent event = new SurveyLaunchedEvent();
    event.setEvent(header);
    event.getPayload().setResponse((SurveyLaunchedResponse) payload);
    return event;
  }

  public static RespondentAuthenticatedEvent buildEvent(RespondentAuthenticatedResponse payload) {

    Header header = buildHeader(EventType.RESPONDENT_AUTHENTICATED);
    RespondentAuthenticatedEvent event = new RespondentAuthenticatedEvent();
    event.setEvent(header);
    event.getPayload().setResponse((RespondentAuthenticatedResponse) payload);
    return event;
  }

  private static Header buildHeader(EventType type) {
    return Header.builder()
        .type(type.toString())
        .source(type.getSource())
        .channel(type.getChannel())
        .dateTime(new Date())
        .transactionId(UUID.randomUUID().toString())
        .build();
  }
}
