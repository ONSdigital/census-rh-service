package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.integration.rhsvc.message.GenericEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.message.SurveyLaunchedEvent;

/** Service implementation responsible for publishing an event from the Respondent service. */
@MessageEndpoint
public class RespondentEventPublisherImpl implements RespondentEventPublisher {

  @Qualifier("respondentEventRabbitTemplate")
  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Qualifier("surveyLaunchedRabbitTemplate")
  @Autowired
  private RabbitTemplate surveyLaunchedRabbitTemplate;

  /**
   * To publish an Respondent Event message
   *
   * @param event as place marker, just send out the incoming CaseEvent at the moment.
   */
  public void sendEvent(GenericEvent event) {
    rabbitTemplate.convertAndSend(event);
  }

  public void sendEvent(SurveyLaunchedEvent event) {
    surveyLaunchedRabbitTemplate.convertAndSend(event);
  }
}
