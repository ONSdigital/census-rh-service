package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.validation.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.common.config.CustomCircuitBreakerConfig;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.NotifyConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.RateLimiterConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.WebformConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;
import uk.gov.service.notify.NotificationClientException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    classes = {WebformServiceImpl.class, AppConfig.class, ValidationAutoConfiguration.class})
public class WebformServiceImplTest extends WebformServiceImplTestBase {
  private static final String TEMPLATE_FULL_NAME = "respondent_full_name";
  private static final String TEMPLATE_EMAIL = "respondent_email";
  private static final String TEMPLATE_REGION = "respondent_region";
  private static final String TEMPLATE_CATEGORY = "respondent_category";
  private static final String TEMPLATE_DESCRIPTION = "respondent_description";

  private static final UUID NOTIFICATION_ID =
      UUID.fromString("8db6313a-d4e3-47a1-8d0e-ddd30c86e878");
  
  @MockBean private EventPublisher eventPublisher;
  
  private UUID notificationId;
  @Autowired AppConfig appConfig;

  @Mock private CustomCircuitBreakerConfig cbConfig;

  @MockBean(name = "webformCb")
  private CircuitBreaker webformCircuitBreaker;

  @Autowired WebformService webformService;

  @MockBean(name = "envoyLimiterCb")
  private CircuitBreaker envoyCircuitBreaker;

  @MockBean private RateLimiterClient rateLimiterClient;

  @Captor ArgumentCaptor<WebformDTO> webformEventCaptor;
  @Captor ArgumentCaptor<Map<String, String>> templateValueCaptor;

  @Before
  public void setUp() {
    NotifyConfig notifyConfig = new NotifyConfig();
    notifyConfig.setApiKey(UUID.randomUUID().toString());
    notifyConfig.setBaseUrl("https://api.notifications.service.gov.uk");
    appConfig.setNotify(notifyConfig);

    WebformConfig webformConfig = new WebformConfig();
    webformConfig.setTemplateId(UUID.randomUUID().toString());
    webformConfig.setEmailEn("english-delivered@notifications.service.gov.uk");
    webformConfig.setEmailCy("welsh-delivered@notifications.service.gov.uk");
    appConfig.setWebform(webformConfig);

    when(cbConfig.getTimeout()).thenReturn(4);
    appConfig.setWebformCircuitBreaker(cbConfig);
    simulateWebformCircuitBreaker();

    appConfig.setRateLimiter(rateLimiterConfig(true));
  }

  private RateLimiterConfig rateLimiterConfig(boolean enabled) {
    RateLimiterConfig rateLimiterConfig = new RateLimiterConfig();
    rateLimiterConfig.setEnabled(enabled);
    return rateLimiterConfig;
  }

  @Test
  public void sendWebformEmail_EN() throws Exception {
    mockSuccessfulSend();

    notificationId = webformService.sendWebformEmail(webform);

    Mockito.verify(notificationClient)
        .sendEmail(
            eq(appConfig.getWebform().getTemplateId()),
            eq(appConfig.getWebform().getEmailEn()),
            templateValueCaptor.capture(),
            any());

    verifyRateLimiterCall(1, webform.getClientIP());
    
    assertTrue(validateTemplateValues(webform, templateValueCaptor.getValue()));
    assertEquals(NOTIFICATION_ID, notificationId);
  }

  @Test
  public void sendWebformEmail_CY() throws Exception {
    webform.setLanguage(WebformDTO.WebformLanguage.CY);

    mockSuccessfulSend();

    notificationId = webformService.sendWebformEmail(webform);

    Mockito.verify(notificationClient)
        .sendEmail(
            eq(appConfig.getWebform().getTemplateId()),
            eq(appConfig.getWebform().getEmailCy()),
            templateValueCaptor.capture(),
            any());

    verifyRateLimiterCall(1, webform.getClientIP());
    
    assertTrue(validateTemplateValues(webform, templateValueCaptor.getValue()));
    assertEquals(NOTIFICATION_ID, notificationId);
  }

  @Test
  public void sendWebformWhenRateLimiterNotEnabled() throws Exception {
    mockSuccessfulSend();
    disableRateLimiter();

    notificationId = webformService.sendWebformEmail(webform);

    Mockito.verify(notificationClient)
        .sendEmail(
            eq(appConfig.getWebform().getTemplateId()),
            eq(appConfig.getWebform().getEmailEn()),
            templateValueCaptor.capture(),
            any());
    
    verifyRateLimiterNotCalled();

    assertTrue(validateTemplateValues(webform, templateValueCaptor.getValue()));
    assertEquals(NOTIFICATION_ID, notificationId);
  }

  public void sendWebformEmail_Error() throws Exception {
    mockFailedSend();
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
    assertTrue(e.getCause().getCause() instanceof NotificationClientException);
  }

  @Test
  public void shouldHandleWebformTimeoutException() throws Exception {
    simulateWebformCircuitBreakerTimeout();
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
    assertTrue(e.getCause() instanceof TimeoutException);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformCategoryNull() throws CTPException {

    webform.setCategory(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformRegionNull() throws CTPException {

    webform.setRegion(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformLanguageNull() throws CTPException {

    webform.setLanguage(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformNameNull() throws CTPException {

    webform.setName(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformDescriptionNull() throws CTPException {

    webform.setDescription(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformEmailNull() throws CTPException {

    webform.setEmail(null);
    webformService.sendWebformEmail(webform);
  }

  private boolean validateTemplateValues(WebformDTO webform, Map<String, String> personalisation) {
    Map<String, Object> result =
        Map.of(
            TEMPLATE_FULL_NAME, webform.getName(),
            TEMPLATE_EMAIL, webform.getEmail(),
            TEMPLATE_REGION, webform.getRegion().name(),
            TEMPLATE_CATEGORY, webform.getCategory().name(),
            TEMPLATE_DESCRIPTION, webform.getDescription());
    return result.equals(personalisation);
  }

  // --- helpers

  private void disableRateLimiter() {
    appConfig.setRateLimiter(rateLimiterConfig(false));
  }

  private void verifyRateLimiterCall(int numTimes, String clientIp) throws Exception {
    verify(rateLimiterClient, times(numTimes)).checkWebformRateLimit(eq(Domain.RH), eq(clientIp));
  }

  private void verifyRateLimiterNotCalled() throws Exception {
    verify(rateLimiterClient, never())
        .checkWebformRateLimit(any(), any());
  }

  private void simulateWebformCircuitBreaker() {
    doAnswer(
            new Answer<Object>() {
              @SuppressWarnings("unchecked")
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Supplier<Object> runner = (Supplier<Object>) args[0];
                Function<Throwable, Object> fallback = (Function<Throwable, Object>) args[1];

                try {
                  // execute the circuitBreaker.run first argument (the Supplier for the code you
                  // want to run)
                  return runner.get();
                } catch (Throwable t) {
                  // execute the circuitBreaker.run second argument (the fallback Function)
                  fallback.apply(t);
                }
                return null;
              }
            })
        .when(webformCircuitBreaker)
        .run(any(), any());
  }

  private void simulateWebformCircuitBreakerTimeout() {
    doAnswer(
            new Answer<Object>() {
              @SuppressWarnings("unchecked")
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Function<Throwable, Object> fallback = (Function<Throwable, Object>) args[1];
                fallback.apply(new TimeoutException());
                return null;
              }
            })
        .when(webformCircuitBreaker)
        .run(any(), any());
  }
}
