package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;

@RunWith(MockitoJUnitRunner.class)
public class SurveyLaunchedServiceImplTest {

  @Mock EventPublisher publisher;

  @InjectMocks SurveyLaunchedServiceImpl surveyLaunchedService;

  @Captor ArgumentCaptor<SurveyLaunchedResponse> sendEventCaptor;

  @Test
  public void testSurveyLaunchedAddressAgentIdValue() throws Exception {
    // Give a survey launched request to the service layer
    SurveyLaunchedDTO surveyLaunchedDTO = new SurveyLaunchedDTO();
    surveyLaunchedDTO.setQuestionnaireId("1234");
    surveyLaunchedDTO.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    surveyLaunchedDTO.setAgentId("1000007");
    surveyLaunchedService.surveyLaunched(surveyLaunchedDTO);

    // Get hold of the event pay load that surveyLaunchedService created
    Mockito.verify(publisher)
        .sendEventWithPersistance(
            eq(EventType.SURVEY_LAUNCHED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.AD),
            sendEventCaptor.capture());
    SurveyLaunchedResponse eventPayload = sendEventCaptor.getValue();

    // Verify contents of pay load object
    assertEquals(surveyLaunchedDTO.getQuestionnaireId(), eventPayload.getQuestionnaireId());
    assertEquals(surveyLaunchedDTO.getCaseId(), eventPayload.getCaseId());
    assertEquals(surveyLaunchedDTO.getAgentId(), eventPayload.getAgentId());
  }

  @Test
  public void testSurveyLaunchedAddressAgentIdEmptyString() throws Exception {
    // Give a survey launched request to the service layer
    SurveyLaunchedDTO surveyLaunchedDTO = new SurveyLaunchedDTO();
    surveyLaunchedDTO.setQuestionnaireId("1234");
    surveyLaunchedDTO.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    surveyLaunchedDTO.setAgentId("");
    surveyLaunchedService.surveyLaunched(surveyLaunchedDTO);

    // Get hold of the event pay load that surveyLaunchedService created
    Mockito.verify(publisher)
        .sendEventWithPersistance(
            eq(EventType.SURVEY_LAUNCHED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            sendEventCaptor.capture());
    SurveyLaunchedResponse eventPayload = sendEventCaptor.getValue();

    // Verify contents of pay load object
    assertEquals(surveyLaunchedDTO.getQuestionnaireId(), eventPayload.getQuestionnaireId());
    assertEquals(surveyLaunchedDTO.getCaseId(), eventPayload.getCaseId());
    assertEquals(surveyLaunchedDTO.getAgentId(), eventPayload.getAgentId());
  }

  @Test
  public void testSurveyLaunchedAddressAgentIdNull() throws Exception {
    // Give a survey launched request to the service layer
    SurveyLaunchedDTO surveyLaunchedDTO = new SurveyLaunchedDTO();
    surveyLaunchedDTO.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    surveyLaunchedService.surveyLaunched(surveyLaunchedDTO);

    // Get hold of the event pay load that surveyLaunchedService created
    Mockito.verify(publisher)
        .sendEventWithPersistance(
            eq(EventType.SURVEY_LAUNCHED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            sendEventCaptor.capture());
    SurveyLaunchedResponse eventPayload = sendEventCaptor.getValue();

    // Verify contents of pay load object
    assertEquals(surveyLaunchedDTO.getQuestionnaireId(), eventPayload.getQuestionnaireId());
    assertEquals(surveyLaunchedDTO.getCaseId(), eventPayload.getCaseId());
    assertEquals(surveyLaunchedDTO.getAgentId(), eventPayload.getAgentId());
  }
}
