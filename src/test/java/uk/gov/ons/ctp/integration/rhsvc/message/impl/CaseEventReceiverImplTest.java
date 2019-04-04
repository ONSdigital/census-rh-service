package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// import uk.gov.ons.ctp.integration.rhsvc.message.GenericEventReceiver;

/** Spring Integration test of flow received from Response Management */
@ContextConfiguration("/caseEventReceiverImpl.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseEventReceiverImplTest {

  @Autowired private SimpleMessageListenerContainer container;

  //  @Autowired private GenericEventReceiver receiver;
  @Autowired private CaseEventReceiver receiver;

  /** Test the receiver flow */
  @Test
  public void CaseEventFlowTest() throws Exception {

    // Construct payload
    CaseEvent caseEvent = new CaseEvent();
    CasePayload casePayload = caseEvent.getPayload();
    CollectionCase collectionCase = casePayload.getCollectionCase();
    collectionCase.setId("bbd55984-0dbf-4499-bfa7-0aa4228700e9");

    //    //    CaseEvent payload = new CaseEvent();
    //    payload.add("uac", "lf5g7mbftjwn");
    //    payload.add("addressLine1", "Office for national Statistics");
    //    payload.add("addressLine2", "Segensworth Road");
    //
    //    // Construct message
    //    MessageProperties amqpMessageProperties = new MessageProperties();
    //    org.springframework.amqp.core.Message amqpMessage =
    //        new Jackson2JsonMessageConverter().toMessage(payload, amqpMessageProperties);
    //
    //    // Send message to container
    //    ChannelAwareMessageListener listener =
    //        (ChannelAwareMessageListener) container.getMessageListener();
    //    final Channel rabbitChannel = mock(Channel.class);
    //    listener.onMessage(amqpMessage, rabbitChannel);
    //
    //    // Capture and check Service Activator argument
    //    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    //    //    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    //    verify(receiver).acceptCaseEvent(captur.capture());
    //    assertTrue(captur.getValue().getProperties().equals(payload.getProperties()));
  }
}
