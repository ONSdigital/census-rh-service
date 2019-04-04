package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import java.sql.Timestamp;
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

  /**
   * To publish an Respondent Event message
   *
   * @param event as place marker, just send out the incoming CaseEvent at the moment.
   */
  public void sendEvent(CaseEvent event) {
    rabbitTemplate.convertAndSend(event);

    Header eventData =
        Header.builder()
            .type("SurveryLaunched")
            .source("ContactCentreAPI")
            .channel("cc")
            .dateTime(new Timestamp(System.currentTimeMillis()))
            .transactionId("c45de4dc-3c3b-11e9-b210-d663bd873pmbd93")
            .build();

    CaseEvent response = new CaseEvent();
    response.add("questionnaireId", "eoueoueoueuouueeopmb");
    response.add("caseId", "bbd55984-0dbf-4499-bfa7-0aa4228700e9");
    response.add("agentId", "cc_000351pmb");

    GenericCaseEvent caseEvent = new GenericCaseEvent(eventData, new Payload(response));

    surveyLaunchedRabbitTemplate.convertAndSend(caseEvent);
  }
}
