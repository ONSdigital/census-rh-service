package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

public final class CaseEndpointUnitTest {
  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  private MockMvc mockMvc;

  private SMSFulfilmentRequestDTO smsFulfilmentRequest;

  private ObjectMapper mapper = new ObjectMapper();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    this.smsFulfilmentRequest =
        FixtureHelper.loadClassFixtures(SMSFulfilmentRequestDTO[].class).get(0);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void validSmsFulfilmentRequest() throws Exception {
    String url = "/cases/" + smsFulfilmentRequest.getCaseId() + "/fulfilments/sms";
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isOk());
  }

  @Test
  public void mismatchedCaseIds() throws Exception {
    String url = "/cases/" + UUID.randomUUID() + "/fulfilments/sms";
    smsFulfilmentRequest.setCaseId(UUID.randomUUID());
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void incorrectRequestBody() throws Exception {
    String url = "/cases/" + UUID.randomUUID() + "/fulfilments/sms";
    String requestAsJson = "{ \"name\": \"Fred\" }";
    mockMvc.perform(postJson(url, requestAsJson)).andExpect(status().isBadRequest());
  }
}
