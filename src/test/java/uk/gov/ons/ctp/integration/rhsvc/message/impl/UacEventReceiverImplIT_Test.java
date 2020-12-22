package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.util.Date;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.common.event.model.UACPayload;
import uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.UACEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, UacEventReceiverImplIT_Config.class})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("mocked-connection-factory")
public class UacEventReceiverImplIT_Test {

  @Autowired private UACEventReceiverImpl receiver;
  @Autowired private SimpleMessageListenerContainer uacEventListenerContainer;
  @MockBean private RespondentDataRepositoryImpl respondentDataRepo;

  @Before
  public void initMocks() {
    Mockito.reset(receiver);
  }

  @SneakyThrows
  private void uacEventFlow(EventType type) {
    UACEvent uacEvent = createUAC(RespondentHomeFixture.A_QID, type);

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
    verify(respondentDataRepo).writeUAC(any());
  }

  /** Test the receiver flow for UAC created */
  @Test
  public void uacCreatedEventFlow() {
    uacEventFlow(EventType.UAC_CREATED);
  }

  /** Test the receiver flow for UAC updated */
  @Test
  public void uacUpdatedEventFlow() {
    uacEventFlow(EventType.UAC_UPDATED);
  }

  @Test
  public void shouldFilterUacEventWithContinuationFormQid() throws Exception {

    UACEvent uacEvent = createUAC(RespondentHomeFixture.QID_12, EventType.UAC_UPDATED);

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
    verify(respondentDataRepo, never()).writeUAC(any());
  }

  @Test
  public void uacEventReceivedWithoutMillisecondsTest() throws Exception {

    // Create a UAC with a timestamp. Note that the milliseconds are not specified
    UACEvent uacEvent = createUAC(RespondentHomeFixture.A_QID, EventType.UAC_UPDATED);
    String uac = new ObjectMapper().writeValueAsString(uacEvent);
    String uacWithTimestamp =
        uac.replaceAll("\"dateTime\":\"[^\"]*", "\"dateTime\":\"2011-08-12T20:17:46Z");
    assertTrue(uacWithTimestamp.contains("20:17:46Z"));

    // Send message to container
    ChannelAwareMessageListener listener =
        (ChannelAwareMessageListener) uacEventListenerContainer.getMessageListener();
    final Channel rabbitChannel = mock(Channel.class);
    MessageProperties amqpMessageProperties = new MessageProperties();
    listener.onMessage(
        new Message(uacWithTimestamp.getBytes(), amqpMessageProperties), rabbitChannel);

    // Capture and check Service Activator argument
    ArgumentCaptor<UACEvent> captur = ArgumentCaptor.forClass(UACEvent.class);
    verify(receiver).acceptUACEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(uacEvent.getPayload()));
    verify(respondentDataRepo).writeUAC(any());
  }

  private UACEvent createUAC(String qid, EventType type) {
    // Construct UACEvent
    UACEvent uacEvent = new UACEvent();
    UACPayload uacPayload = uacEvent.getPayload();
    UAC uac = uacPayload.getUac();
    uac.setUacHash("999999999");
    uac.setActive("true");
    uac.setQuestionnaireId(qid);
    uac.setCaseId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    Header header = new Header();
    header.setType(type);
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    header.setDateTime(new Date());
    uacEvent.setEvent(header);
    return uacEvent;
  }
}
