package uk.gov.ons.ctp.integration.rhsvc.event.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.common.event.model.GenericEvent;
import uk.gov.ons.ctp.integration.rhsvc.event.RespondentEventPublisher;

/** Service implementation responsible for publishing an event from the Respondent service. */
@MessageEndpoint
public class RespondentEventPublisherImpl implements RespondentEventPublisher {

  @Qualifier("eventRabbitTemplate")
  @Autowired
  private RabbitTemplate rabbitTemplate;

  public void sendEvent(GenericEvent event) {
    rabbitTemplate.convertAndSend(event);
  }
}
