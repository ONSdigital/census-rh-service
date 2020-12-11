package uk.gov.ons.ctp.integration.rhsvc;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.godaddy.logging.LoggingConfigs;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.SpringRabbitEventSender;
import uk.gov.ons.ctp.common.event.persistence.FirestoreEventPersistence;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
import uk.gov.ons.ctp.common.rest.RestClient;
import uk.gov.ons.ctp.common.rest.RestClientConfig;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientApi;

/** The 'main' entry point for the RHSvc SpringBoot Application. */
@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration", "uk.gov.ons.ctp.common"})
@ImportResource("springintegration/broker.xml")
public class RHSvcApplication {
  private static final Logger log = LoggerFactory.getLogger(RHSvcApplication.class);

  @Value("${management.metrics.export.stackdriver.project-id}")
  private String stackdriverProjectId;

  @Value("${management.metrics.export.stackdriver.enabled}")
  private boolean stackdriverEnabled;

  @Value("${management.metrics.export.stackdriver.step}")
  private String stackdriverStep;

  @Autowired private AppConfig appConfig;

  /**
   * The main entry point for this application.
   *
   * @param args runtime command line args
   */
  public static void main(final String[] args) {
    SpringApplication.run(RHSvcApplication.class, args);
  }

  @EnableWebSecurity
  public static class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      // Post requests to the service only work with csrf disabled!
      http.csrf().disable();
    }
  }

  @Bean
  public EventPublisher eventPublisher(
      final RabbitTemplate rabbitTemplate,
      final FirestoreEventPersistence eventPersistence,
      @Qualifier("eventPublisherCbFactory")
          Resilience4JCircuitBreakerFactory circuitBreakerFactory) {
    EventSender sender = new SpringRabbitEventSender(rabbitTemplate);
    CircuitBreaker circuitBreaker = circuitBreakerFactory.create("eventSendCircuitBreaker");
    return EventPublisher.createWithEventPersistence(sender, eventPersistence, circuitBreaker);
  }

  @Bean
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setExchange("events");
    template.setChannelTransacted(true);
    return template;
  }

  @Bean
  public RateLimiterClient rateLimiterClient() {
    RestClientConfig clientConfig = appConfig.getRateLimiter().getRestClientConfig();
    log.info("Rate Limiter configuration: {}", appConfig.getRateLimiter());
    var statusMapping = clientErrorMapping();
    RestClient restClient =
        new RestClient(clientConfig, statusMapping, HttpStatus.INTERNAL_SERVER_ERROR);
    return new RateLimiterClient(restClient);
  }

  private Map<HttpStatus, HttpStatus> clientErrorMapping() {
    Map<HttpStatus, HttpStatus> mapping = new HashMap<>();
    EnumSet.allOf(HttpStatus.class).stream()
        .filter(s -> s.is4xxClientError())
        .forEach(s -> mapping.put(s, s));
    return mapping;
  }

  /**
   * Custom Object Mapper
   *
   * @return a customer object mapper
   */
  @Bean
  @Primary
  public CustomObjectMapper customObjectMapper() {
    return new CustomObjectMapper();
  }

  @Value("#{new Boolean('${logging.useJson}')}")
  private boolean useJsonLogging;

  @PostConstruct
  public void initJsonLogging() {
    if (useJsonLogging) {
      LoggingConfigs.setCurrent(LoggingConfigs.getCurrent().useJson());
    }
  }

  @Bean
  StackdriverConfig stackdriverConfig() {
    return new StackdriverConfig() {
      @Override
      public Duration step() {
        return Duration.parse(stackdriverStep);
      }

      @Override
      public boolean enabled() {
        return stackdriverEnabled;
      }

      @Override
      public String projectId() {
        return stackdriverProjectId;
      }

      @Override
      public String get(String key) {
        return null;
      }
    };
  }

  @Bean
  public MeterFilter meterFilter() {
    return new MeterFilter() {
      @Override
      public MeterFilterReply accept(Meter.Id id) {
        // RM use this to remove Rabbit clutter from the metrics as they have alternate means of
        // monitoring it
        // We will probable want to experiment with removing this to see what value we get from
        // rabbit metrics
        // a) once we have Grafana setup, and b) once we try out micrometer in a perf environment
        if (id.getName().startsWith("rabbitmq")) {
          return MeterFilterReply.DENY;
        }
        return MeterFilterReply.NEUTRAL;
      }
    };
  }

  @Bean
  StackdriverMeterRegistry meterRegistry(StackdriverConfig stackdriverConfig) {

    StackdriverMeterRegistry.builder(stackdriverConfig).build();
    return StackdriverMeterRegistry.builder(stackdriverConfig).build();
  }

  @Bean
  public NotificationClientApi notificationClient() {

    return new NotificationClient(
        appConfig.getNotify().getApiKey(), appConfig.getNotify().getBaseUrl());
  }
}
