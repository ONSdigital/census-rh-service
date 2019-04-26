package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.common.TestHelper;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.message.SurveyLaunchedEvent;

public class RespondentHomeServiceImplTest {

  @Mock RespondentEventPublisher publisher;

  @InjectMocks RespondentHomeServiceImpl respondentHomeService;

  @Captor ArgumentCaptor<SurveyLaunchedEvent> sendEventCaptor;

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

    // Get hold of the event that respondentHomeService created
    Mockito.verify(publisher).sendEvent(sendEventCaptor.capture());
    SurveyLaunchedEvent surveyLaunchedEvent = sendEventCaptor.getValue();

    // Verify the contents of the top level event data
    assertEquals("SURVEY_LAUNCHED", surveyLaunchedEvent.getEvent().getType());
    assertEquals("CONTACT_CENTRE_API", surveyLaunchedEvent.getEvent().getSource());
    assertEquals("CC", surveyLaunchedEvent.getEvent().getChannel());
    TestHelper.validateAsDateTime(surveyLaunchedEvent.getEvent().getDateTime());
    TestHelper.validateAsUUID(surveyLaunchedEvent.getEvent().getTransactionId());

    // Verify contents of payload object
    SurveyLaunchedResponse response = surveyLaunchedEvent.getPayload().getResponse();
    assertEquals(surveyLaunchedDTO.getQuestionnaireId(), response.getQuestionnaireId());
    assertEquals(surveyLaunchedDTO.getCaseId(), response.getCaseId());
    assertNull(response.getAgentId());
  }
}
