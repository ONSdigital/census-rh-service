package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.config.CustomCircuitBreakerConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.NotifyConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.WebformConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    classes = {WebformServiceImpl.class, AppConfig.class, ValidationAutoConfiguration.class})
public class WebformServiceImplTest {

  private static final String SEND_EMAIL_RESPONSE_JSON =
      "{\"content\" : {"
          + "\"body\" : null,"
          + "\"from_email\" : \"census.2021@test.gov.uk\","
          + "\"subject\" : \"COMPLAINT\"},"
          + "\"id\" : \"8db6313a-d4e3-47a1-8d0e-ddd30c86e878\","
          + "\"reference\" : \"88e4a66a-1a8d-4b8e-802d-b8a9dd10528d\","
          + "\"scheduled_for\" :null,"
          + "\"template\" : {"
          + "\"id\" : \"457d8d8c-bdcb-4875-8f2f-88030643ad13\","
          + "\"uri\" : null,"
          + "\"version\" : \"1\" },"
          + "\"uri\" : null }";

  private static final String TEMPLATE_FULL_NAME = "respondent_full_name";
  private static final String TEMPLATE_EMAIL = "respondent_email";
  private static final String TEMPLATE_REGION = "respondent_region";
  private static final String TEMPLATE_CATEGORY = "respondent_category";
  private static final String TEMPLATE_DESCRIPTION = "respondent_description";

  private static final UUID NOTIFICATION_ID =
      UUID.fromString("8db6313a-d4e3-47a1-8d0e-ddd30c86e878");

  private UUID notificationId;
  private WebformDTO webform;
  @Autowired AppConfig appConfig;

  @MockBean private NotificationClientApi notificationClient;

  @Mock private CustomCircuitBreakerConfig cbConfig;

  @MockBean(name = "webformCb")
  private CircuitBreaker circuitBreaker;

  @Autowired WebformService webformService;

  @Captor ArgumentCaptor<WebformDTO> webformEventCaptor;
  @Captor ArgumentCaptor<Map<String, String>> templateValueCaptor;

  @Before
  public void setUp() {

    webform = FixtureHelper.loadClassFixtures(WebformDTO[].class).get(0);

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
    simulateCircuitBreaker();
  }

  @Test
  public void sendWebformEmail_EN() throws Exception {

    when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenReturn(new SendEmailResponse(SEND_EMAIL_RESPONSE_JSON));

    notificationId = webformService.sendWebformEmail(webform);

    Mockito.verify(notificationClient)
        .sendEmail(
            eq(appConfig.getWebform().getTemplateId()),
            eq(appConfig.getWebform().getEmailEn()),
            templateValueCaptor.capture(),
            any());

    assertTrue(validateTemplateValues(webform, templateValueCaptor.getValue()));
    assertEquals(NOTIFICATION_ID, notificationId);
  }

  @Test
  public void sendWebformEmail_CY() throws Exception {

    webform.setLanguage(WebformDTO.WebformLanguage.CY);

    when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenReturn(new SendEmailResponse(SEND_EMAIL_RESPONSE_JSON));

    notificationId = webformService.sendWebformEmail(webform);

    Mockito.verify(notificationClient)
        .sendEmail(
            eq(appConfig.getWebform().getTemplateId()),
            eq(appConfig.getWebform().getEmailCy()),
            templateValueCaptor.capture(),
            any());

    assertTrue(validateTemplateValues(webform, templateValueCaptor.getValue()));
    assertEquals(NOTIFICATION_ID, notificationId);
  }

  @Test
  public void sendWebformEmail_Error() throws Exception {
    when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenThrow(new NotificationClientException("GOV.UK Notify service failure"));

    RuntimeException e =
        assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
    assertTrue(e.getCause().getCause() instanceof NotificationClientException);
  }

  @Test
  public void shouldHandleTimeoutException() throws Exception {
    simulateCircuitBreakerTimeout();
    RuntimeException e =
        assertThrows(RuntimeException.class, () -> webformService.sendWebformEmail(webform));
    assertTrue(e.getCause() instanceof TimeoutException);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformCategoryNull() {

    webform.setCategory(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformRegionNull() {

    webform.setRegion(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformLanguageNull() {

    webform.setLanguage(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformNameNull() {

    webform.setName(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformDescriptionNull() {

    webform.setDescription(null);
    webformService.sendWebformEmail(webform);
  }

  @Test(expected = ConstraintViolationException.class)
  public void webformEmailNull() {

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

  private void simulateCircuitBreaker() {
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
        .when(circuitBreaker)
        .run(any(), any());
  }

  private void simulateCircuitBreakerTimeout() {
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
        .when(circuitBreaker)
        .run(any(), any());
  }
}
