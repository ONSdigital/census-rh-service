package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import java.util.Date;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CasePayload;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

/** Spring Integration test of flow received from Response Management */
@SpringBootTest
@ContextConfiguration("/caseEventReceiverImpl.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("mocked-connection-factory")
public class CaseEventReceiverImplIT_Test {

  @Autowired private CaseEventReceiverImpl receiver;
  @Autowired private SimpleMessageListenerContainer caseEventListenerContainer;
  @MockBean private RespondentDataRepositoryImpl respondentDataRepo;

  @Before
  public void initMocks() {
    Mockito.reset(receiver);
  }

  /** Test the receiver flow */
  @Test
  public void caseEventFlowTest() throws Exception {

    CaseEvent caseEvent = createCaseEvent();

    // Construct message
    MessageProperties amqpMessageProperties = new MessageProperties();
    org.springframework.amqp.core.Message amqpMessage =
        new Jackson2JsonMessageConverter().toMessage(caseEvent, amqpMessageProperties);

    // Send message to container
    ChannelAwareMessageListener listener =
        (ChannelAwareMessageListener) caseEventListenerContainer.getMessageListener();
    final Channel rabbitChannel = mock(Channel.class);
    listener.onMessage(amqpMessage, rabbitChannel);

    // Capture and check Service Activator argument
    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    verify(receiver).acceptCaseEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(caseEvent.getPayload()));
  }

  @Test
  public void caseCaseReceivedWithoutMillisTest() throws Exception {

    // Create a case with a timestamp. Note that that the milliseconds are not specified
    CaseEvent caseEvent = createCaseEvent();
    String caseAsJson = new ObjectMapper().writeValueAsString(caseEvent);
    String caseWithoutMillis =
        caseAsJson.replaceAll("\"dateTime\":\"[^\"]*", "\"dateTime\":\"2011-08-12T20:17:46Z");
    assertTrue(caseWithoutMillis.contains("20:17:46Z"));

    // Send message to container
    ChannelAwareMessageListener listener =
        (ChannelAwareMessageListener) caseEventListenerContainer.getMessageListener();
    final Channel rabbitChannel = mock(Channel.class);
    listener.onMessage(
        new Message(caseWithoutMillis.getBytes(), new MessageProperties()), rabbitChannel);

    // Capture and check Service Activator argument
    ArgumentCaptor<CaseEvent> captur = ArgumentCaptor.forClass(CaseEvent.class);
    verify(receiver).acceptCaseEvent(captur.capture());
    assertTrue(captur.getValue().getPayload().equals(caseEvent.getPayload()));
  }

  private CaseEvent createCaseEvent() {
    CaseEvent caseEvent = new CaseEvent();
    CasePayload casePayload = caseEvent.getPayload();
    CollectionCase collectionCase = casePayload.getCollectionCase();
    collectionCase.setId("900000000");
    collectionCase.setCaseRef("10000000010");
    collectionCase.setSurvey("Census");
    collectionCase.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    collectionCase.setActionableFrom("2011-08-12T20:17:46.384Z");

    Address address = collectionCase.getAddress();
    address.setAddressLine1("1 main street");
    address.setAddressLine2("upper upperingham");
    address.setAddressLine3("");
    address.setTownName("upton");
    address.setPostcode("UP103UP");
    address.setRegion("E");
    address.setLatitude("50.863849");
    address.setLongitude("-1.229710");
    address.setUprn("XXXXXXXXXXXXX");
    address.setArid("XXXXX");
    address.setAddressType("CE");
    address.setEstabType("XXX");

    Contact contact = collectionCase.getContact();
    contact.setTitle("Ms");
    contact.setForename("jo");
    contact.setSurname("smith");
    contact.setTelNo("+447890000000");

    Header header = new Header();
    header.setType(EventType.CASE_UPDATED);
    header.setDateTime(new Date());
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    caseEvent.setEvent(header);
    return caseEvent;
  }
}
