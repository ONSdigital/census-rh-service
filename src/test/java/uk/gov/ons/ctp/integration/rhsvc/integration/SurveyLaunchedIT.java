package uk.gov.ons.ctp.integration.rhsvc.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.TestHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.endpoint.RespondentHomeEndpoint;
import uk.gov.ons.ctp.integration.rhsvc.message.SurveyLaunchedEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Header;

/**
 * This is a component test which submits a Post saying that a survey has been launched and uses a
 * mock to confirm that RH publishes a survey launched event.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SurveyLaunchedIT {
  @Autowired private RespondentHomeEndpoint respondentHomeEndpoint;

  @MockBean(name = "surveyLaunchedRabbitTemplate")
  private RabbitTemplate rabbitTemplate;

  private MockMvc mockMvc;

  @Captor private ArgumentCaptor<SurveyLaunchedEvent> publishCaptor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(respondentHomeEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .build();
  }

  /**
   * This test Posts a survey launched event and uses a mock rabbit template to confirm that a
   * survey launched event is published.
   */
  @Test
  public void surveyLaunched_success() throws Exception {
    // Read request body from resource file
    ObjectNode surveyLaunchedRequestBody = FixtureHelper.loadClassObjectNode();
    String questionnaireId = surveyLaunchedRequestBody.get("questionnaireId").asText();
    String caseId = surveyLaunchedRequestBody.get("caseId").asText();

    // Send a Post request to the /surveyLaunched endpoint
    mockMvc
        .perform(postJson("/surveyLaunched", surveyLaunchedRequestBody.toString()))
        .andExpect(status().isOk());

    // Get ready to capture the survey details published to the exchange
    Mockito.verify(rabbitTemplate).convertAndSend(publishCaptor.capture());
    SurveyLaunchedEvent publishedEvent = publishCaptor.getValue();

    // Validate contents of the published event
    Header event = publishedEvent.getEvent();
    assertEquals("SURVEY_LAUNCHED", event.getType());
    assertEquals("CONTACT_CENTRE_API", event.getSource());
    assertEquals("CC", event.getChannel());
    TestHelper.validateAsDateTime(event.getDateTime());
    TestHelper.validateAsUUID(event.getTransactionId());
    // Verify content of 'payload' part
    SurveyLaunchedResponse response = publishedEvent.getPayload().getResponse();
    assertEquals(questionnaireId, response.getQuestionnaireId());
    assertEquals(caseId, response.getCaseId());
    assertNull(response.getAgentId());
  }

  /**
   * This simulates a Rabbit failure during event posting, which should result in a 500 (internal
   * server) error.
   */
  @Test
  public void surveyLaunched_failsOnSend() throws Exception {
    // Simulate event posting failure
    Mockito.doThrow(AmqpException.class).when(rabbitTemplate).convertAndSend(any());

    // Read request body from resource file
    ObjectNode surveyLaunchedRequestBody = FixtureHelper.loadClassObjectNode();

    // Send a Post request to the /surveyLaunched endpoint
    mockMvc
        .perform(postJson("/surveyLaunched", surveyLaunchedRequestBody.toString()))
        .andExpect(status().isInternalServerError());
  }
}
