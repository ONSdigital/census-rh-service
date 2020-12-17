package uk.gov.ons.ctp.integration.rhsvc;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.ons.ctp.common.config.CustomCircuitBreakerConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;

/** Configuration for all circuit breakers used in RHSvc. */
@Configuration
public class RHSvcCircuitBreakerConfig {
  private static final Logger log = LoggerFactory.getLogger(RHSvcCircuitBreakerConfig.class);

  @Bean("eventPublisherCbFactory")
  public Resilience4JCircuitBreakerFactory eventPublisherCircuitBreakerFactory(
      AppConfig appConfig) {
    CustomCircuitBreakerConfig config = appConfig.getEventPublisherCircuitBreaker();
    log.info("Event Publisher Circuit breaker configuration: {}", config);
    return createCbFactory(config);
  }

  @Bean("envoyLimiterCbFactory")
  public Resilience4JCircuitBreakerFactory envoyLimiterCircuitBreakerFactory(AppConfig appConfig) {
    CustomCircuitBreakerConfig config = appConfig.getEnvoyLimiterCircuitBreaker();
    log.info("Envoy Limiter Circuit breaker configuration: {}", config);
    return createCbFactory(config);
  }

  @Bean("webformCbFactory")
  public Resilience4JCircuitBreakerFactory webformCircuitBreakerFactory(AppConfig appConfig) {
    CustomCircuitBreakerConfig config = appConfig.getWebformCircuitBreaker();
    log.info("Webform Circuit breaker configuration: {}", config);
    return createCbFactory(config);
  }

  @Bean("webformCb")
  public CircuitBreaker webformCircuitBreaker(
      @Qualifier("webformCbFactory") Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
    return circuitBreakerFactory.create("webformCircuitBreaker");
  }

  @Bean("envoyLimiterCb")
  public CircuitBreaker envoyLimiterCircuitBreaker(
      @Qualifier("envoyLimiterCbFactory") Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
    return circuitBreakerFactory.create("rateLimiterCircuitBreaker");
  }

  private Resilience4JCircuitBreakerFactory createCbFactory(CustomCircuitBreakerConfig config) {
    Customizer<Resilience4JCircuitBreakerFactory> customiser =
        config.defaultCircuitBreakerCustomiser();
    Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory();
    customiser.customize(factory);
    return factory;
  }
}
