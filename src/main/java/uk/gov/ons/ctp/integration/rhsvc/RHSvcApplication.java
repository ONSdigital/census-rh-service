package uk.gov.ons.ctp.integration.rhsvc;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;

/** The 'main' entry point for the RHSvc SpringBoot Application. */
@SpringBootApplication
@IntegrationComponentScan("uk.gov.ons.ctp.integration")
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
@ImportResource("springintegration/main.xml")
public class RHSvcApplication {

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
  public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
    final var rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
    return rabbitTemplate;
  }

  @Bean
  public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
    return new Jackson2JsonMessageConverter();
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
}
