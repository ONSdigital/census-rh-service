package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcCircuitBreakerConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

/**
 * Test that uses real circuit-breaker behaviour. Values are made small to stop the tests taking too
 * many seconds to run.
 */
@EnableConfigurationProperties
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(
    classes = {
      WebformServiceImpl.class,
      AppConfig.class,
      RHSvcCircuitBreakerConfig.class,
      ValidationAutoConfiguration.class
    })
@TestPropertySource(
    properties = {
      "webform-circuit-breaker.min-number-of-calls=4",
      "webform-circuit-breaker.sliding-window-size=4",
      "webform-circuit-breaker.timeout=1",
      "webform-circuit-breaker.wait-duration-seconds-in-open-state=2",
      "webform-circuit-breaker.permitted-number-of-calls-in-half-open-state=1"
    })
public class WebformServiceImpl_IT extends WebformServiceImplTestBase {
  private static final int WINDOW_SIZE = 4;
  private static final int TIMEOUT = 1;

  @MockBean RateLimiterClient rateLimiterClient;

  private void mockSlowSend() throws Exception {
    doAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep((1000 * TIMEOUT) + 500);
                return new SendEmailResponse(SEND_EMAIL_RESPONSE_JSON);
              }
            })
        .when(notificationClient)
        .sendEmail(any(), any(), any(), any());
  }

  @Test
  public void shouldSendEmail() throws Exception {
    mockSuccessfulSend();
    webformService.sendWebformEmail(webform);
  }

  @DirtiesContext
  @Test
  public void shouldFailToSendEmail() throws Exception {
    mockFailedSend();
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
    assertTrue(e.getCause().getCause() instanceof NotificationClientException);
  }

  @DirtiesContext
  @Test
  public void shouldFailToSendEmailWhenSlowResponse() throws Exception {
    mockSlowSend();
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
    assertTrue(e.getCause() instanceof TimeoutException);
  }

  @DirtiesContext
  @Test
  public void shouldBreakCircuitOnMultipleFailToSendEmail() throws Exception {
    mockFailedSend();
    for (int i = 0; i < WINDOW_SIZE; i++) {
      RuntimeException e =
          assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
      assertTrue(
          "wrong type of exception: " + e.getCause().getClass() + " on iteration: " + i,
          e.getCause().getCause() instanceof NotificationClientException);
    }
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
    assertTrue(e.getCause() instanceof CallNotPermittedException);
  }
}
