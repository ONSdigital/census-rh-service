package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class CustomMessageListener {

  private static final Logger log = LoggerFactory.getLogger(CustomMessageListener.class);

  @RabbitListener(queues = "Generic.Gateway")
  public void receiveMessage(final Message message) {

    Object eventType = null;

    log.info("Received message as generic: {}", message.toString());

    // eventType = message.getMessageProperties().getHeaders().get("type");

    // log.info("The value of eventType is: " + eventType.toString());
  }

  // @RabbitListener(queues = MessagingApplication.QUEUE_SPECIFIC_NAME)
  //  public void receiveMessage(final CustomMessage customMessage) {
  //     log.info("Received message as specific class: {}", customMessage.toString());
  // }
}
