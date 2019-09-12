package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.ons.ctp.integration.rhsvc.representation.FeedbackDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.FeedbackService;

/**
 * This class holds unit tests for the Feedback Endpoint to validate its handling of requests and
 * their parameters.
 */
public final class FeedbackEndpointUnitTest {
  @InjectMocks private FeedbackEndpoint feedbackEndpoint;

  @Mock FeedbackService feedbackService;

  private MockMvc mockMvc;

  private ObjectMapper mapper = new ObjectMapper();

  private FeedbackDTO feedbackDTO;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    this.mockMvc =
        MockMvcBuilders.standaloneSetup(feedbackEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();

    this.feedbackDTO = FixtureHelper.loadClassFixtures(FeedbackDTO[].class).get(0);
  }

  @Test
  public void feedbackSubmittal_valid() throws Exception {
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isOk());
  }

  @Test
  public void feedbackSubmittal_nullChannel() throws Exception {
    feedbackDTO.setChannel(null);
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void feedbackSubmittal_pageUrlIsNull() throws Exception {
    feedbackDTO.setPageUrl(null);
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void feedbackSubmittal_pageUrlTooLong() throws Exception {
    feedbackDTO.setPageUrl(createLongString(4000));
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void feedbackSubmittal_pageTitleIsNull() throws Exception {
    feedbackDTO.setPageTitle(null);
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void feedbackSubmittal_pageTitleTooLong() throws Exception {
    feedbackDTO.setPageTitle(createLongString(4000));
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void feedbackSubmittal_feedbackIsNull() throws Exception {
    feedbackDTO.setFeedbackText(null);
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void feedbackSubmittal_FeedbackTooLong() throws Exception {
    feedbackDTO.setFeedbackText(createLongString(15000));
    String feedbackAsJson = mapper.writeValueAsString(feedbackDTO);
    mockMvc.perform(postJson("/feedback", feedbackAsJson)).andExpect(status().isBadRequest());
  }

  private String createLongString(int len) {
    return "x".repeat(len);
  }
}
