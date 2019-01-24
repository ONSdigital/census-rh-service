package uk.gov.ons.ctp.integration.rhsvc.endpoint;

// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// import static uk.gov.ons.ctp.common.MvcHelper.getJson;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.ResultActions;

/** Respondent Data Endpoint Unit tests */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public final class RespondentDataEndpointUnitTest {

  //  @Autowired private MockMvc mockMvc;

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  public void getRespondentDataFromEndpoint() throws Exception {

    assertThat(
            this.restTemplate.getForObject(
                "http://localhost:" + port + "/respondent/data", String.class))
        .contains("Hello Census Integration!");

    //    ResultActions actions = mockMvc.perform(getJson("/respondent/data"));
    //
    //    actions.andExpect(status().isOk());
  }
}
