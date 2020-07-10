package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture.EXPECTED_JSON_CONTENT_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** Unit Tests on endpoint for Case resources */
@RunWith(MockitoJUnitRunner.class)
public class CaseEndpointUnitTest {

  private static final String UPRN = "123456";
  private static final String INVALID_UPRN = "q23456";
  private static final String INCONSISTENT_CASEID = "ff9999f9-ff9f-9f99-f999-9ff999ff9ff9";
  private static final String ERROR_MESSAGE = "Failed to retrieve UPRN";
  private static final String INVALID_CODE = "VALIDATION_FAILED";
  private static final String INVALID_MESSAGE = "Provided json is incorrect.";
  private static final String CASEID_UPRN_INCONSISTENT =
      "The UPRN of the referenced Case and the provided Address UPRN must be matching";
  private static final String CASEID_INCONSISTENT =
      "The caseid in the modifyAddress URL does not match the caseid in the request body";

  @InjectMocks private CaseEndpoint caseEndpoint;

  @Mock CaseService caseService;

  private MockMvc mockMvc;

  private SMSFulfilmentRequestDTO smsFulfilmentRequest;

  private ObjectMapper mapper = new ObjectMapper();

  private List<CaseDTO> caseDTO;

  /** Setup tests */
  @Before
  public void setUp() {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(caseEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .build();
    this.caseDTO = FixtureHelper.loadClassFixtures(CaseDTO[].class);

    this.smsFulfilmentRequest =
        FixtureHelper.loadClassFixtures(SMSFulfilmentRequestDTO[].class).get(0);
  }

  /** Test returns valid JSON for valid UPRN */
  @Test
  public void getCaseByUPRNFound() throws Exception {
    CaseDTO rmCase0 = caseDTO.get(0);

    when(caseService.getLatestValidNonHICaseByUPRN(new UniquePropertyReferenceNumber(UPRN)))
        .thenReturn(caseDTO.get(0));

    mockMvc
        .perform(get("/cases/uprn/{uprn}", UPRN))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        // .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$.caseId", is(rmCase0.getCaseId().toString())))
        .andExpect(jsonPath("$.caseRef", is(rmCase0.getCaseRef())))
        .andExpect(jsonPath("$.caseType", is(rmCase0.getCaseType())))
        .andExpect(jsonPath("$.addressType", is(rmCase0.getAddressType())))
        .andExpect(jsonPath("$.addressLevel", is(rmCase0.getAddressLevel())))
        .andExpect(jsonPath("$.addressLine1", is(rmCase0.getAddress().getAddressLine1())))
        .andExpect(jsonPath("$.townName", is(rmCase0.getAddress().getTownName())))
        .andExpect(jsonPath("$.postcode", is(rmCase0.getAddress().getPostcode())));
  }

  /** Test returns resource not found for non-existent UPRN */
  @Test
  public void getCaseByUPRNNotFound() throws Exception {

    when(caseService.getLatestValidNonHICaseByUPRN(new UniquePropertyReferenceNumber(UPRN)))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, ERROR_MESSAGE));

    mockMvc
        .perform(get("/cases/uprn/{uprn}", UPRN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code", is(CTPException.Fault.RESOURCE_NOT_FOUND.toString())))
        .andExpect(jsonPath("$.error.message", is(ERROR_MESSAGE)));
  }

  /** Test returns bad request for invalid UPRN */
  @Test
  public void getCaseByUPRNBadRequest() throws Exception {

    mockMvc
        .perform(get("/cases/uprn/{uprn}", INVALID_UPRN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is(INVALID_CODE)))
        .andExpect(jsonPath("$.error.message", is(INVALID_MESSAGE)));
  }

  /** Test returns valid JSON for valid caseId */
  @Test
  public void putModifyAddressByCaseIdOK() throws Exception {
    CaseDTO rmCase = caseDTO.get(0);
    AddressChangeDTO addressChangeDTO =
        new AddressChangeDTO(rmCase.getCaseId(), rmCase.getAddress());

    when(caseService.modifyAddress(addressChangeDTO)).thenReturn(rmCase);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/cases/{caseId}/address", rmCase.getCaseId().toString())
                .content(mapper.writeValueAsString(addressChangeDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(jsonPath("$.caseId", is(rmCase.getCaseId().toString())))
        .andExpect(jsonPath("$.caseRef", is(rmCase.getCaseRef())))
        .andExpect(jsonPath("$.caseType", is(rmCase.getCaseType())))
        .andExpect(jsonPath("$.addressType", is(rmCase.getAddressType())))
        .andExpect(jsonPath("$.addressLevel", is(rmCase.getAddressLevel())))
        .andExpect(jsonPath("$.addressLine1", is(rmCase.getAddress().getAddressLine1())))
        .andExpect(jsonPath("$.addressLine2", is(rmCase.getAddress().getAddressLine2())))
        .andExpect(jsonPath("$.addressLine3", is(rmCase.getAddress().getAddressLine3())))
        .andExpect(jsonPath("$.townName", is(rmCase.getAddress().getTownName())))
        .andExpect(jsonPath("$.postcode", is(rmCase.getAddress().getPostcode())));
  }

  /** Test returns bad request if caseId path parameter and body don't match */
  @Test
  public void putModifyAddressByCaseIdInconsistent() throws Exception {
    CaseDTO rmCase = caseDTO.get(0);
    AddressChangeDTO addressChangeDTO =
        new AddressChangeDTO(UUID.fromString(rmCase.getCaseId().toString()), rmCase.getAddress());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/cases/{caseId}/address", INCONSISTENT_CASEID)
                .content(mapper.writeValueAsString(addressChangeDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is(CTPException.Fault.BAD_REQUEST.toString())))
        .andExpect(jsonPath("$.error.message", is(CASEID_INCONSISTENT)));
  }

  /** Test returns bad request for missing UPRN */
  @Test
  public void putModifyUPRNMissing() throws Exception {
    CaseDTO rmCase = caseDTO.get(0);
    UUID caseId = UUID.fromString(rmCase.getCaseId().toString());
    AddressChangeDTO addressChangeDTO = new AddressChangeDTO(caseId, rmCase.getAddress());
    addressChangeDTO.getAddress().setUprn(null);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/cases/{caseId}/address", rmCase.getCaseId().toString())
                .content(mapper.writeValueAsString(addressChangeDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is(INVALID_CODE)))
        .andExpect(jsonPath("$.error.message", containsString(INVALID_MESSAGE)));
  }

  /** Test returns bad request for missing address line 1 */
  @Test
  public void putModifyAddressLine1Missing() throws Exception {
    CaseDTO rmCase = caseDTO.get(0);
    UUID caseId = UUID.fromString(rmCase.getCaseId().toString());
    AddressChangeDTO addressChangeDTO = new AddressChangeDTO(caseId, rmCase.getAddress());
    addressChangeDTO.getAddress().setAddressLine1(null);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/cases/{caseId}/address", rmCase.getCaseId().toString())
                .content(mapper.writeValueAsString(addressChangeDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is(INVALID_CODE)))
        .andExpect(jsonPath("$.error.message", containsString(INVALID_MESSAGE)));
  }

  /** Test returns bad request for missing postcode */
  @Test
  public void putModifyPostcodeMissing() throws Exception {
    CaseDTO rmCase = caseDTO.get(0);
    UUID caseId = UUID.fromString(rmCase.getCaseId().toString());
    AddressChangeDTO addressChangeDTO = new AddressChangeDTO(caseId, rmCase.getAddress());
    addressChangeDTO.getAddress().setPostcode(null);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/cases/{caseId}/address", rmCase.getCaseId().toString())
                .content(mapper.writeValueAsString(addressChangeDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is(INVALID_CODE)))
        .andExpect(jsonPath("$.error.message", containsString(INVALID_MESSAGE)));
  }

  /** Test returns bad request for inconsistent caseId and UPRN */
  @Test
  public void putModifyCaseIdUPRNInconsistent() throws Exception {
    CaseDTO rmCase = caseDTO.get(0);
    AddressChangeDTO addressChangeDTO =
        new AddressChangeDTO(rmCase.getCaseId(), rmCase.getAddress());

    when(caseService.modifyAddress(addressChangeDTO))
        .thenThrow(new CTPException(CTPException.Fault.BAD_REQUEST, CASEID_UPRN_INCONSISTENT));

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/cases/{caseId}/address", rmCase.getCaseId().toString())
                .content(mapper.writeValueAsString(addressChangeDTO))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code", is(CTPException.Fault.BAD_REQUEST.toString())))
        .andExpect(jsonPath("$.error.message", is(CASEID_UPRN_INCONSISTENT)));
  }

  @Test
  public void fulfilmentRequestBySMS_valid() throws Exception {
    String url = "/cases/" + smsFulfilmentRequest.getCaseId() + "/fulfilments/sms";
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isOk());
    verify(caseService).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  }

  @Test
  public void fulfilmentRequestBySMS_mismatchedCaseIds() throws Exception {
    String url = "/cases/81455015-28b1-4975-b2f1-540d0b8876b6/fulfilments/sms";
    smsFulfilmentRequest.setCaseId(UUID.randomUUID());
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  }

  @Test
  public void fulfilmentRequestBySMS_phoneNumberFailsRegex() throws Exception {
    String url = "/cases/81455015-28b1-4975-b2f1-540d0b8876b6/fulfilments/sms";
    smsFulfilmentRequest.setTelNo("abc123");
    String smsFulfilmentRequestAsJson = mapper.writeValueAsString(smsFulfilmentRequest);
    mockMvc.perform(postJson(url, smsFulfilmentRequestAsJson)).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  }

  @Test
  public void fulfilmentRequestBySMS_incorrectRequestBody() throws Exception {
    String url = "/cases/81455015-28b1-4975-b2f1-540d0b8876b6/fulfilments/sms";
    String requestAsJson = "{ \"name\": \"Fred\" }";
    mockMvc.perform(postJson(url, requestAsJson)).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestBySMS(any(SMSFulfilmentRequestDTO.class));
  }

  @Test
  public void shouldFulfilByPost() throws Exception {
    String url = "/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/fulfilment/post";
    ObjectNode json = FixtureHelper.loadClassObjectNode("postal");
    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isOk());
    verify(caseService).fulfilmentRequestByPost(any(PostalFulfilmentRequestDTO.class));
  }

  @Test
  public void shouldRejectFulfilByPostWithMismatchedCaseIds() throws Exception {
    String url = "/cases/" + INCONSISTENT_CASEID + "/fulfilment/post";
    ObjectNode json = FixtureHelper.loadClassObjectNode("postal");
    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestByPost(any(PostalFulfilmentRequestDTO.class));
  }

  @Test
  public void shouldRejectFulfilByPostWithBadlyFormedCaseId() throws Exception {
    String url = "/cases/abc/fulfilment/post";
    ObjectNode json = FixtureHelper.loadClassObjectNode("postal");
    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestByPost(any(PostalFulfilmentRequestDTO.class));
  }

  @Test
  public void shouldRejectFulfilByPostWithBadlyFormedDate() throws Exception {
    String url = "/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/fulfilment/post";
    ObjectNode json = FixtureHelper.loadClassObjectNode("postal");
    json.put("dateTime", "2019:12:25 12:34:56");
    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestByPost(any(PostalFulfilmentRequestDTO.class));
  }

  @Test
  public void shouldRejectFulfilByPostWithMissingFulfilmentCode() throws Exception {
    String url = "/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/fulfilment/post";
    ObjectNode json = FixtureHelper.loadClassObjectNode("postal");
    json.remove("fulfilmentCode");
    mockMvc.perform(postJson(url, json.toString())).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestByPost(any(PostalFulfilmentRequestDTO.class));
  }

  @Test
  public void shouldRejectFulfilByPostWithIncorrectRequestBody() throws Exception {
    String url = "/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/fulfilment/post";
    String json = "{ \"name\": \"Fred\" }";
    mockMvc.perform(postJson(url, json)).andExpect(status().isBadRequest());
    verify(caseService, never()).fulfilmentRequestByPost(any(PostalFulfilmentRequestDTO.class));
  }
}
