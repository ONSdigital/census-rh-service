package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SmokeTest {

  @Autowired private RespondentHomeEndpoint controller;

  @Test
  @Ignore
  public void contexLoads() throws Exception {
    assertThat(controller).isNotNull();
  }
}
