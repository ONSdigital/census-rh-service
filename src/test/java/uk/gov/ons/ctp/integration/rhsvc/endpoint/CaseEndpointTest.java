package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

public final class CaseEndpointTest {
  private static final String UPRN = "123456";
  private static final String INVALID_UPRN = "q23456";
  private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";
  private static final String ERROR_MESSAGE = "Failed to retrieve UPRN";
  private static final String INVALID_CODE = "VALIDATION_FAILED";
  private static final String INVALID_MESSAGE = "Provided json is incorrect.";

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  private MockMvc mockMvc;

  private SMSFulfilmentRequestDTO smsFulfilmentRequest;

  private ObjectMapper mapper = new ObjectMapper();

  private List<CaseDTO> caseDTO;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new CustomObjectMapper()))
            .build();
    this.caseDTO = FixtureHelper.loadClassFixtures(CaseDTO[].class);

    this.smsFulfilmentRequest =
        FixtureHelper.loadClassFixtures(SMSFulfilmentRequestDTO[].class).get(0);
  }

  //    this.mockMvc =
  //        MockMvcBuilders.standaloneSetup(caseEndpoint)
  //            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
  //            .build();
  //  }

  /** Test returns valid JSON for valid UPRN */
  @Test
  public void getHHCaseByUPRNFound() throws Exception {
    CaseDTO rmCase0 = caseDTO.get(0);
    CaseDTO rmCase1 = caseDTO.get(1);

    when(caseService.getHHCaseByUPRN(new UniquePropertyReferenceNumber(UPRN))).thenReturn(caseDTO);

    mockMvc
        .perform(get("/cases/uprn/{uprn}", UPRN))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$.[0].id", is(rmCase0.getId().toString())))
        .andExpect(jsonPath("$.[0].caseRef", is(rmCase0.getCaseRef())))
        .andExpect(jsonPath("$.[0].addressType", is(rmCase0.getAddressType())))
        .andExpect(jsonPath("$.[0].state", is(rmCase0.getState())))
        .andExpect(jsonPath("$.[0].addressLine1", is(rmCase0.getAddressLine1())))
        .andExpect(jsonPath("$.[0].townName", is(rmCase0.getTownName())))
        .andExpect(jsonPath("$.[0].postcode", is(rmCase0.getPostcode())))
        .andExpect(jsonPath("$.[0].uprn", is(Long.toString(rmCase0.getUprn().getValue()))))
        .andExpect(jsonPath("$.[1].id", is(rmCase1.getId().toString())))
        .andExpect(jsonPath("$.[1].caseRef", is(rmCase1.getCaseRef())))
        .andExpect(jsonPath("$.[1].addressType", is(rmCase1.getAddressType())))
        .andExpect(jsonPath("$.[1].state", is(rmCase1.getState())))
        .andExpect(jsonPath("$.[1].addressLine1", is(rmCase1.getAddressLine1())))
        .andExpect(jsonPath("$.[1].townName", is(rmCase1.getTownName())))
        .andExpect(jsonPath("$.[1].postcode", is(rmCase1.getPostcode())))
        .andExpect(jsonPath("$.[1].uprn", is(Long.toString(rmCase1.getUprn().getValue()))));
  }

  /** Test returns resource not found for non-existent UPRN */
  @Test
  public void getHHCaseByUPRNNotFound() throws Exception {

    when(caseService.getHHCaseByUPRN(new UniquePropertyReferenceNumber(UPRN)))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, ERROR_MESSAGE));

    mockMvc
        .perform(get("/cases/uprn/{uprn}", UPRN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code", is(ERROR_CODE)))
        .andExpect(jsonPath("$.error.message", is(ERROR_MESSAGE)));
  }

  /** Test returns bad request for invalid UPRN */
  @Test
  public void getHHCaseByUPRNBadRequest() throws Exception {

    mockMvc
        .perform(get("/cases/uprn/{uprn}", INVALID_UPRN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is(INVALID_CODE)))
        .andExpect(jsonPath("$.error.message", is(INVALID_MESSAGE)));
  }

  @Test
  public void fulfilmentRequestBySMS_valid() throws Exception {
    String url = "/cases/" + smsFulfilmentRequest.getCaseId() + "/fulfilments/sms";
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isOk());
  }

  @Test
  public void fulfilmentRequestBySMS_mismatchedCaseIds() throws Exception {
    String url = "/cases/81455015-28b1-4975-b2f1-540d0b8876b6/fulfilments/sms";
    smsFulfilmentRequest.setCaseId(UUID.randomUUID());
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void fulfilmentRequestBySMS_phoneNumberFailsRegex() throws Exception {
    String url = "/cases/81455015-28b1-4975-b2f1-540d0b8876b6/fulfilments/sms";
    smsFulfilmentRequest.setTelNo("abc123");
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isBadRequest());
  }

  @Test
  public void fulfilmentRequestBySMS_incorrectRequestBody() throws Exception {
    String url = "/cases/81455015-28b1-4975-b2f1-540d0b8876b6/fulfilments/sms";
    String requestAsJson = "{ \"name\": \"Fred\" }";
    mockMvc.perform(postJson(url, requestAsJson)).andExpect(status().isBadRequest());
  }
}
