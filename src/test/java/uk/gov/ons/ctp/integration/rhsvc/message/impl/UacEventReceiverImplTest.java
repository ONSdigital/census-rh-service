package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rabbitmq.client.Channel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/** Spring Integration test of flow received from Response Management */
@ContextConfiguration("/uacEventReceiverImpl.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class UacEventReceiverImplTest {

  @Autowired private UACEventReceiver receiver;
  @Autowired private SimpleMessageListenerContainer uacEventListenerContainer;

  /** Test the receiver flow */
  @Test
  public void uacEventFlowTest() throws Exception {

    // Construct UACEvent
    UACEvent uacEvent = new UACEvent();
    UACPayload uacPayload = uacEvent.getPayload();
    UAC uac = uacPayload.getUac();
    uac.setUacHash("72C84BA99D77EE766E9468A0DE36433A44888E5DEC4AFB84F8019777800B7364");
    uac.setActive("true");
    uac.setQuestionnaireId("1110000009");
    uac.setCaseType("H");
    uac.setRegion("E");
    uac.setCaseId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    uac.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");

    // Construct message
    MessageProperties amqpMessageProperties = new MessageProperties();
    org.springframework.amqp.core.Message amqpMessage =
        new Jackson2JsonMessageConverter().toMessage(uacEvent, amqpMessageProperties);

    // Send message to container
    ChannelAwareMessageListener listener =
        (ChannelAwareMessageListener) uacEventListenerContainer.getMessageListener();
    final Channel rabbitChannel = mock(Channel.class);
    listener.onMessage(amqpMessage, rabbitChannel);

    // Capture and check Service Activator argument
    ArgumentCaptor<UACEvent> captur = ArgumentCaptor.forClass(UACEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(uacEvent.getPayload()));
  }
}
