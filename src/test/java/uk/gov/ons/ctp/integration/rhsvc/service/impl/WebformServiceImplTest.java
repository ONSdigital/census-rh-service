package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Webform;

@RunWith(MockitoJUnitRunner.class)
public class WebformServiceImplTest {

  private static final String TRANSACTIONID = "fdc64299-1a08-49b1-af33-63b322a04e34";

  @Mock private EventPublisher eventPublisher;

  @InjectMocks WebformServiceImpl webformService;

  @Captor ArgumentCaptor<Webform> webformEventCaptor;

  @Test
  public void webformCapture() throws Exception {
    Webform webform = FixtureHelper.loadClassFixtures(Webform[].class).get(0);

    Mockito.when(
            eventPublisher.sendEvent(
                eq(EventType.WEB_FORM_REQUEST),
                eq(Source.RESPONDENT_HOME),
                eq(Channel.RH),
                webformEventCaptor.capture()))
        .thenReturn(TRANSACTIONID);

    String transactionId = webformService.sendWebformEvent(webform);

    Mockito.verify(eventPublisher)
        .sendEvent(
            eq(EventType.WEB_FORM_REQUEST),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            webformEventCaptor.capture());

    Webform event = webformEventCaptor.getValue();

    assertEquals(TRANSACTIONID, transactionId);
    assertEquals(webform, event);
  }
}
