package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Webform;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;
import uk.gov.service.notify.NotificationClientApi;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

/** This is a service layer class, which performs RH business level logic for webform requests. */
@Service
public class WebformServiceImpl implements WebformService {

  private static final Logger log = LoggerFactory.getLogger(WebformServiceImpl.class);

  private static final String TEMPLATE_FULL_NAME = "respondent_full_name";
  private static final String TEMPLATE_EMAIL = "respondent_email";
  private static final String TEMPLATE_REGION = "respondent_region";
  private static final String TEMPLATE_CATEGORY = "respondent_category";
  private static final String TEMPLATE_DESCRIPTION = "respondent_description";

  private EventPublisher eventPublisher;

  private NotificationClientApi notificationClient;

  private AppConfig appConfig;
  @Autowired private RateLimiterClient rateLimiterClient;

  /**
   * Constructor for WebformServiceImpl
   *
   * @param eventPublisher service for publication of events to RabbitMQ
   * @param notificationClient Gov.uk Notify service client
   * @param appConfig centralised configuration properties
   */
  @Autowired
  public WebformServiceImpl(
      final EventPublisher eventPublisher,
      final NotificationClientApi notificationClient,
      final AppConfig appConfig) {
    this.eventPublisher = eventPublisher;
    this.notificationClient = notificationClient;
    this.appConfig = appConfig;
  }

  @Override
  public String sendWebformEvent(Webform webform) throws CTPException {

    checkWebformRateLimit(webform.getClientIP());

    String transactionId =
        eventPublisher.sendEvent(
            EventType.WEB_FORM_REQUEST, Source.RESPONDENT_HOME, Channel.RH, webform);
    return transactionId;
  }

  @Override
  public void sendWebformEmail(Webform webform) {

    String emailToAddress =
        Webform.WebformLanguage.CY.equals(webform.getLanguage())
            ? appConfig.getWebform().getEmailCy()
            : appConfig.getWebform().getEmailEn();
    String reference = UUID.randomUUID().toString();

    try {
      SendEmailResponse response =
          notificationClient.sendEmail(
              appConfig.getWebform().getTemplateId(),
              emailToAddress,
              templateValues(webform),
              reference);
      log.with("reference", reference)
          .with("notificationId", response.getNotificationId().toString())
          .with("templateId", response.getTemplateId().toString())
          .with("templateVersion", response.getTemplateVersion())
          .debug("Gov Notify sendEmail response recieved");
    } catch (NotificationClientException ex) {
      log.with("reference", reference)
          .with("webform", webform)
          .with("emailToAddress", emailToAddress)
          .with("status", ex.getHttpResult())
          .with("message", ex.getMessage())
          .error(ex, "Gov Notify sendEmail error");
      throw new RuntimeException("Gov Notify sendEmail error", ex);
    }
  }

  private Map<String, String> templateValues(Webform webform) {
    Map<String, String> personalisation = new HashMap<>();
    personalisation.put(TEMPLATE_FULL_NAME, webform.getName());
    personalisation.put(TEMPLATE_EMAIL, webform.getEmail());
    personalisation.put(TEMPLATE_REGION, webform.getRegion().name());
    personalisation.put(TEMPLATE_CATEGORY, webform.getCategory().name());
    personalisation.put(TEMPLATE_DESCRIPTION, webform.getDescription());
    return personalisation;
  }

  private void checkWebformRateLimit(String ipAddress) throws CTPException {
    if (appConfig.getRateLimiter().isEnabled()) {
      log.with("ipAddress", ipAddress).debug("Invoking rate limiter for webform");
      // Do rest call to rate limiter
      rateLimiterClient.checkWebformRateLimit(Domain.RH, ipAddress);
    } else {
      log.info("Rate limiter client is disabled");
    }
  }
}
