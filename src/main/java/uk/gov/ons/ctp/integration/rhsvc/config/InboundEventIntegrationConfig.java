package uk.gov.ons.ctp.integration.rhsvc.config;

import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptorFactoryBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.common.jackson.CustomObjectMapper;
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
  public AmqpInboundChannelAdapter caseEventInboundAmqp(
      @Qualifier("caseEventListenerContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("caseJsonMessageConverter") MessageConverter msgConverter,
      @Qualifier("acceptCaseEvent") MessageChannel outputChannel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setMessageConverter(msgConverter);
    adapter.setOutputChannel(outputChannel);
    return adapter;
  }

  @Bean
  public AmqpInboundChannelAdapter uacEventInboundAmqp(
      @Qualifier("uacEventListenerContainer") SimpleMessageListenerContainer listenerContainer,
      @Qualifier("uacJsonMessageConverter") MessageConverter msgConverter,
      @Qualifier("acceptUACEvent") MessageChannel outputChannel) {
    AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
    adapter.setMessageConverter(msgConverter);
    adapter.setOutputChannel(outputChannel);
    return adapter;
  }

  @Bean
  public Jackson2JsonMessageConverter caseJsonMessageConverter(
      CustomObjectMapper customObjectMapper) {
    return jsonMessageConverter(customObjectMapper, CaseEvent.class);
  }

  @Bean
  public Jackson2JsonMessageConverter uacJsonMessageConverter(
      CustomObjectMapper customObjectMapper) {
    return jsonMessageConverter(customObjectMapper, UACEvent.class);
  }

  private Jackson2JsonMessageConverter jsonMessageConverter(
      CustomObjectMapper customObjectMapper, Class<?> defaultType) {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(customObjectMapper);
    DefaultClassMapper mapper = new DefaultClassMapper();
    mapper.setDefaultType(defaultType);
    mapper.setTrustedPackages("*");
    converter.setClassMapper(mapper);
    return converter;
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
  public MessageChannel acceptCaseEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(CaseEvent.class);
    return channel;
  }

  @Bean
  public MessageChannel acceptUACEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(UACEvent.class);
    return channel;
  }
}
