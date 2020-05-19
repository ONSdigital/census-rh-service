package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.MvcHelper.postJson;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;
import static uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture.EXPECTED_JSON_CONTENT_TYPE;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.UniqueAccessCodeService;

/** Unit Tests on endpoint for UAC resources */
public class UniqueAccessCodeEndpointTest {

  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";
  private static final String POSTCODE = "UP103UP";
  private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";
  private static final String ERROR_MESSAGE = "Failed to retrieve UAC";

  @InjectMocks private UniqueAccessCodeEndpoint uacEndpoint;

  @Mock UniqueAccessCodeService uacService;

  private MockMvc mockMvc;

  private List<UniqueAccessCodeDTO> uacDTO;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(uacEndpoint)
            .setHandlerExceptionResolvers(mockAdviceFor(RestExceptionHandler.class))
            .build();
    this.uacDTO = FixtureHelper.loadClassFixtures(UniqueAccessCodeDTO[].class);
  }

  /** Test returns valid JSON for valid UAC */
  @Test
  public void getUACClaimContextUACFound() throws Exception {
    when(uacService.getAndAuthenticateUAC(UAC_HASH)).thenReturn(uacDTO.get(0));

    mockMvc
        .perform(get(String.format("/uacs/%s", UAC_HASH)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(jsonPath("$.uacHash", is(UAC_HASH)))
        .andExpect(jsonPath("$.caseId", is(CASE_ID)))
        .andExpect(jsonPath("$.address.postcode", is(POSTCODE)))
        .andExpect(jsonPath("$.handDelivery", is(true)));
  }

  /** Test returns resource not found for invalid UAC */
  @Test
  public void getUACClaimContextUACNotFound() throws Exception {
    when(uacService.getAndAuthenticateUAC(UAC_HASH))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, ERROR_MESSAGE));

    mockMvc
        .perform(get("/uacs/{uac}", UAC_HASH))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code", is(ERROR_CODE)))
        .andExpect(jsonPath("$.error.message", is(ERROR_MESSAGE)));
  }

  @Test
  public void linkUACtoCase() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("UACLinkRequestDTO");

    when(uacService.linkUACCase(eq(UAC_HASH), any())).thenReturn(uacDTO.get(0));

    mockMvc
        .perform(postJson("/uacs/" + UAC_HASH + "/link", json.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(EXPECTED_JSON_CONTENT_TYPE))
        .andExpect(jsonPath("$.uacHash", is(UAC_HASH)))
        .andExpect(jsonPath("$.caseId", is(CASE_ID)))
        .andExpect(jsonPath("$.address.postcode", is(POSTCODE)))
        .andExpect(jsonPath("$.handDelivery", is(true)));
  }

  @Test
  public void linkUACtoCaseUACNotFound() throws Exception {
    ObjectNode json = FixtureHelper.loadClassObjectNode("UACLinkRequestDTO");

    when(uacService.linkUACCase(eq(UAC_HASH), any()))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, ERROR_MESSAGE));

    mockMvc
        .perform(postJson("/uacs/" + UAC_HASH + "/link", json.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code", is(ERROR_CODE)))
        .andExpect(jsonPath("$.error.message", is(ERROR_MESSAGE)));
  }
}
