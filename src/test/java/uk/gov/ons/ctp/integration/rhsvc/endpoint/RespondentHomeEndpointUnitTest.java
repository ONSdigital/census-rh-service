package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentHomeService;

/** Respondent Home Endpoint Unit tests */
public final class RespondentHomeEndpointUnitTest {
  @InjectMocks private RespondentHomeEndpoint respondentHomeEndpoint;

  @Mock RespondentHomeService respondentHomeService;

  private MockMvc mockMvc;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(respondentHomeEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }


  @Test
  public void surveyLaunched() throws Exception {
    Mockito.doNothing().when(respondentHomeService).surveyLaunched(any());
    
    String surveyLaunchedRequestBody = "{ \"questionnaireId\": \"23434234234\",   \"caseId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\" }";
    mockMvc.perform(postJson("/surveyLaunched", surveyLaunchedRequestBody))
        .andExpect(status().isOk());
  }

  @Test
  public void surveyLaunchedWithMissingQuestionnaireId() throws Exception {
    String surveyLaunchedRequestBody = "{ \"caseId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\" }";
    mockMvc.perform(postJson("/surveyLaunched", surveyLaunchedRequestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void surveyLaunchedWithInvalidCaseId() throws Exception {
    String surveyLaunchedRequestBody = "{ \"questionnaireId\": \"23434234234\",   \"caseId\": \"euieuieu@#$@#$\" }";
    mockMvc.perform(postJson("/surveyLaunched", surveyLaunchedRequestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void surveyLaunchedWithInvalidRequest() throws Exception {
    String surveyLaunchedRequestBody = "uoeuoeu 45345345 euieuiaooo";
    mockMvc.perform(postJson("/surveyLaunched", surveyLaunchedRequestBody))
        .andExpect(status().isBadRequest());
  }
}
