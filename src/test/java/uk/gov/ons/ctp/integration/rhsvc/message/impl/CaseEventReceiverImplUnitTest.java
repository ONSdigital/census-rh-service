package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.message.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.CasePayload;
import uk.gov.ons.ctp.integration.rhsvc.service.impl.RespondentDataServiceImpl;

// import org.junit.Ignore;
// import org.junit.runner.RunWith;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;
// import org.powermock.api.mockito.PowerMockito;
// import org.powermock.core.classloader.annotations.PowerMockIgnore;
// import org.powermock.core.classloader.annotations.PrepareForTest;
// import org.powermock.modules.junit4.PowerMockRunner;
// import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

// @RunWith(PowerMockRunner.class)
// @PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*"})
// @PrepareForTest(fullyQualifiedNames = "uk.gov.ons.ctp.integration.rhsvc.message.*")
@ContextConfiguration("/caseEventReceiverImplUnit.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class CaseEventReceiverImplUnitTest {

  //  @Mock private RespondentDataService mockRespondentDataService;
  //
  @Autowired private CaseEventReceiverImpl receiver;
  @Autowired private RespondentDataServiceImpl respondentDataServiceImpl;
  //  @Before
  //  public void setUp() {
  //
  ////    target = new CaseEventReceiverImpl();
  //    //    mockRespondentDataService = PowerMockito.mock(RespondentDataService.class);
  //    //    MockitoAnnotations.initMocks(this);
  //
  //  }

  //  @PrepareForTest(CaseEventReceiverImpl.class)
  @Test
  public void test_acceptCaseEvent_success() {

    //    CaseEvent caseEventMock = PowerMockito.mock(CaseEvent.class);
    //    CasePayload casePayloadMock = PowerMockito.mock(CasePayload.class);
    //    CollectionCase collectionCaseMock = PowerMockito.mock(CollectionCase.class);
    //
    //    PowerMockito.when(caseEventMock.getPayload()).thenReturn(casePayloadMock);
    //    PowerMockito.when(caseEventMock.getPayload().getCollectionCase())
    //        .thenReturn(collectionCaseMock);
    //

    // Construct CaseEvent
    CaseEvent caseEventFixture = new CaseEvent();
    CasePayload casePayloadFixture = caseEventFixture.getPayload();
    CollectionCase collectionCaseFixture = casePayloadFixture.getCollectionCase();
    collectionCaseFixture.setId("900000000");
    collectionCaseFixture.setCaseRef("10000000010");
    collectionCaseFixture.setSurvey("Census");
    collectionCaseFixture.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    collectionCaseFixture.setSampleUnitRef("");
    collectionCaseFixture.setAddress("");
    collectionCaseFixture.setState("actionable");
    collectionCaseFixture.setActionableFrom("2011-08-12T20:17:46.384Z");

    receiver.acceptCaseEvent(caseEventFixture);
  }
}
