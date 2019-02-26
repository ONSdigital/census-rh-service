package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEventReceiver;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;

@Configuration
public class CaseEventReceiverConfiguration {

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

  /** Mock injected by CaseEventReceiver */
  @Bean
  public RespondentEventPublisher publisher() {
    return mock(RespondentEventPublisher.class);
  }

  /** Spy on Service Activator Message End point */
  @Bean
  public CaseEventReceiver reciever() {
    return Mockito.spy(new CaseEventReceiverImpl());
  }
}
