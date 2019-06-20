package uk.gov.ons.ctp.integration.rhsvc;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import uk.gov.ons.ctp.common.error.RestExceptionHandler;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.cloud.FirestoreDataStore;

/** The 'main' entry point for the RHSvc SpringBoot Application. */
@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
@ImportResource("springintegration/main.xml")
public class RHSvcApplication {
  private static final Logger log = LoggerFactory.getLogger(RHSvcApplication.class);

  @Value("${queueconfig.event-exchange}")
  private String eventExchange;

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

  /**
   * Bean used to publish asynchronous event messages
   *
   * @param connectionFactory RabbitMQ connection settings and strategies
   * @return the event publisher
   */
  @Bean
  public EventPublisher eventPublisher(final ConnectionFactory connectionFactory) {
    final var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.setExchange(eventExchange);
    template.setChannelTransacted(true);
    return new EventPublisher(template);
  }

  /**
   * Bean used to map exceptions for endpoints
   *
   * @return the service client
   */
  @Bean
  public RestExceptionHandler restExceptionHandler() {
    return new RestExceptionHandler();
  }

  /**
   * Connect to Google Firestore.
   *
   * @return a Firestore connection.
   */
  @Bean
  public Firestore firestore() {
    String googleCredentials = System.getenv(FirestoreDataStore.FIRESTORE_CREDENTIALS_ENV_NAME);
    String googleProjectName = System.getenv(FirestoreDataStore.FIRESTORE_PROJECT_ENV_NAME);
    log.info(
        "Connecting to Firestore project '{}' using credentials at '{}'",
        googleProjectName,
        googleCredentials);

    return FirestoreOptions.getDefaultInstance().getService();
  }
}
