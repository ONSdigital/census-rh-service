package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.message.Header;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Address;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Contact;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.CasePayload;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

public class CaseEventReceiverImplUnit_Test {

  private RespondentDataService mockRespondentDataService;
  private CaseEventReceiverImpl target;

  @Before
  public void setUp() {

    target = new CaseEventReceiverImpl();

    mockRespondentDataService = Mockito.mock(RespondentDataServiceImpl.class);
    target.setRespondentDataService(mockRespondentDataService);
  }

  @Test
  public void test_acceptCaseEvent_success() throws CTPException {

    // Construct CaseEvent
    CaseEvent caseEventFixture = new CaseEvent();
    CasePayload casePayloadFixture = caseEventFixture.getPayload();
    CollectionCase collectionCaseFixture = casePayloadFixture.getCollectionCase();
    Address addressFixture = collectionCaseFixture.getAddress();
    Contact contactFixture = collectionCaseFixture.getContact();
    collectionCaseFixture.setId("900000000");
    collectionCaseFixture.setCaseRef("10000000010");
    collectionCaseFixture.setSurvey("Census");
    collectionCaseFixture.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    collectionCaseFixture.setState("actionable");
    collectionCaseFixture.setActionableFrom("2011-08-12T20:17:46.384Z");
    addressFixture.setAddressLine1("1 main street");
    addressFixture.setAddressLine2("upper upperingham");
    addressFixture.setAddressLine3("");
    addressFixture.setTownName("upton");
    addressFixture.setPostcode("UP103UP");
    addressFixture.setRegion("E");
    addressFixture.setLatitude("50.863849");
    addressFixture.setLongitude("-1.229710");
    addressFixture.setUprn("XXXXXXXXXXXXX");
    addressFixture.setArid("XXXXX");
    addressFixture.setAddressType("CE");
    addressFixture.setEstabType("XXX");
    contactFixture.setTitle("Ms");
    contactFixture.setForename("jo");
    contactFixture.setSurname("smith");
    contactFixture.setEmail("me@example.com");
    contactFixture.setTelNo("+447890000000");
    Header headerFixture = new Header();
    headerFixture.setType("CASE_UPDATED");
    headerFixture.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    caseEventFixture.setEvent(headerFixture);

    // execution
    target.acceptCaseEvent(caseEventFixture);

    // verification
    Mockito.verify(mockRespondentDataService).writeCollectionCase(collectionCaseFixture);
  }
}
