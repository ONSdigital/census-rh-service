package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import java.util.ArrayList;
import java.util.List;
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
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.rhsvc.representation.ProductDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.FulfilmentsService;

/**
 * This class holds unit tests for the Fulfilments Endpoint to validate its handling of requests and
 * their parameters.
 */
@RunWith(MockitoJUnitRunner.class)
public final class FulfilmentsEndpointUnitTest {
  @InjectMocks private FulfilmentsEndpoint fulfilmentsEndpoint;

  @Mock FulfilmentsService fulfilmentsService;

  private MockMvc mockMvc;

  /**
   * Set up of tests
   *
   * @throws Exception exception thrown
   */
  @Before
  public void setUp() throws Exception {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(fulfilmentsEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
  }

  @Test
  public void fulfilmentsReqestNoParameters() throws Exception {
    mockMvc.perform(getJson("/fulfilments")).andExpect(status().isOk());
  }

  @Test
  public void fulfilmentsReqestAllParameters() throws Exception {
    List<ProductDTO> emptyList = new ArrayList<>();
    Mockito.when(fulfilmentsService.getFulfilments(any(), any(), any(), any(), anyBoolean()))
        .thenReturn(emptyList);

    mockMvc
        .perform(
            getJson(
                "/fulfilments?caseType=HH&region=E&deliveryChannel=SMS&individual=false&productGroup=UAC"))
        .andExpect(status().isOk());
  }

  @Test
  public void fulfilmentsReqestWithInvalidCaseType() throws Exception {
    mockMvc.perform(getJson("/fulfilments?caseType=X")).andExpect(status().isBadRequest());
  }

  @Test
  public void fulfilmentsReqestWithInvalidRegion() throws Exception {
    mockMvc.perform(getJson("/fulfilments?region=X")).andExpect(status().isBadRequest());
  }

  @Test
  public void fulfilmentsReqestWithInvalidDeliveryChannel() throws Exception {
    mockMvc.perform(getJson("/fulfilments?deliveryChannel=X")).andExpect(status().isBadRequest());
  }

  @Test
  public void fulfilmentsRequestWithInvalidProductGroup() throws Exception {
    mockMvc.perform(getJson("/fulfilments?productGroup=XXX")).andExpect(status().isBadRequest());
  }
}
