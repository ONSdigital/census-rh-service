package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.rhsvc.representation.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.SurveyLaunchedService;

/** Respondent Home Endpoint Unit tests */
@RunWith(MockitoJUnitRunner.class)
public final class SurveyLaunchedEndpointUnitTest {
  @InjectMocks private SurveyLaunchedEndpoint surveyLaunchedEndpoint;

  @Mock SurveyLaunchedService surveyLaunchedService;

  private ObjectMapper mapper = new ObjectMapper();

  private MockMvc mockMvc;
  private SurveyLaunchedDTO dto;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(surveyLaunchedEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
    dto = FixtureHelper.loadClassFixtures(SurveyLaunchedDTO[].class).get(0);
  }

  @Test
  public void surveyLaunchedSuccessCaseEmptyString() throws Exception {
    Mockito.doNothing().when(surveyLaunchedService).surveyLaunched(any());
    callEndointExpectingSuccess();
  }

  @Test
  public void surveyLaunchedSuccessCaseAssistedDigitalLocation() throws Exception {
    Mockito.doNothing().when(surveyLaunchedService).surveyLaunched(any());
    callEndointExpectingSuccess();
  }

  @Test
  public void shouldRejectMissingQuestionnaireId() throws Exception {
    dto.setQuestionnaireId(null);
    callEndointExpectingBadRequest();
  }

  @Test
  public void shouldLaunchWithMissingClientIP() throws Exception {
    dto.setClientIP(null);
    callEndointExpectingSuccess();
  }

  @Test
  public void shouldRejectInvalidCaseId() throws Exception {
    String surveyLaunchedRequestBody =
        "{ \"questionnaireId\": \"23434234234\",   \"caseId\": \"euieuieu@#$@#$\" }";
    callEndointExpectingBadRequest(surveyLaunchedRequestBody);
  }

  @Test
  public void shouldRejectInvalidRequest() throws Exception {
    String surveyLaunchedRequestBody = "uoeuoeu 45345345 euieuiaooo";
    callEndointExpectingBadRequest(surveyLaunchedRequestBody);
  }

  private void callEndointExpectingSuccess() throws Exception {
    mockMvc
        .perform(postJson("/surveyLaunched", mapper.writeValueAsString(dto)))
        .andExpect(status().isOk());
  }

  private void callEndointExpectingBadRequest() throws Exception {
    callEndointExpectingBadRequest(mapper.writeValueAsString(dto));
  }

  private void callEndointExpectingBadRequest(String body) throws Exception {
    mockMvc.perform(postJson("/surveyLaunched", body)).andExpect(status().isBadRequest());
  }
}
