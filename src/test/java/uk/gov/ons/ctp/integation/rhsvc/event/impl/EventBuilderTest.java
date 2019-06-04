package uk.gov.ons.ctp.integation.rhsvc.event.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedEvent;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.EventBuilder;

public class EventBuilderTest {

  private static final UUID CASE_ID = UUID.fromString("dc4477d1-dd3f-4c69-b181-7ff725dc9fa4");
  private static final String QUESTIONNAIRE_ID = "1110000009";
  private static final String SUREVEY_LAUNCHED_ERROR =
      "SURVEY_LAUNCHED payload not instance of class uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse";
  private static final String RESPONDENT_AUTHENTICATED_ERROR =
      "RESPONDENT_AUTHENTICATED payload not instance of class uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse";

  private SurveyLaunchedResponse surveyLaunchedResponse;
  private RespondentAuthenticatedResponse respondentAuthenticatedResponse;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {

    surveyLaunchedResponse =
        SurveyLaunchedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .agentId(null)
            .build();

    respondentAuthenticatedResponse =
        RespondentAuthenticatedResponse.builder()
            .questionnaireId(QUESTIONNAIRE_ID)
            .caseId(CASE_ID)
            .build();
  }

  /** Test build of Survey Launched event message with correct pay load */
  @Test
  public void buildSurveyLaunchedCorrectPayload() throws Exception {

    SurveyLaunchedEvent event =
        EventBuilder.buildEvent(EventBuilder.EventType.SURVEY_LAUNCHED, surveyLaunchedResponse);

    assertEquals(
        EventBuilder.EventType.SURVEY_LAUNCHED.getChannel(), event.getEvent().getChannel());
    assertThat(event.getEvent().getDateTime(), instanceOf(Date.class));
    assertEquals(EventBuilder.EventType.SURVEY_LAUNCHED.getSource(), event.getEvent().getSource());
    assertThat(UUID.fromString(event.getEvent().getTransactionId()), instanceOf(UUID.class));
    assertEquals(EventBuilder.EventType.SURVEY_LAUNCHED.toString(), event.getEvent().getType());
    assertEquals(CASE_ID, event.getPayload().getResponse().getCaseId());
    assertEquals(QUESTIONNAIRE_ID, event.getPayload().getResponse().getQuestionnaireId());
  }

  /** Test build of Survey Launched event message with wrong pay load */
  @Test
  public void buildSurveyLaunchedWrongPayload() {

    boolean exceptionThrown = false;

    try {
      EventBuilder.buildEvent(
          EventBuilder.EventType.SURVEY_LAUNCHED, respondentAuthenticatedResponse);
    } catch (CTPException e) {
      exceptionThrown = true;
      assertEquals(SUREVEY_LAUNCHED_ERROR, e.getMessage());
    }

    assertTrue(exceptionThrown);
  }

  /** Test build of Respondent Authenticated event message with correct pay load */
  @Test
  public void buildRespondentAuthenticatedCorrectPayload() throws Exception {

    RespondentAuthenticatedEvent event =
        EventBuilder.buildEvent(
            EventBuilder.EventType.RESPONDENT_AUTHENTICATED, respondentAuthenticatedResponse);

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

  /** Test build of Respondent Authenticated event message with wrong pay load */
  @Test
  public void buildRespondentAuthenticatedWrongPayload() {

    boolean exceptionThrown = false;

    try {
      EventBuilder.buildEvent(
          EventBuilder.EventType.RESPONDENT_AUTHENTICATED, surveyLaunchedResponse);
    } catch (CTPException e) {
      exceptionThrown = true;
      assertEquals(RESPONDENT_AUTHENTICATED_ERROR, e.getMessage());
    }

    assertTrue(exceptionThrown);
  }
}
