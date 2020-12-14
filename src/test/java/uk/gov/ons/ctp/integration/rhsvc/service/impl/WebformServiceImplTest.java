package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import javax.validation.ConstraintViolationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Webform;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.NotifyConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.RateLimiterConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.WebformConfig;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    classes = {WebformServiceImpl.class, AppConfig.class, ValidationAutoConfiguration.class})
public class WebformServiceImplTest {

  private static final String TRANSACTIONID = "fdc64299-1a08-49b1-af33-63b322a04e34";
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

  private Webform webform;
  @Autowired AppConfig appConfig;

  @MockBean private EventPublisher eventPublisher;

  @MockBean private NotificationClientApi notificationClient;

  @MockBean private RateLimiterClient rateLimiterClient;

  @Autowired WebformService webformService;

  @Captor ArgumentCaptor<Webform> webformEventCaptor;
  @Captor ArgumentCaptor<Map<String, String>> templateValueCaptor;

  @Before
  public void setUp() {

    webform = FixtureHelper.loadClassFixtures(Webform[].class).get(0);

    NotifyConfig notifyConfig = new NotifyConfig();
    notifyConfig.setApiKey(UUID.randomUUID().toString());
    notifyConfig.setBaseUrl("https://api.notifications.service.gov.uk");
    appConfig.setNotify(notifyConfig);

    WebformConfig webformConfig = new WebformConfig();
    webformConfig.setTemplateId(UUID.randomUUID().toString());
    webformConfig.setEmailEn("english-delivered@notifications.service.gov.uk");
    webformConfig.setEmailCy("welsh-delivered@notifications.service.gov.uk");
    appConfig.setWebform(webformConfig);

    appConfig.setRateLimiter(rateLimiterConfig(true));
  }

  private RateLimiterConfig rateLimiterConfig(boolean enabled) {
    RateLimiterConfig rateLimiterConfig = new RateLimiterConfig();
    rateLimiterConfig.setEnabled(enabled);
    return rateLimiterConfig;
  }

  @Test
  public void sendWebformEvent() throws Exception {

    Mockito.when(
            eventPublisher.sendEvent(
                eq(EventType.WEB_FORM_REQUEST),
                eq(Source.RESPONDENT_HOME),
                eq(Channel.RH),
                webformEventCaptor.capture()))
        .thenReturn(TRANSACTIONID);

    String transactionId = webformService.sendWebformEvent(webform);

    verifyRateLimiterCall(1, webform.getClientIP());

    Mockito.verify(eventPublisher)
        .sendEvent(
            eq(EventType.WEB_FORM_REQUEST),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            webformEventCaptor.capture());

    Webform event = webformEventCaptor.getValue();

    assertEquals(TRANSACTIONID, transactionId);
    assertEquals(webform, event);
  }

  @Test
  public void sendWebformEmail_EN() throws Exception {

    Mockito.when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenReturn(new SendEmailResponse(SEND_EMAIL_RESPONSE_JSON));

    webformService.sendWebformEmail(webform);

    Mockito.verify(notificationClient)
        .sendEmail(
            eq(appConfig.getWebform().getTemplateId()),
            eq(appConfig.getWebform().getEmailEn()),
            templateValueCaptor.capture(),
            any());

    assertTrue(validateTemplateValues(webform, templateValueCaptor.getValue()));
  }

  @Test
  public void sendWebformEmail_CY() throws Exception {

    webform.setLanguage(Webform.WebformLanguage.CY);

    Mockito.when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenReturn(new SendEmailResponse(SEND_EMAIL_RESPONSE_JSON));

    webformService.sendWebformEmail(webform);

    Mockito.verify(notificationClient)
        .sendEmail(
            eq(appConfig.getWebform().getTemplateId()),
            eq(appConfig.getWebform().getEmailCy()),
            templateValueCaptor.capture(),
            any());

    assertTrue(validateTemplateValues(webform, templateValueCaptor.getValue()));
  }

  @Test
  public void sendWebformWhenRateLimiterNotEnabled() throws Exception {
    Mockito.when(
            eventPublisher.sendEvent(
                eq(EventType.WEB_FORM_REQUEST),
                eq(Source.RESPONDENT_HOME),
                eq(Channel.RH),
                webformEventCaptor.capture()))
        .thenReturn(TRANSACTIONID);

    // Disable rate limiter
    appConfig.setRateLimiter(rateLimiterConfig(false));

    String transactionId = webformService.sendWebformEvent(webform);

    Mockito.verify(eventPublisher)
        .sendEvent(
            eq(EventType.WEB_FORM_REQUEST),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            webformEventCaptor.capture());

    Webform event = webformEventCaptor.getValue();

    assertEquals(TRANSACTIONID, transactionId);
    assertEquals(webform, event);

    verifyRateLimiterNotCalled();
  }

  @Test(expected = RuntimeException.class)
  public void sendWebformEmail_Error() throws Exception {

    Mockito.when(notificationClient.sendEmail(any(), any(), any(), any()))
        .thenThrow(new NotificationClientException("GOV.UK Notify service failure"));

    webformService.sendWebformEmail(webform);
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

  private boolean validateTemplateValues(Webform webform, Map<String, String> personalisation) {
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

  private void verifyRateLimiterCall(int numTimes, String clientIp) throws Exception {
    verify(rateLimiterClient, times(numTimes)).checkWebformRateLimit(eq(Domain.RH), eq(clientIp));
  }

  private void verifyRateLimiterNotCalled() throws Exception {
    verify(rateLimiterClient, never())
        .checkFulfilmentRateLimit(any(), any(), any(), any(), any(), any());
  }
}
