package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.config.EnableIntegration;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;

@Profile("mocked-connection-factory")
@Configuration
@EnableIntegration
public class EventReceiverConfiguration {

  /** Setup mock ConnectionFactory for SimpleMessageContainerListener */
  @Bean
  @Primary
  public ConnectionFactory connectionFactory() {

    Connection connection = mock(Connection.class);
    doAnswer(invocation -> mock(Channel.class)).when(connection).createChannel(anyBoolean());
    ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    when(connectionFactory.createConnection()).thenReturn(connection);
    return connectionFactory;
  }

  @Bean
  public AmqpAdmin amqpAdmin() {
    return mock(AmqpAdmin.class);
  }

  @Bean
  public CustomObjectMapper mapper() {
    return new CustomObjectMapper();
  }
}
