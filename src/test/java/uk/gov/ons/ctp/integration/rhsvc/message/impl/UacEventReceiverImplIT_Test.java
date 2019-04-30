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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.message.Header;
import uk.gov.ons.ctp.integration.rhsvc.message.UACEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.UACPayload;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@ContextConfiguration("/uacEventReceiverImpl.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class UacEventReceiverImplIT_Test {

  @Autowired private UACEventReceiverImpl receiver;
  @Autowired private SimpleMessageListenerContainer uacEventListenerContainer;
  @Autowired private RespondentDataServiceImpl respondentDataServiceImpl;

  /** Test the receiver flow */
  @Test
  public void uacEventFlowTest() throws Exception {

    String bucket = "uac_bucket";

    // Construct UACEvent
    UACEvent uacEvent = new UACEvent();
    UACPayload uacPayload = uacEvent.getPayload();
    UAC uac = uacPayload.getUac();
    uac.setUacHash("999999999");
    uac.setActive("true");
    uac.setQuestionnaireId("1110000009");
    uac.setCaseType("H");
    uac.setRegion("E");
    uac.setCaseId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    uac.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    Header header = new Header();
    header.setType("UAC_UPDATED");
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    uacEvent.setEvent(header);

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

    // Tidy up by deleting the CaseEvent message from the case_bucket
    respondentDataServiceImpl.deleteJsonFromCloud("999999999", bucket);
  }
}
