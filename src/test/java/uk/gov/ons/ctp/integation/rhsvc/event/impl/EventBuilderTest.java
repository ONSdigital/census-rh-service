package uk.gov.ons.ctp.integation.rhsvc.event.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Date;
import java.util.UUID;
import org.junit.Test;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.EventBuilder;

public class EventBuilderTest {

  private static final UUID CASE_ID = UUID.fromString("dc4477d1-dd3f-4c69-b181-7ff725dc9fa4");
  private static final String QUESTIONNAIRE_ID = "1110000009";

  /** Test build of Survey Launched event message */
  @Test
  public void buildSurveyLaunchedEvent() throws Exception {

    SurveyLaunchedResponse surveyLaunchedResponse =
        SurveyLaunchedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .agentId(null)
            .build();

    SurveyLaunchedEvent event = EventBuilder.buildEvent(surveyLaunchedResponse);

    assertEquals(
        EventBuilder.EventType.SURVEY_LAUNCHED.getChannel(), event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(EventBuilder.EventType.SURVEY_LAUNCHED.getSource(), event.getEvent().getSource());
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventBuilder.EventType.SURVEY_LAUNCHED.toString(), event.getEvent().getType());
    assertEquals(CASE_ID, event.getPayload().getResponse().getCaseId());
    assertEquals(QUESTIONNAIRE_ID, event.getPayload().getResponse().getQuestionnaireId());
  }

  /** Test build of Respondent Authenticated event message */
  @Test
  public void buildRespondentAuthenticatedEvent() {

    RespondentAuthenticatedResponse respondentAuthenticatedResponse =
        RespondentAuthenticatedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .build();

    RespondentAuthenticatedEvent event = EventBuilder.buildEvent(respondentAuthenticatedResponse);

    assertEquals(
        EventBuilder.EventType.RESPONDENT_AUTHENTICATED.getChannel(),
        event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(
        EventBuilder.EventType.RESPONDENT_AUTHENTICATED.getSource(), event.getEvent().getSource());
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(
        EventBuilder.EventType.RESPONDENT_AUTHENTICATED.toString(), event.getEvent().getType());
    assertEquals(CASE_ID, event.getPayload().getResponse().getCaseId());
    assertEquals(QUESTIONNAIRE_ID, event.getPayload().getResponse().getQuestionnaireId());
  }
}
