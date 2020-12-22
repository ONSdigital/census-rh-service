package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
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
  private CircuitBreaker webformCircuitBreaker;

  private AppConfig appConfig;

  @Autowired private RateLimiterClient rateLimiterClient;

  /**
   * Constructor for WebformServiceImpl
   *
   * @param notificationClient Gov.uk Notify service client
   * @param webformCircuitBreaker circuit breaker for calls to GOV.UK notify
   * @param envoyCircuitBreaker circuit breaker for calls to envoy rate limiter
   * @param appConfig centralised configuration properties
   */
  @Autowired
  public WebformServiceImpl(
      final NotificationClientApi notificationClient,
      final @Qualifier("webformCb") CircuitBreaker webformCircuitBreaker,
      final AppConfig appConfig) {
    this.notificationClient = notificationClient;
    this.webformCircuitBreaker = webformCircuitBreaker;
    this.appConfig = appConfig;
  }

  @Override
  public UUID sendWebformEmail(WebformDTO webform) throws CTPException {
    checkWebformRateLimit(webform.getClientIP());
    return doSendWebFormEmail(webform);
  }

  /**
   * Since it is possible the GOV.UK notify service could either fail or be slow, we use a circuit
   * breaker wrapper here to fail fast to prevent the user waiting for a failed response over a long
   * time, and also to protect the RHSvc (thread) resources from getting tied up.
   *
   * <p>If GOV.UK notify response is too slow an error response will be returned back to the caller.
   * If GOV.UK notify returns an error, or is down, an error response will go back to the caller,
   * and for repeated failures the circuit breaker will do it's usual fail-fast mechanism.
   *
   * @param webform webform DTO
   * @return the notification ID returned by the GOV.UK notify service.
   * @throws RuntimeException a wrapper around any error response exception, which could typically
   *     be a failure from GOV.UK Notify, or a circuit breaker timeout or fail-fast.
   */
  private UUID doSendWebFormEmail(WebformDTO webform) {
    return this.webformCircuitBreaker.run(
        () -> {
          SendEmailResponse response = send(webform);
          return response.getNotificationId();
        },
        throwable -> {
          String msg = throwable.getMessage();
          if (throwable instanceof TimeoutException) {
            int timeout = appConfig.getWebformCircuitBreaker().getTimeout();
            msg = "call timed out, took longer than " + timeout + " seconds to complete";
          }
          log.info("Send within circuit breaker failed: {}", msg);
          throw new RuntimeException(throwable);
        });
  }

  private SendEmailResponse send(WebformDTO webform) {
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
      return response;
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
