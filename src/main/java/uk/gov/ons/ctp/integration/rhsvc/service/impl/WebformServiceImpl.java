package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.representation.WebformDTO;
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

  private NotificationClientApi notificationClient;
  private CircuitBreaker circuitBreaker;

  private AppConfig appConfig;

  /**
   * Constructor for WebformServiceImpl
   *
   * @param notificationClient Gov.uk Notify service client
   * @param circuitBreaker circuit breaker
   * @param appConfig centralised configuration properties
   */
  @Autowired
  public WebformServiceImpl(
      final NotificationClientApi notificationClient,
      final @Qualifier("webformCb") CircuitBreaker circuitBreaker,
      final AppConfig appConfig) {
    this.notificationClient = notificationClient;
    this.circuitBreaker = circuitBreaker;
    this.appConfig = appConfig;
  }

  @Override
  public UUID sendWebformEmail(WebformDTO webform) {
    return this.circuitBreaker.run(
        () -> {
          return send(webform);
        },
        throwable -> {
          log.debug("{}", throwable.getMessage());
          throw new RuntimeException(throwable);
        });
  }

  private UUID send(WebformDTO webform) {
    String emailToAddress =
        WebformDTO.WebformLanguage.CY.equals(webform.getLanguage())
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
          .with("notificationId", response.getNotificationId())
          .with("templateId", response.getTemplateId())
          .with("templateVersion", response.getTemplateVersion())
          .debug("Gov Notify sendEmail response received");
      return response.getNotificationId();
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

  private Map<String, String> templateValues(WebformDTO webform) {
    Map<String, String> personalisation = new HashMap<>();
    personalisation.put(TEMPLATE_FULL_NAME, webform.getName());
    personalisation.put(TEMPLATE_EMAIL, webform.getEmail());
    personalisation.put(TEMPLATE_REGION, webform.getRegion().name());
    personalisation.put(TEMPLATE_CATEGORY, webform.getCategory().name());
    personalisation.put(TEMPLATE_DESCRIPTION, webform.getDescription());
    return personalisation;
  }
}
