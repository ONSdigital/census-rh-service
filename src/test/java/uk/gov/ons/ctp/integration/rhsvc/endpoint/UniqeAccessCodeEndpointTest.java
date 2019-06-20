package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.ons.ctp.common.utility.MockMvcControllerAdviceHelper.mockAdviceFor;

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
public class UniqeAccessCodeEndpointTest {

  private static final String UAC = "w4nwwpphjjpt";
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
    when(uacService.getAndAuthenticateUAC(UAC)).thenReturn(uacDTO.get(0));

    mockMvc
        .perform(get(String.format("/uacs/%s", UAC)))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json;charset=UTF-8"))
        .andExpect(jsonPath("$.uac", is(UAC)))
        .andExpect(jsonPath("$.caseId", is(CASE_ID)))
        .andExpect(jsonPath("$.address.postcode", is(POSTCODE)));
  }

  /** Test returns resource not found for invalid UAC */
  @Test
  public void getUACClaimContextUACNotFound() throws Exception {
    when(uacService.getAndAuthenticateUAC(UAC))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, ERROR_MESSAGE));

    mockMvc
        .perform(get("/uacs/{uac}", UAC))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code", is(ERROR_CODE)))
        .andExpect(jsonPath("$.error.message", is(ERROR_MESSAGE)));
  }
}
