package uk.gov.ons.ctp.integration.rhsvc.config;

import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptorFactoryBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import uk.gov.ons.ctp.common.retry.CTPRetryPolicy;

/** Integration configuration for inbound events. */
@Configuration
public class InboundEventIntegrationConfig {
  @Autowired private AppConfig appConfig;

  @Bean
  public BackOff rabbitDownBackOff() {
    return new ExponentialBackOff();
  }

  @Bean
  public RetryTemplate retryTemplate() {
    MessagingConfig messaging = appConfig.getMessaging();
    ExponentialBackOffPolicy backoffPolicy = new ExponentialBackOffPolicy();
    backoffPolicy.setMaxInterval(messaging.getBackoffMax());
    backoffPolicy.setMultiplier(messaging.getBackoffMultiplier());
    backoffPolicy.setInitialInterval(messaging.getBackoffInitial());
    RetryTemplate template = new RetryTemplate();
    template.setBackOffPolicy(backoffPolicy);
    RetryPolicy retryPolicy = new CTPRetryPolicy(messaging.getConMaxAttempts());
    template.setRetryPolicy(retryPolicy);
    return template;
  }

  /**
   * Create advice bean.
   *
   * <p>NOTE: Not expected to store to a transactional resource such as a database, if need rollback
   * on a transactional datastore for instance look into using
   * StatefulRetryOperationsInterceptorFactoryBean
   *
   * @param retryTemplate retryTemplate
   * @return retry advice
   */
  @Bean
  public StatelessRetryOperationsInterceptorFactoryBean eventRetryAdvice(
      RetryTemplate retryTemplate) {
    var advice = new StatelessRetryOperationsInterceptorFactoryBean();
    advice.setMessageRecoverer(new RejectAndDontRequeueRecoverer());
    advice.setRetryOperations(retryTemplate);
    return advice;
  }

  @Bean
  public SimpleMessageListenerContainer caseEventListenerContainer(
      ConnectionFactory connectionFactory,
      StatelessRetryOperationsInterceptorFactoryBean eventRetryAdvice,
      BackOff rabbitDownBackOff) {
    return makeListenerContainer(
        connectionFactory,
        eventRetryAdvice,
        rabbitDownBackOff,
        appConfig.getQueueConfig().getCaseQueue());
  }

  @Bean
  public SimpleMessageListenerContainer uacEventListenerContainer(
      ConnectionFactory connectionFactory,
      StatelessRetryOperationsInterceptorFactoryBean eventRetryAdvice,
      BackOff rabbitDownBackOff) {
    return makeListenerContainer(
        connectionFactory,
        eventRetryAdvice,
        rabbitDownBackOff,
        appConfig.getQueueConfig().getUacQueue());
  }

  private SimpleMessageListenerContainer makeListenerContainer(
      ConnectionFactory connectionFactory,
      StatelessRetryOperationsInterceptorFactoryBean eventRetryAdvice,
      BackOff rabbitDownBackOff,
      String queueName) {
    MessagingConfig messaging = appConfig.getMessaging();
    SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer();
    listener.setConnectionFactory(connectionFactory);
    listener.setMismatchedQueuesFatal(messaging.isMismatchedQueuesFatal());
    listener.setQueueNames(queueName);
    listener.setAdviceChain(eventRetryAdvice.getObject());
    listener.setConcurrentConsumers(messaging.getConsumingThreads());
    listener.setPrefetchCount(messaging.getPrefetchCount());
    listener.setRecoveryBackOff(rabbitDownBackOff);
    return listener;
  }

  @Bean
  public SimpleMessageConverter simpleMessageConverter() {
    return new SimpleMessageConverter();
  }

  @Bean
  public MessageChannel caseEventDlqChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel uacEventDlqChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel jsonCaseEventMessage() {
    return new DirectChannel();
  }

  @Bean
  public MessageChannel jsonUacEventMessage() {
    return new DirectChannel();
  }
}
