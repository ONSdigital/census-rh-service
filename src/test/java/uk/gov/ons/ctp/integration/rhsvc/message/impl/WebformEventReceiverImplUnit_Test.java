package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.WebformEvent;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.WebformEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.WebformServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class WebformEventReceiverImplUnit_Test {

  @Mock private WebformServiceImpl mockWebformService;

  @InjectMocks private WebformEventReceiverImpl target;

  @Test
  public void acceptWebformEvent() {
    WebformEvent event = FixtureHelper.loadPackageFixtures(WebformEvent[].class).get(0);
    target.acceptWebformEvent(event);
    verify(mockWebformService, times(1)).sendWebformEmail(event.getPayload().getWebform());
  }
}
