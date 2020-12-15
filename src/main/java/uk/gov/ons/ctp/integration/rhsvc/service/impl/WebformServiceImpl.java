package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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
  private CircuitBreaker envoyCircuitBreaker;

  private AppConfig appConfig;
  @Autowired private RateLimiterClient rateLimiterClient;

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
      final @Qualifier("webformCb") CircuitBreaker webformCircuitBreaker,
      final @Qualifier("envoyLimiterCb") CircuitBreaker envoyCircuitBreaker,
      final AppConfig appConfig) {
    this.notificationClient = notificationClient;
    this.webformCircuitBreaker = webformCircuitBreaker;
    this.envoyCircuitBreaker = envoyCircuitBreaker;
    this.appConfig = appConfig;
  }

  @Override
  public UUID sendWebformEmail(WebformDTO webform) {
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

  private void checkWebformRateLimit(String ipAddress) {
    if (appConfig.getRateLimiter().isEnabled()) {
      log.with("ipAddress", ipAddress).debug("Recording rate-limiting");
      doCheckWebformRateLimit(ipAddress);
    } else {
      log.info("Rate limiter client is disabled");
    }
  }

  /*
   * Call the rate limiter within a circuit-breaker, thus protecting the RHSvc
   * functionality from the unlikely event that that the rate limiter service is failing.
   *
   * If the limit is breached, a ResponseStatusException with HTTP 429 will be thrown.
   */
  private void doCheckWebformRateLimit(String ipAddress) {
    ResponseStatusException limitException =
        envoyCircuitBreaker.run(
            () -> {
              try {
                rateLimiterClient.checkWebformRateLimit(Domain.RH, ipAddress);
                return null;
              } catch (CTPException e) {
                // we should get here if the rate-limiter is failing or not communicating
                // ... wrap and rethrow to be handled by the circuit-breaker
                throw new RuntimeException(e);
              } catch (ResponseStatusException e) {
                // we have got a 429 but don't rethrow it otherwise this will count against
                // the circuit-breaker accounting, so instead we return it to later throw
                // outside the circuit-breaker mechanism.
                return e;
              }
            },
            throwable -> {
              // This is the Function for the circuitBreaker.run second parameter, which is called
              // when an exception is thrown from the first Supplier parameter (above), including
              // as part of the processing of being in the circuit-breaker OPEN state.
              //
              // It is OK to carry on, since it is better to tolerate limiter error than fail
              // operation, however by getting here, the circuit-breaker has counted the failure,
              // or we are in circuit-breaker OPEN state.
              if (throwable instanceof CallNotPermittedException) {
                log.info("Circuit breaker is OPEN calling rate limiter for webform");
              } else {
                log.with("error", throwable.getMessage())
                    .error(throwable, "Rate limiter failure for webform");
              }
              return null;
            });
    if (limitException != null) {
      throw limitException;
    }
  }
}
