package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// import static org.junit.Assert.assertTrue;
// import static org.mockito.Mockito.verify;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.mockito.ArgumentCaptor;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
// import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
// import uk.gov.ons.ctp.integration.rhsvc.message.CaseEventReceiver;

/** Spring Integration test of flow received from Response Management */
@ContextConfiguration("/caseEventReceiverImpl.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseEventReceiverImplTest {
  @Autowired private SimpleMessageListenerContainer container;

  //  SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

  @Autowired private CaseEventReceiver receiver;

  /** Test the receiver flow */
  @Test
  public void caseEventFlowTest() throws Exception {

    //    //    CaseEvent payload = new CaseEvent();
    //    payload.add("uac", "lf5g7mbftjwn");
    //    payload.add("addressLine1", "Office for national Statistics");
    //    payload.add("addressLine2", "Segensworth Road");

    // System.out.Println("Can I see this message?");

    // Construct CaseEvent
    CaseEvent caseEvent = new CaseEvent();
    CasePayload casePayload = caseEvent.getPayload();
    CollectionCase collectionCase = casePayload.getCollectionCase();
    collectionCase.setId("bbd55984-0dbf-4499-bfa7-0aa4228700e9");
    collectionCase.setCaseRef("10000000010");
    collectionCase.setSurvey("Census");
    collectionCase.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    collectionCase.setSampleUnitRef("");
    collectionCase.setAddress("");
    collectionCase.setState("actionable");
    collectionCase.setActionableFrom("2011-08-12T20:17:46.384Z");

    //    // Construct message
    //    MessageProperties amqpMessageProperties = new MessageProperties();
    //    org.springframework.amqp.core.Message amqpMessage =
    //        new Jackson2JsonMessageConverter().toMessage(payload, amqpMessageProperties);

    // Construct message
    MessageProperties amqpMessageProperties = new MessageProperties();
    org.springframework.amqp.core.Message amqpMessage =
        new Jackson2JsonMessageConverter().toMessage(caseEvent, amqpMessageProperties);

    //    // Send message to container
    //    ChannelAwareMessageListener listener =
    //        (ChannelAwareMessageListener) container.getMessageListener();
    //    final Channel rabbitChannel = mock(Channel.class);
    //    listener.onMessage(amqpMessage, rabbitChannel);

    // Send message to container
    ChannelAwareMessageListener listener =
        (ChannelAwareMessageListener) container.getMessageListener();
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
