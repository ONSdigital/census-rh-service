package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Header;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.event.model.UACEvent;
import uk.gov.ons.ctp.common.event.model.UACPayload;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.UACEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.repository.impl.RespondentDataRepositoryImpl;

public class UacEventReceiverImplUnit_Test {

  private RespondentDataRepository mockRespondentDataRepo;
  private UACEventReceiverImpl target;

  @Before
  public void setUp() {

    target = new UACEventReceiverImpl();

    mockRespondentDataRepo = Mockito.mock(RespondentDataRepositoryImpl.class);
    target.setRespondentDataRepo(mockRespondentDataRepo);
  }

  @Test
  public void test_acceptUACEvent_success() throws CTPException {

    // Construct UACEvent
    UACEvent uacEventFixture = new UACEvent();
    UACPayload uacPayloadFixture = uacEventFixture.getPayload();
    UAC uacFixture = uacPayloadFixture.getUac();

    Header headerFixture = new Header();
    headerFixture.setType(EventType.UAC_UPDATED);
    headerFixture.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    uacEventFixture.setEvent(headerFixture);

    // execution
    target.acceptUACEvent(uacEventFixture);

    // verification
    Mockito.verify(mockRespondentDataRepo).writeUAC(uacFixture);
  }
}
