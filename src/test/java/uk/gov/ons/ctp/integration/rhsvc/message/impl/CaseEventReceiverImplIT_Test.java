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
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Address;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Contact;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.CasePayload;
import uk.gov.ons.ctp.integration.rhsvc.message.Header;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

/** Spring Integration test of flow received from Response Management */
@ContextConfiguration("/caseEventReceiverImpl.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseEventReceiverImplIT_Test {

  @Autowired private CaseEventReceiverImpl receiver;
  @Autowired private SimpleMessageListenerContainer caseEventListenerContainer;
  @Autowired private RespondentDataServiceImpl respondentDataServiceImpl;

  /** Test the receiver flow */
  @Test
  public void caseEventFlowTest() throws Exception {

    String bucket = "case_bucket";

    // Construct CaseEvent
    CaseEvent caseEvent = new CaseEvent();
    CasePayload casePayload = caseEvent.getPayload();
    CollectionCase collectionCase = casePayload.getCollectionCase();
    Address address = collectionCase.getAddress();
    Contact contact = collectionCase.getContact();
    collectionCase.setId("900000000");
    collectionCase.setCaseRef("10000000010");
    collectionCase.setSurvey("Census");
    collectionCase.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    collectionCase.setState("actionable");
    collectionCase.setActionableFrom("2011-08-12T20:17:46.384Z");
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
    contact.setTitle("Ms");
    contact.setForename("jo");
    contact.setSurname("smith");
    contact.setEmail("me@example.com");
    contact.setTelNo("+447890000000");
    Header header = new Header();
    header.setType("CASE_UPDATED");
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    caseEvent.setEvent(header);

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

    // Tidy up by deleting the CaseEvent message from the case_bucket
    respondentDataServiceImpl.deleteJsonFromCloud("900000000", bucket);
  }
}
