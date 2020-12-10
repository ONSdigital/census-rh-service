package uk.gov.ons.ctp.integration.rhsvc.config;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.config.StatelessRetryOperationsInterceptorFactoryBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
import uk.gov.ons.ctp.integration.rhsvc.config.MessagingConfig.ContainerConfig;

/** Integration configuration for inbound events. */
@Configuration
public class InboundEventIntegrationConfig {

  private AppConfig appConfig;

  /**
   * Constructor for InboundEventIntegrationConfig
   *
   * @param appConfig centralised configuration
   */
  public InboundEventIntegrationConfig(final AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  /**
   * Configure a backoff for what happens when rabbitMQ goes down. This allows us to do less
   * thrashing than the default when there is a rabbit problem thus reducing load on the system.
   *
   * @return backoff object
   */
  @Bean
  public BackOff rabbitDownBackOff() {
    MessagingConfig messaging = appConfig.getMessaging();
    BackoffConfig recoveryBackoff = messaging.getRecoveryBackoff();
    ExponentialBackOff backoff = new ExponentialBackOff();
    backoff.setMaxInterval(recoveryBackoff.getMax());
    backoff.setMultiplier(recoveryBackoff.getMultiplier());
    backoff.setInitialInterval(recoveryBackoff.getInitial());
    return backoff;
  }

  /**
   * Configure the retry behaviour for when a UAC, Case message throws a RuntimeException while it
   * is being processed in our event processing code.
   *
   * <p>Note that this does not apply to when Rabbit goes down, just problems with the subsequent
   * processing throwing an unchecked exception (of the type that Spring might emit).
   *
   * <p>Note also that the retry policy is configured with "maximum attempts" so for instance if
   * "conMaxAttempts" is 3 , then there will be at most 2 retries after the initial attempt.
   *
   * @return retry template
   */
  @Bean
  public RetryTemplate uacCaseRetryTemplate() {
    ContainerConfig messaging = appConfig.getMessaging().getUacCaseListener();
    BackoffConfig processingBackoff = messaging.getProcessingBackoff();
    ExponentialBackOffPolicy backoffPolicy = new ExponentialBackOffPolicy();
    backoffPolicy.setMaxInterval(processingBackoff.getMax());
    backoffPolicy.setMultiplier(processingBackoff.getMultiplier());
    backoffPolicy.setInitialInterval(processingBackoff.getInitial());
    RetryTemplate template = new RetryTemplate();
    template.setBackOffPolicy(backoffPolicy);
    RetryPolicy retryPolicy = new CTPRetryPolicy(messaging.getConMaxAttempts());
    template.setRetryPolicy(retryPolicy);
    return template;
  }

  /**
   * Create advice bean for the UAC, Case listener, containing the retry configuration, and specify
   * that if the retry fails, to place the message in the DLQ.
   *
   * @param retryTemplate uacCaseRetryTemplate
   * @return retry advice for the listener.
   */
  @Bean
  public StatelessRetryOperationsInterceptorFactoryBean uacCaseRetryAdvice(
      RetryTemplate uacCaseRetryTemplate) {
    var advice = new StatelessRetryOperationsInterceptorFactoryBean();
    advice.setMessageRecoverer(new RejectAndDontRequeueRecoverer());
    advice.setRetryOperations(uacCaseRetryTemplate);
    return advice;
  }

  /**
   * Configure a listener container for the Case events. This listens for Case events on the rabbit
   * case queue.
   *
   * @param connectionFactory connection factory
   * @param eventRetryAdvice retry advice
   * @param rabbitDownBackOff backoff for when rabbit problems occur
   * @return listener container for the Case events.
   */
  @Bean
  public SimpleMessageListenerContainer caseEventListenerContainer(
      ConnectionFactory connectionFactory,
      @Qualifier("uacCaseRetryAdvice")
          StatelessRetryOperationsInterceptorFactoryBean eventRetryAdvice,
      BackOff rabbitDownBackOff) {
    return makeListenerContainer(
        connectionFactory,
        eventRetryAdvice,
        rabbitDownBackOff,
        appConfig.getMessaging().getUacCaseListener(),
        appConfig.getQueueConfig().getCaseQueue());
  }

  /**
   * Configure a listener container for the UAC events. This listens for UAC events on the rabbit
   * UAC queue.
   *
   * @param connectionFactory connection factory
   * @param eventRetryAdvice retry advice
   * @param rabbitDownBackOff backoff for when rabbit problems occur
   * @return listener container for the UAC events.
   */
  @Bean
  public SimpleMessageListenerContainer uacEventListenerContainer(
      ConnectionFactory connectionFactory,
      @Qualifier("uacCaseRetryAdvice")
          StatelessRetryOperationsInterceptorFactoryBean eventRetryAdvice,
      BackOff rabbitDownBackOff) {
    return makeListenerContainer(
        connectionFactory,
        eventRetryAdvice,
        rabbitDownBackOff,
        appConfig.getMessaging().getUacCaseListener(),
        appConfig.getQueueConfig().getUacQueue());
  }

  private SimpleMessageListenerContainer makeListenerContainer(
      ConnectionFactory connectionFactory,
      StatelessRetryOperationsInterceptorFactoryBean eventRetryAdvice,
      BackOff rabbitDownBackOff,
      ContainerConfig containerConfig,
      String queueName) {
    MessagingConfig messaging = appConfig.getMessaging();
    SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer();
    listener.setConnectionFactory(connectionFactory);
    listener.setMismatchedQueuesFatal(messaging.isMismatchedQueuesFatal());
    listener.setQueueNames(queueName);
    listener.setAdviceChain(eventRetryAdvice.getObject());
    listener.setConcurrentConsumers(containerConfig.getConsumingThreads());
    listener.setPrefetchCount(containerConfig.getPrefetchCount());
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

  /**
   * Create a message converter specifically for CASE JSON events to be converted to CaseEvent java
   * objects.
   *
   * @param customObjectMapper object mapper
   * @return JSON converted
   */
  @Bean
  public Jackson2JsonMessageConverter caseJsonMessageConverter(
      CustomObjectMapper customObjectMapper) {
    return jsonMessageConverter(customObjectMapper, CaseEvent.class);
  }

  /**
   * Create a message converter specifically for UAC JSON events to be converted to UACEvent java
   * objects.
   *
   * @param customObjectMapper object mapper
   * @return JSON converted
   */
  @Bean
  public Jackson2JsonMessageConverter uacJsonMessageConverter(
      CustomObjectMapper customObjectMapper) {
    return jsonMessageConverter(customObjectMapper, UACEvent.class);
  }

  private Jackson2JsonMessageConverter jsonMessageConverter(
      CustomObjectMapper customObjectMapper, Class<?> defaultType) {
    Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(customObjectMapper);
    DefaultClassMapper mapper =
        new DefaultClassMapper() {
          @Override
          public Class<?> toClass(MessageProperties properties) {
            return defaultType;
          }
        };
    converter.setClassMapper(mapper);
    return converter;
  }

  /** @return channel for accepting case events */
  @Bean
  public MessageChannel acceptCaseEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(CaseEvent.class);
    return channel;
  }

  /** @return channel for accepting UAC events */
  @Bean
  public MessageChannel acceptUACEvent() {
    DirectChannel channel = new DirectChannel();
    channel.setDatatypes(UACEvent.class);
    return channel;
  }
}
