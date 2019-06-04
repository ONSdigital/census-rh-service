package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
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

  @SuppressWarnings("unchecked")
  public static <T extends GenericEvent, S> T buildEvent(EventType type, S payload)
      throws CTPException {

    Header header = buildHeader(type);

    switch (type) {
      case SURVEY_LAUNCHED:
        if (payload instanceof SurveyLaunchedResponse) {
          SurveyLaunchedEvent event = new SurveyLaunchedEvent();
          event.setEvent(header);
          event.getPayload().setResponse((SurveyLaunchedResponse) payload);
          return (T) event;
        } else {
          throw new CTPException(
              CTPException.Fault.SYSTEM_ERROR,
              type.toString() + " payload not instance of " + SurveyLaunchedResponse.class);
        }
      case RESPONDENT_AUTHENTICATED:
        if (payload instanceof RespondentAuthenticatedResponse) {
          RespondentAuthenticatedEvent event = new RespondentAuthenticatedEvent();
          event.setEvent(header);
          event.getPayload().setResponse((RespondentAuthenticatedResponse) payload);
          return (T) event;
        } else {
          throw new CTPException(
              CTPException.Fault.SYSTEM_ERROR,
              type.toString()
                  + " payload not instance of "
                  + RespondentAuthenticatedResponse.class);
        }
      default:
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, type.toString() + " not supported");
    }
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
