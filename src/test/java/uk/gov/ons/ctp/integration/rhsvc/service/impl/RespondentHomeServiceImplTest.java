package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;

public class RespondentHomeServiceImplTest {

  @Mock EventPublisher publisher;

  @InjectMocks RespondentHomeServiceImpl respondentHomeService;

  @Captor ArgumentCaptor<SurveyLaunchedResponse> sendEventCaptor;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testSurveyLaunchedAddressQueryProcessing() throws Exception {
    // Give a survey launched request to the service layer
    SurveyLaunchedDTO surveyLaunchedDTO = new SurveyLaunchedDTO();
    surveyLaunchedDTO.setQuestionnaireId("1234");
    surveyLaunchedDTO.setCaseId(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));
    respondentHomeService.surveyLaunched(surveyLaunchedDTO);

    // Get hold of the event pay load that respondentHomeService created
    Mockito.verify(publisher)
        .sendEvent(
            eq(EventType.SURVEY_LAUNCHED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            sendEventCaptor.capture());
    SurveyLaunchedResponse eventPayload = sendEventCaptor.getValue();

    // Verify contents of pay load object
    assertEquals(surveyLaunchedDTO.getQuestionnaireId(), eventPayload.getQuestionnaireId());
    assertEquals(surveyLaunchedDTO.getCaseId(), eventPayload.getCaseId());
    assertNull(eventPayload.getAgentId());
  }
}
