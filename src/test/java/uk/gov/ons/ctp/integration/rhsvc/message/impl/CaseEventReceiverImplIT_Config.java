package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import uk.gov.ons.ctp.integration.rhsvc.config.InboundEventIntegrationConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;

@Profile("mocked-connection-factory")
@Configuration
@Import({InboundEventIntegrationConfig.class, EventReceiverConfiguration.class})
public class CaseEventReceiverImplIT_Config {

  /** Spy on Service Activator Message End point */
  @Bean
  public CaseEventReceiverImpl caseEventReceiver() {
    return Mockito.spy(new CaseEventReceiverImpl());
  }
}
