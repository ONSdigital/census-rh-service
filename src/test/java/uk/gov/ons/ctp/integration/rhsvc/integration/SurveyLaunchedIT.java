package uk.gov.ons.ctp.integration.rhsvc.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.TestHelper;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.endpoint.RespondentHomeEndpoint;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.GenericCaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Header;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
// @EnableAutoConfiguration(exclude=CaseEventReceiverConfiguration.class)
// @TestPropertySource(properties= {"spring.autoconfigure.exclude=CaseEventReceiverConfiguration"})
public class SurveyLaunchedIT {
  @Autowired private RespondentHomeEndpoint respondentHomeEndpoint;

  @MockBean(name = "surveyLaunchedRabbitTemplate")
  RabbitTemplate rabbitTemplate;

  private MockMvc mockMvc;

  @Captor ArgumentCaptor<GenericCaseEvent> publishCaptor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    this.mockMvc = MockMvcBuilders.standaloneSetup(respondentHomeEndpoint).build();
  }

  /**
   * This test submits a generic address query and validates that some data is returned in the
   * expected format. Without a fixed test data set this is really as much validation as it can do.
   */
  @Test
  public void validateAddressQueryResponse() throws Exception {
    String questionnaireId = "23434234234";
    String caseId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String surveyLaunchedRequestBody =
        "{"
            + "\"questionnaireId\": \""
            + questionnaireId
            + "\","
            + "\"caseId\": \""
            + caseId
            + "\""
            + "}";
    mockMvc
        .perform(postJson("/surveyLaunched", surveyLaunchedRequestBody))
        .andExpect(status().isOk());

    Mockito.verify(rabbitTemplate).convertAndSend(publishCaptor.capture());
    GenericCaseEvent publishedEvent = publishCaptor.getValue();

    Header event = publishedEvent.getEvent();
    assertEquals("SURVEY_LAUNCHED", event.getType());
    assertEquals("CONTACT_CENTRE_API", event.getSource());
    assertEquals("CC", event.getChannel());
    TestHelper.validateAsDateTime(event.getDateTime());
    TestHelper.validateAsUUID(event.getTransactionId());

    CaseEvent response = publishedEvent.getPayload().getResponse();
    assertEquals(questionnaireId, response.getProperties().get("questionnaireId"));
    assertEquals(caseId, response.getProperties().get("caseId"));
    assertNull(response.getProperties().get("agentId"));
  }
}
