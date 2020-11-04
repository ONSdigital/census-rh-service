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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.Webform;
import uk.gov.ons.ctp.common.event.model.WebformEvent;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.service.WebformService;

@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = {AppConfig.class, WebformEventReceiverImplIT_Config.class})
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("mocked-connection-factory")
public class WebformEventReceiverImplIT_Test {

  @Autowired private SimpleMessageListenerContainer webformEventListenerContainer;
  @MockBean private WebformService webformService;

  /** Test the receiver flow */
  @Test
  public void webformEventFlowTest() throws Exception {

    WebformEvent webformEvent = FixtureHelper.loadPackageFixtures(WebformEvent[].class).get(0);

    // Construct message
    MessageProperties amqpMessageProperties = new MessageProperties();
    org.springframework.amqp.core.Message amqpMessage =
        new Jackson2JsonMessageConverter().toMessage(webformEvent, amqpMessageProperties);

    // Send message to container
    ChannelAwareMessageListener listener =
        (ChannelAwareMessageListener) webformEventListenerContainer.getMessageListener();
    final Channel rabbitChannel = mock(Channel.class);
    listener.onMessage(amqpMessage, rabbitChannel);

    // Capture and check Service Activator argument
    ArgumentCaptor<Webform> captur = ArgumentCaptor.forClass(Webform.class);
    verify(webformService).sendWebformEmail(captur.capture());
    assertTrue(captur.getValue().equals(webformEvent.getPayload().getWebform()));
  }
}
