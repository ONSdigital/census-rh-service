package uk.gov.ons.ctp.integration.rhsvc.message;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;

/** Service implementation responsible for publishing an event from the Respondent service. */
@MessageEndpoint
public class RespondentEventPublisher {

  @Qualifier("respondentEventRabbitTemplate")
  @Autowired
  private RabbitTemplate rabbitTemplate;

  /**
   * To publish an Respondent Event message
   *
   * @param event as place marker, just send out the incoming CaseEvent at the moment.
   */
  public void sendEvent(CaseEvent event) {
    rabbitTemplate.convertAndSend(event);
  }
}
