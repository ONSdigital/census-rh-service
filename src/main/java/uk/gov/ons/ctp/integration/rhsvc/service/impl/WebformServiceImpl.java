package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
  @Autowired private CircuitBreaker circuitBreaker;

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
  public String sendWebformEvent(Webform webform) {

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
        circuitBreaker.run(
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
                log.with("error", throwable.getMessage()).error(throwable, "Rate limiter failure for webform");
              }
              return null;
            });
    if (limitException != null) {
      throw limitException;
    }
  }
}
