package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.InboundEventIntegrationConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.UACEventReceiverImpl;

@Profile("mocked-connection-factory")
@Configuration
@Import({InboundEventIntegrationConfig.class, EventReceiverConfiguration.class})
public class UacEventReceiverImplIT_Config {

  /** Spy on Service Activator Message End point */
  @Bean
  public UACEventReceiverImpl uacEventReceiver(AppConfig appConfig) {
    UACEventReceiverImpl receiver = new UACEventReceiverImpl();
    ReflectionTestUtils.setField(receiver, "appConfig", appConfig);
    return Mockito.spy(receiver);
  }
}
