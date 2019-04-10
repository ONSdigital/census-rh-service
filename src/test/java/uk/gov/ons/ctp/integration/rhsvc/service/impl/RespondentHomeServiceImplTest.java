package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import java.util.Map;
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
import uk.gov.ons.ctp.integration.rhsvc.endpoint.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.GenericCaseEvent;

public class RespondentHomeServiceImplTest {

  @Mock RespondentEventPublisher publisher;

  @InjectMocks
  RespondentHomeServiceImpl respondentHomeService;

  @Captor ArgumentCaptor<GenericCaseEvent> sendEventCaptor;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testAddressQueryProcessing() throws Exception {
    // Ask the service layer to send the surveyLaunched event
    String questionnaireId = "1234";
    String caseId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    SurveyLaunchedDTO surveyLaunchedDTO = new SurveyLaunchedDTO();
    surveyLaunchedDTO.setQuestionnaireId(questionnaireId);
    surveyLaunchedDTO.setCaseId(UUID.fromString(caseId));
    respondentHomeService.surveyLaunched(surveyLaunchedDTO);

    // Verify the contents of the generated survey launched event
    Mockito.verify(publisher).sendEvent(sendEventCaptor.capture());
    GenericCaseEvent genericCaseEvent = sendEventCaptor.getValue();
    assertEquals("SurveyLaunched", genericCaseEvent.getEvent().getType());
    assertEquals("ContactCentreAPI", genericCaseEvent.getEvent().getSource());
    assertEquals("cc", genericCaseEvent.getEvent().getChannel());
    TestHelper.validateAsDateTime(genericCaseEvent.getEvent().getDateTime());
    assertEquals("123", genericCaseEvent.getEvent().getTransactionId());
    Map<String, String> responseProperties = genericCaseEvent.getPayload().getResponse().getProperties();
    assertEquals(questionnaireId, responseProperties.get("questionnaireId"));
    assertEquals(caseId, responseProperties.get("caseId"));
    assertEquals(null, responseProperties.get("agentId"));
    assertEquals(3, responseProperties.size());
  }
}
