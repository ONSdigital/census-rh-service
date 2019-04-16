package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;

/** Service implementation responsible for publishing an event from the Respondent service. */
@MessageEndpoint
public class RespondentEventPublisherImpl implements RespondentEventPublisher {

  @Qualifier("respondentEventRabbitTemplate")
  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Qualifier("surveyLaunchedRabbitTemplate")
  @Autowired
  private RabbitTemplate surveyLaunchedRabbitTemplate;

  public RespondentEventPublisherImpl() {
    System.out.println("PMB in constructor");
  }
  
  /**
   * To publish an Respondent Event message
   *
   * @param event as place marker, just send out the incoming CaseEvent at the moment.
   */
  public void sendEvent(CaseEvent event) {
    rabbitTemplate.convertAndSend(event);
  }

  public void sendSurveyLaunchedEvent(GenericCaseEvent event) {
    surveyLaunchedRabbitTemplate.convertAndSend(event);
  }
}
