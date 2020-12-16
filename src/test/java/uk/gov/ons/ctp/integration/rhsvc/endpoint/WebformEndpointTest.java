package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;

/** Respondent Home Endpoint Unit tests */
@RunWith(MockitoJUnitRunner.class)
public final class WebformEndpointTest {
  @InjectMocks private WebformEndpoint webformEndpoint;

  @Mock WebformService webformService;

  private MockMvc mockMvc;
  private ObjectMapper mapper = new ObjectMapper();

  private WebformDTO webformRequest;
  private String webformRequestJson;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(webformEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();

    webformRequest = FixtureHelper.loadClassFixtures(WebformDTO[].class).get(0);
    webformRequestJson = mapper.writeValueAsString(webformRequest);
  }

  @Test
  public void webform_validRequest() throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/webform")
                .content(mapper.writeValueAsString(webformRequest))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  public void webform_nullCategory() throws Exception {
    webformRequest.setCategory(null);
    invokeEndpointAndExpectBadRequest(webformRequest);
  }

  @Test
  public void webform_unknownCategory() throws Exception {
    webformRequestJson = updateJson(webformRequestJson, "category", "foo");
    invokeEndpointAndExpectBadRequest(webformRequestJson);
  }

  @Test
  public void webform_nullRegion() throws Exception {
    webformRequest.setRegion(null);
    invokeEndpointAndExpectBadRequest(webformRequest);
  }

  @Test
  public void webform_unknownRegion() throws Exception {
    webformRequestJson = updateJson(webformRequestJson, "region", "foo");
    invokeEndpointAndExpectBadRequest(webformRequestJson);
  }

  @Test
  public void webform_nullLanguage() throws Exception {
    webformRequest.setLanguage(null);
    invokeEndpointAndExpectBadRequest(webformRequest);
  }

  @Test
  public void webform_unknownLanguage() throws Exception {
    webformRequestJson = updateJson(webformRequestJson, "language", "foo");
    invokeEndpointAndExpectBadRequest(webformRequestJson);
  }

  @Test
  public void webform_nullName() throws Exception {
    webformRequest.setName(null);
    invokeEndpointAndExpectBadRequest(webformRequest);
  }

  @Test
  public void webform_nullDescription() throws Exception {
    webformRequest.setDescription(null);
    invokeEndpointAndExpectBadRequest(webformRequest);
  }

  @Test
  public void webform_nullEmail() throws Exception {
    webformRequest.setEmail(null);
    invokeEndpointAndExpectBadRequest(webformRequest);
  }

  private void invokeEndpointAndExpectBadRequest(WebformDTO webformRequest)
      throws Exception, JsonProcessingException {
    String webformAsJson = mapper.writeValueAsString(webformRequest);
    invokeEndpointAndExpectBadRequest(webformAsJson);
  }

  private void invokeEndpointAndExpectBadRequest(String webformRequestJson) throws Exception {
    mockMvc.perform(postJson("/webform", webformRequestJson)).andExpect(status().isBadRequest());
  }

  private String updateJson(String webformRequestJson, String fieldName, String newValue) {
    String regex = fieldName + ".*?,";
    String replacement = fieldName + "\":\"" + newValue + "\",";
    return webformRequestJson.replaceFirst(regex, replacement);
  }
}
