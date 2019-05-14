package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.message.Header;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.message.UACEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.UACPayload;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

public class UacEventReceiverImplUnit_Test {

  private RespondentDataService mockRespondentDataService;
  private UACEventReceiverImpl target;

  @Before
  public void setUp() {

    target = new UACEventReceiverImpl();

    mockRespondentDataService = Mockito.mock(RespondentDataServiceImpl.class);
    target.setRespondentDataService(mockRespondentDataService);
  }

  @Test
  public void test_acceptUACEvent_success() throws CTPException {

    // Construct UACEvent
    UACEvent uacEventFixture = new UACEvent();
    UACPayload uacPayloadFixture = uacEventFixture.getPayload();
    UAC uacFixture = uacPayloadFixture.getUac();
    uacFixture.setUacHash("999999999");
    uacFixture.setActive("true");
    uacFixture.setQuestionnaireId("1110000009");
    uacFixture.setCaseType("H");
    uacFixture.setRegion("E");
    uacFixture.setCaseId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    uacFixture.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    Header headerFixture = new Header();
    headerFixture.setType("UAC_UPDATED");
    headerFixture.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    uacEventFixture.setEvent(headerFixture);

    // execution
    target.acceptUACEvent(uacEventFixture);

    // verification
    Mockito.verify(mockRespondentDataService).writeUAC(uacFixture);
  }
}
