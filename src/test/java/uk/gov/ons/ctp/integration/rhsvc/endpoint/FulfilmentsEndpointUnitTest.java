package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.getJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.common.product.model.Product.CaseType.HH;
import static uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel.SMS;
import static uk.gov.ons.ctp.integration.common.product.model.Product.ProductGroup.UAC;
import static uk.gov.ons.ctp.integration.common.product.model.Product.Region.E;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
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

  private List<ProductDTO> productDTO;
  private ProductDTO product;

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
    this.productDTO = FixtureHelper.loadClassFixtures(ProductDTO[].class);
    this.product = productDTO.get(0);
  }

  @Test
  public void fulfilmentsReqestNoParameters() throws Exception {
    Mockito.when(fulfilmentsService.getFulfilments(Arrays.asList(), null, null, null, null))
        .thenReturn(productDTO);

    ResultActions action = mockMvc.perform(getJson("/fulfilments"));
    statusOk(action);
    Mockito.verify(fulfilmentsService).getFulfilments(Arrays.asList(), null, null, null, null);
  }

  @Test
  public void fulfilmentsReqestAllParameters() throws Exception {
    List<ProductDTO> emptyList = new ArrayList<>();
    Mockito.when(fulfilmentsService.getFulfilments(Arrays.asList(HH), E, SMS, UAC, true))
        .thenReturn(productDTO);
    ResultActions action =
        mockMvc.perform(
            getJson(
                "/fulfilments?caseType=HH&region=E&deliveryChannel=SMS&individual=true&productGroup=UAC"));
    statusOk(action);
    Mockito.verify(fulfilmentsService).getFulfilments(Arrays.asList(HH), E, SMS, UAC, true);
  }

  @Test
  public void fulfilmentsReqestWithInvalidCaseType() throws Exception {
    ResultActions action = mockMvc.perform(getJson("/fulfilments?caseType=X"))
       .andExpect(status().isBadRequest())
       .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
  }

  @Test
  public void fulfilmentsReqestWithInvalidRegion() throws Exception {
    mockMvc
        .perform(getJson("/fulfilments?region=X"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
  }

  @Test
  public void fulfilmentsReqestWithInvalidDeliveryChannel() throws Exception {
    mockMvc
        .perform(getJson("/fulfilments?deliveryChannel=X"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
  }

  @Test
  public void fulfilmentsRequestWithInvalidProductGroup() throws Exception {
    mockMvc
        .perform(getJson("/fulfilments?productGroup=XXX"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")));
  }

  private void statusOk(ResultActions action) throws Exception {
    action.andExpect(status().isOk());
    action.andExpect(jsonPath("$.[0].fulfilmentCode", is(product.getFulfilmentCode())));
    action.andExpect(jsonPath("$.[0].language", is(product.getLanguage().toString())));
    action.andExpect(jsonPath("$.[0].productGroup", is(product.getProductGroup().toString())));
    action.andExpect(jsonPath("$.[0].caseTypes[0]", is(product.getCaseTypes().get(0).name())));
    action.andExpect(jsonPath("$.[0].caseTypes[1]", is(product.getCaseTypes().get(1).name())));
    action.andExpect(jsonPath("$.[0].individual", is(product.getIndividual())));
    action.andExpect(
        jsonPath("$.[0].regions", is(Arrays.asList(product.getRegions().get(0).name()))));
    action.andExpect(
        jsonPath("$.[0].deliveryChannel", is(product.getDeliveryChannel().toString())));
    action.andExpect(jsonPath("$.[0].handler", is(product.getHandler().toString())));
  }
}
