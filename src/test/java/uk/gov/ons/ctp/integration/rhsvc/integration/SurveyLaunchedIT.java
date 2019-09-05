package uk.gov.ons.ctp.integration.rhsvc.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.TestHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedEvent;
import uk.gov.ons.ctp.common.event.model.SurveyLaunchedResponse;
import uk.gov.ons.ctp.integration.rhsvc.endpoint.SurveyLaunchedEndpoint;

/**
 * This is a component test which submits a Post saying that a survey has been launched and uses a
 * mock of RabbitMQ to confirm that RH publishes a survey launched event.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SurveyLaunchedIT {
  @Autowired private SurveyLaunchedEndpoint surveyLaunchedEndpoint;

  @MockBean private RabbitTemplate rabbitTemplate;

  @Autowired EventPublisher eventPublisher;

  private MockMvc mockMvc;

  @Captor private ArgumentCaptor<SurveyLaunchedEvent> publishCaptor;

  @Before
  public void setUp() throws Exception {
    ReflectionTestUtils.setField(eventPublisher, "template", rabbitTemplate);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(surveyLaunchedEndpoint)
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
    String caseIdString = surveyLaunchedRequestBody.get("caseId").asText();

    // Send a Post request to the /surveyLaunched endpoint
    mockMvc
        .perform(postJson("/surveyLaunched", surveyLaunchedRequestBody.toString()))
        .andExpect(status().isOk());

    // Get ready to capture the survey details published to the exchange
    Mockito.verify(rabbitTemplate)
        .convertAndSend(eq("event.response.authentication"), publishCaptor.capture());
    SurveyLaunchedEvent publishedEvent = publishCaptor.getValue();

    // Validate contents of the published event
    Header event = publishedEvent.getEvent();
    assertEquals("SURVEY_LAUNCHED", event.getType());
    assertEquals("RESPONDENT_HOME", event.getSource());
    assertEquals("RH", event.getChannel());
    assertNotNull(event.getDateTime());
    TestHelper.validateAsUUID(event.getTransactionId());
    // Verify content of 'payload' part
    SurveyLaunchedResponse response = publishedEvent.getPayload().getResponse();
    assertEquals(questionnaireId, response.getQuestionnaireId());
    assertEquals(UUID.fromString(caseIdString), response.getCaseId());
    assertNull(response.getAgentId());
  }

  /**
   * This simulates a Rabbit failure during event posting, which should result in a 500 (internal
   * server) error.
   */
  @Test
  public void surveyLaunched_failsOnSend() throws Exception {
    // Simulate event posting failure
    Mockito.doThrow(AmqpException.class)
        .when(rabbitTemplate)
        .convertAndSend((String) eq("event.response.authentication"), (Object) any());

    // Read request body from resource file
    ObjectNode surveyLaunchedRequestBody = FixtureHelper.loadClassObjectNode();

    // Send a Post request to the /surveyLaunched endpoint
    mockMvc
        .perform(postJson("/surveyLaunched", surveyLaunchedRequestBody.toString()))
        .andExpect(status().isInternalServerError());
  }
}
