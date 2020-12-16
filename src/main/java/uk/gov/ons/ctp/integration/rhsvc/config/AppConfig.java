package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import uk.gov.ons.ctp.common.config.CustomCircuitBreakerConfig;

/** Application Config bean */
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class AppConfig {
  private String collectionExerciseId;
  private SwaggerSettings swaggerSettings;
  private Logging logging;
  private QueueConfig queueConfig;
  private MessagingConfig messaging;
  private CustomCircuitBreakerConfig eventPublisherCircuitBreaker;
  private CustomCircuitBreakerConfig envoyLimiterCircuitBreaker;
  private CustomCircuitBreakerConfig webformCircuitBreaker;
  private RateLimiterConfig rateLimiter;
  private NotifyConfig notify;
  private WebformConfig webform;
}
