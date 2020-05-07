package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.model.AddressLevel;
import uk.gov.ons.ctp.common.model.AddressType;
import uk.gov.ons.ctp.common.model.CaseType;
import uk.gov.ons.ctp.common.model.EstabType;
import uk.gov.ons.ctp.common.model.FormType;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACLinkRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

/** Unit tests of the Unique Access Code Service */
public class UniqueAccessCodeServiceImplTest {

  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";
  private static final String UPRN = "305634838282";
  
  @InjectMocks private UniqueAccessCodeServiceImpl uacSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  private List<UAC> uac;
  
  private List<CollectionCase> collectionCase;
  CollectionCase hhCase;
  CollectionCase hiCase1;
  CollectionCase hiCase2;

  private UACLinkRequestDTO linkRequest;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    
    this.uac = FixtureHelper.loadClassFixtures(UAC[].class);
    
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.hhCase = collectionCase.get(0);
    this.hiCase1 = collectionCase.get(1);
    this.hiCase2 = collectionCase.get(2);

    this.linkRequest = FixtureHelper.loadClassFixtures(UACLinkRequestDTO[].class).get(0);
  }

  /** Test request for claim object where UAC and Case found */
  @Test
  public void getUniqueAccessCodeDataUACAndCaseFound() throws Exception {

    ArgumentCaptor<RespondentAuthenticatedResponse> payloadCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedResponse.class);

    UAC uacTest = uac.get(0);
    CollectionCase caseTest = collectionCase.get(0);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCollectionCase(CASE_ID)).thenReturn(Optional.of(caseTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.RESPONDENT_AUTHENTICATED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(CaseStatus.OK, uacDTO.getCaseStatus());
    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(caseTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertEquals(uacTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(uacTest.getRegion(), uacDTO.getRegion());
    assertEquals(uacTest.getFormType(), uacDTO.getFormType());

    assertEquals(caseTest.getAddress().getAddressLine1(), uacDTO.getAddress().getAddressLine1());
    assertEquals(caseTest.getAddress().getAddressLine2(), uacDTO.getAddress().getAddressLine2());
    assertEquals(caseTest.getAddress().getAddressLine3(), uacDTO.getAddress().getAddressLine3());
    assertEquals(caseTest.getAddress().getTownName(), uacDTO.getAddress().getTownName());
    assertEquals(caseTest.getAddress().getPostcode(), uacDTO.getAddress().getPostcode());
    assertEquals(
        caseTest.getAddress().getUprn(), Long.toString(uacDTO.getAddress().getUprn().getValue()));

    RespondentAuthenticatedResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQuestionnaireId(), payload.getQuestionnaireId());
  }

  /** Test request for claim object where UAC found with caseID, CollectionCase not found */
  @Test
  public void getUniqueAccessCodeDataUACFoundWithCaseID() throws Exception {

    ArgumentCaptor<RespondentAuthenticatedResponse> payloadCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedResponse.class);

    UAC uacTest = uac.get(0);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCollectionCase(CASE_ID)).thenReturn(Optional.empty());

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.RESPONDENT_AUTHENTICATED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(CaseStatus.NOT_FOUND, uacDTO.getCaseStatus());
    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(uacTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertEquals(uacTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(uacTest.getRegion(), uacDTO.getRegion());

    assertEquals(null, uacDTO.getAddress());

    RespondentAuthenticatedResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQuestionnaireId(), payload.getQuestionnaireId());
  }

  /** Test request for claim object where UAC found without caseID */
  @Test
  public void getUniqueAccessCodeDataUACFoundWithoutCaseId() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(CaseStatus.UNKNOWN, uacDTO.getCaseStatus());
    assertEquals(null, uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(uacTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertEquals(uacTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(uacTest.getRegion(), uacDTO.getRegion());

    assertEquals(null, uacDTO.getAddress());
  }

  /** Test request for claim object where UAC not found */
  @Test
  public void getUniqueAccessCodeDataUACNotFound() throws Exception {

    when(dataRepo.readUAC(UAC_HASH))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND));

    boolean exceptionThrown = false;
    try {
      uacSvc.getAndAuthenticateUAC(UAC_HASH);
    } catch (CTPException e) {
      exceptionThrown = true;
    }

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());

    assertTrue(exceptionThrown);
  }

  // Happy path test for linking when the UAC links to an existing case. No new case is created
  @Test
  public void linkUAC_toExistingCase_noHICaseCreated() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    List<CollectionCase> cases = Stream.of(hiCase1, hiCase2, hhCase).collect(Collectors.toList());
    when(dataRepo.readCollectionCasesByUprn(eq(linkRequest.getUprn().asString())))
        .thenReturn(cases);

    // Build request object. Mostly empty as we will be using the existing case
    UACLinkRequestDTO request = new UACLinkRequestDTO();
    request.setAddressLine1("AL1");
    request.setAddressLine1("AL2");
    request.setAddressLine1("AL3");
    request.setAddressLine1("town");
    request.setRegion("x");
    request.setPostcode("PC");
    request.setUprn(new UniquePropertyReferenceNumber(UPRN));
    request.setEstabType(EstabType.RESIDENTIAL_CARAVAN.name());
    request.setAddressType(AddressType.HH.name());
    
    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    grabRepoWriteCollectionCaseValues(0);

    verifyUACUpdated(UAC_HASH, hhCase.getId());

    VerifyQuestionnaireLinkedEventSent(uacTest.getQuestionnaireId(), hhCase.getId(), null);
    
    verifyRespondentAuthenticatedEventSent(uacTest.getQuestionnaireId(), hhCase.getId());

    verifyTotalNumberEventsSent(2);
    
    verifyLinkingResult(uniqueAccessCodeDTO, hhCase.getId(), CaseType.HH, uacTest, hhCase.getAddress());
  }

  // Happy path test for linking when the UAC links to an existing case. No new case is created
  @Test
  public void linkUAC_toExistingCase_HICaseCreated() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    uacTest.setCaseType(CaseType.HH.name());
    uacTest.setFormType(FormType.I.name());
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    List<CollectionCase> cases = Stream.of(hiCase1, hiCase2, hhCase).collect(Collectors.toList());
    when(dataRepo.readCollectionCasesByUprn(eq(linkRequest.getUprn().asString())))
        .thenReturn(cases);

    // Build request object. Mostly empty as we will be using the existing case
    UACLinkRequestDTO request = new UACLinkRequestDTO();
    request.setAddressLine1("AL1");
    request.setAddressLine1("AL2");
    request.setAddressLine1("AL3");
    request.setAddressLine1("town");
    request.setRegion("x");
    request.setPostcode("PC");
    request.setUprn(new UniquePropertyReferenceNumber(UPRN));
    request.setEstabType(EstabType.RESIDENTIAL_CARAVAN.name());
    request.setAddressType(AddressType.HH.name());
    
    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // Verify that new HI case has been created
    List<CollectionCase> newCases = grabRepoWriteCollectionCaseValues(1);
    CollectionCase newHiCase = newCases.get(0);
//PMB    validateCase(newHiCase, CaseType.HI, uacTest, expectedAddress);

    verifyUACUpdated(UAC_HASH, newHiCase.getId());

    VerifyQuestionnaireLinkedEventSent(uacTest.getQuestionnaireId(), hhCase.getId(), newHiCase.getId());
    
    verifyRespondentAuthenticatedEventSent(uacTest.getQuestionnaireId(), newHiCase.getId());

    verifyTotalNumberEventsSent(2);
    
    verifyLinkingResult(uniqueAccessCodeDTO, newHiCase.getId(), CaseType.HH, uacTest, hhCase.getAddress());
  }

  // Happy path test for linking when the UAC doesn't link to an existing case, and one needs to be created
  @Test
  public void linkUACtoNewCase() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    // Don't find any cases when searching by UPRN
    when(dataRepo.readCollectionCasesByUprn(eq(linkRequest.getUprn().asString())))
        .thenReturn(new ArrayList<CollectionCase>());

    // Build request object. Mostly empty as we will be using the existing case
    UACLinkRequestDTO request = new UACLinkRequestDTO();
    request.setAddressLine1("Newton Manor");
    request.setAddressLine2("1 New Street");
    request.setAddressLine3("Bumble");
    request.setTownName("Newton");
    request.setRegion("W");
    request.setPostcode("AA1 2BB");
    request.setUprn(new UniquePropertyReferenceNumber(UPRN));
    request.setEstabType(EstabType.RESIDENTIAL_CARAVAN.getCode());
    request.setAddressType(AddressType.HH.name());
    
    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // Build expectation for the the address that will have been created
    Address expectedAddress = createAddressFromLinkRequest(request);

    // Verify that a new case has been created
    List<CollectionCase> newCases = grabRepoWriteCollectionCaseValues(1);
    CollectionCase newCase = newCases.get(0);
    validateCase(newCase, CaseType.HH.name(), uacTest, expectedAddress);
    
    verifyNewAddressEventSent(CaseType.HH, uacTest.getCollectionExerciseId(), expectedAddress);

    verifyUACUpdated(UAC_HASH, newCase.getId());

    VerifyQuestionnaireLinkedEventSent(uacTest.getQuestionnaireId(), newCase.getId(), null);
    
    verifyRespondentAuthenticatedEventSent(uacTest.getQuestionnaireId(), newCase.getId());

    verifyTotalNumberEventsSent(3);
    
    verifyLinkingResult(uniqueAccessCodeDTO, newCase.getId(), CaseType.HH, uacTest, newCase.getAddress());
  }

  // Happy path test for linking when the UAC doesn't link to an existing case, and one needs to be created
  @Test
  public void linkHiUACtoNewCase() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    uacTest.setCaseType(CaseType.HH.name());
    uacTest.setFormType(FormType.I.name());
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    // Don't find any cases when searching by UPRN
    when(dataRepo.readCollectionCasesByUprn(eq(linkRequest.getUprn().asString())))
        .thenReturn(new ArrayList<CollectionCase>());

    // Build request object. Mostly empty as we will be using the existing case
    UACLinkRequestDTO request = new UACLinkRequestDTO();
    request.setAddressLine1("Newton Manor");
    request.setAddressLine2("1 New Street");
    request.setAddressLine3("Bumble");
    request.setTownName("Newton");
    request.setRegion("W");
    request.setPostcode("AA1 2BB");
    request.setUprn(new UniquePropertyReferenceNumber(UPRN));
    request.setEstabType(EstabType.RESIDENTIAL_CARAVAN.getCode());
    request.setAddressType(AddressType.HH.name());
    
    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // Build expectation for the the address that will have been created
    Address expectedAddress = createAddressFromLinkRequest(request);

    // Verify that 2 new cases have been created
    List<CollectionCase> newCases = grabRepoWriteCollectionCaseValues(2);
    CollectionCase newCase = newCases.get(0);
    CollectionCase newHiCase = newCases.get(1);
    validateCase(newCase, CaseType.HH.name(), uacTest, expectedAddress);
    validateCase(newHiCase, request.getAddressType(), uacTest, expectedAddress);

    verifyNewAddressEventSent(CaseType.HH, uacTest.getCollectionExerciseId(), expectedAddress);

    verifyUACUpdated(UAC_HASH, newHiCase.getId());

    VerifyQuestionnaireLinkedEventSent(uacTest.getQuestionnaireId(), newCase.getId(), newHiCase.getId());

    verifyRespondentAuthenticatedEventSent(uacTest.getQuestionnaireId(), newHiCase.getId());

    verifyTotalNumberEventsSent(3);
    
    verifyLinkingResult(uniqueAccessCodeDTO, newHiCase.getId(), CaseType.HH, uacTest, newCase.getAddress());
  }

  // Test that we get an error when the request contains an AddressType which is not a valid enum name
  @Test
  public void attemptToLinkUACButWithInvalidAddressType() throws Exception {
    linkRequest.setAddressType("x");

    try {
      uacSvc.linkUACCase(UAC_HASH, linkRequest);
      fail("Should have failed on address type validation");
    } catch (CTPException e) {
      assertEquals(Fault.BAD_REQUEST, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains("address"));
    }
  }

  // Test that linking fails when the UAC is not found in Firestore
  @Test
  public void attemptToLinkUACtoUnknownCase() throws Exception {

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.empty());

    try {
      uacSvc.linkUACCase(UAC_HASH, linkRequest);
      fail("Should have failed to find UAC");
    } catch (CTPException e) {
      assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains("UAC"));
    }
  }

  // Test that we get a failure when multiple cases are found for the UPRN but none of them are HH cases
  @Test
  public void linkUACtoCaseButNoHHCaseFound() throws Exception {
    UAC uacTest = uac.get(0);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    // Setup cases which Firestore will return, but _without_ a HH case
    CollectionCase hiCase1 = collectionCase.get(0);
    hiCase1.setCaseType(CaseType.HI.name());
    CollectionCase hiCase2 = collectionCase.get(0);
    hiCase2.setCaseType(CaseType.HI.name());
    List<CollectionCase> cases = Stream.of(hiCase1, hiCase2).collect(Collectors.toList());
    when(dataRepo.readCollectionCasesByUprn(eq(linkRequest.getUprn().asString())))
        .thenReturn(cases);

    try {
      uacSvc.linkUACCase(UAC_HASH, linkRequest);
      fail("Should have failed as there is no HH case");
    } catch (CTPException e) {
      assertEquals(Fault.SYSTEM_ERROR, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains("Household"));
    }
  }

  private void assertAddressesEqual(Address expected, AddressDTO actual) {
    assertEquals(expected.getUprn(), actual.getUprn().asString());
    assertEquals(expected.getAddressLine1(), actual.getAddressLine1());
    assertEquals(expected.getAddressLine2(), actual.getAddressLine2());
    assertEquals(expected.getAddressLine3(), actual.getAddressLine3());
    assertEquals(expected.getTownName(), actual.getTownName());
    assertEquals(expected.getPostcode(), actual.getPostcode());
  }

  // Enum for testLinkingMatrix tests
  private enum LinkingExpectation {
    OK,
    INVALID
  }

  /**
   * This test calls the Link UAC endpoint with the UAC form type and case type permutations
   * listed in the unlinked authentication wiki page. 
   * 
   * The code is based on the permutations listed in:
   * https://collaborate2.ons.gov.uk/confluence/display/SDC/Auth.05+-+Unlinked+Authentication#Matrix
   *
   * @throws CTPException
   */
//  @Test
//  public void testLinkingMatrix() throws CTPException {
//    doLinkingTest(FormType.H, CaseType.HH, CaseType.HH, LinkingExpectation.OK);
//    doLinkingTest(FormType.H, CaseType.HH, CaseType.SPG, LinkingExpectation.OK);
//    doLinkingTest(FormType.H, CaseType.HH, CaseType.CE, LinkingExpectation.OK);
//    doLinkingTest(FormType.H, CaseType.SPG, CaseType.HH, LinkingExpectation.OK);
//    doLinkingTest(FormType.H, CaseType.SPG, CaseType.SPG, LinkingExpectation.OK);
//    doLinkingTest(FormType.H, CaseType.SPG, CaseType.CE, LinkingExpectation.OK);
//    doLinkingTest(FormType.H, CaseType.CE, CaseType.HH, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.H, CaseType.CE, CaseType.SPG, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.H, CaseType.CE, CaseType.CE, LinkingExpectation.INVALID);
//
//    doLinkingTest(FormType.I, CaseType.HH, CaseType.HH, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.HH, CaseType.SPG, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.HH, CaseType.CE, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.SPG, CaseType.HH, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.SPG, CaseType.SPG, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.SPG, CaseType.CE, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.CE, CaseType.HH, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.CE, CaseType.SPG, LinkingExpectation.OK);
//    doLinkingTest(FormType.I, CaseType.CE, CaseType.CE, LinkingExpectation.OK);
//
//    doLinkingTest(FormType.CE1, CaseType.HH, CaseType.HH, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.HH, CaseType.SPG, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.HH, CaseType.CE, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.SPG, CaseType.HH, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.SPG, CaseType.SPG, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.SPG, CaseType.CE, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.CE, CaseType.HH, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.CE, CaseType.SPG, LinkingExpectation.INVALID);
//    doLinkingTest(FormType.CE1, CaseType.CE, CaseType.CE, LinkingExpectation.OK);
//  }

  private void doLinkingTest(
      FormType uacFormType,
      CaseType uacCaseType,
      CaseType caseCaseType,
      LinkingExpectation linkAllowed)
      throws CTPException {
    // Setup fake UAC
    UAC uacTest = uac.get(0);
    uacTest.setFormType(uacFormType.name());
    uacTest.setCaseType(uacCaseType.name());
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    // Setup fake Case
    CollectionCase caseToLinkTo = collectionCase.get(0);
    caseToLinkTo.setCaseType(caseCaseType.name());
    List<CollectionCase> cases = Stream.of(caseToLinkTo).collect(Collectors.toList());
    when(dataRepo.readCollectionCasesByUprn(eq(linkRequest.getUprn().asString())))
        .thenReturn(cases);

    // Invoke code under test and decide if it threw an incompatible UAC/Case exception
    boolean incompatibleUACandCaseThrown;
    try {
      uacSvc.linkUACCase(UAC_HASH, linkRequest);
      incompatibleUACandCaseThrown = false;
    } catch (CTPException e) {
      if (e.getMessage().contains("incompatible")) {
        incompatibleUACandCaseThrown = true;
      } else {
        throw e;
      }
    }

    // Decide if code under test behaved correctly
    if (linkAllowed == LinkingExpectation.OK && incompatibleUACandCaseThrown) {
      fail();
    } else if (linkAllowed == LinkingExpectation.INVALID && !incompatibleUACandCaseThrown) {
      fail();
    }
  }
  
  private Address createAddressFromLinkRequest(UACLinkRequestDTO request) {
    Address expectedAddress = new Address();
   
    expectedAddress.setAddressLine1(request.getAddressLine1());
    expectedAddress.setAddressLine2(request.getAddressLine2());
    expectedAddress.setAddressLine3(request.getAddressLine3());
    expectedAddress.setTownName(request.getTownName());
    expectedAddress.setPostcode(request.getPostcode());
    expectedAddress.setRegion(request.getRegion());
    expectedAddress.setUprn(request.getUprn().asString());
    expectedAddress.setAddressType(request.getAddressType());
    expectedAddress.setAddressLevel(AddressLevel.U.name());
    expectedAddress.setEstabType(request.getEstabType());

    return expectedAddress;
  }

  // Support method to get the newly created cases from dataRepo.writeCollectionCase and return the cases
  private List<CollectionCase> grabRepoWriteCollectionCaseValues(int expectedNumberCasesCreated) throws CTPException {
    ArgumentCaptor<CollectionCase> caseCapture = ArgumentCaptor.forClass(CollectionCase.class);
    verify(dataRepo, times(expectedNumberCasesCreated)).writeCollectionCase(caseCapture.capture());

    return caseCapture.getAllValues();
  }

  private void validateCase(CollectionCase newCase, String expectedCaseType, UAC uac, Address expectedAddress) {
    assertEquals(null, newCase.getCaseRef());
    assertEquals(expectedCaseType, newCase.getCaseType());
    assertEquals("CENSUS", newCase.getSurvey());
    assertEquals(uac.getCollectionExerciseId(), newCase.getCollectionExerciseId());
    assertEquals(expectedAddress, newCase.getAddress());
    assertEquals(new Contact(), newCase.getContact());
    assertEquals(null, newCase.getActionableFrom());
    assertFalse(newCase.isHandDelivery());
  }
  
  private void verifyNewAddressEventSent(CaseType hh, String collectionExerciseId, Address expectedAddress) {
    ArgumentCaptor<CollectionCaseNewAddress> newAddressCapture = ArgumentCaptor.forClass(CollectionCaseNewAddress.class);
    verify(eventPublisher, times(1))
      .sendEvent(
        eq(EventType.NEW_ADDRESS_REPORTED),
        eq(Source.RESPONDENT_HOME),
        eq(Channel.RH),
        newAddressCapture.capture());
    
    CollectionCaseNewAddress newAddress = newAddressCapture.getValue();
    assertEquals(CaseType.HH.name(), newAddress.getCaseType());
    assertEquals("CENSUS", newAddress.getSurvey());
    assertEquals(collectionExerciseId, newAddress.getCollectionExerciseId());
    assertEquals(null, newAddress.getFieldCoordinatorId());
    assertEquals(null, newAddress.getFieldOfficerId());
    assertEquals(expectedAddress, newAddress.getAddress());
  }

  private void verifyUACUpdated(String uacHash, String expectedCaseId) throws CTPException {
    ArgumentCaptor<UAC> uacUpdateCapture = ArgumentCaptor.forClass(UAC.class);
    verify(dataRepo, times(1)).writeUAC(uacUpdateCapture.capture());
    
    UAC uacUpdated = uacUpdateCapture.getValue();
    assertEquals(UAC_HASH, uacUpdated.getUacHash());
    assertEquals(expectedCaseId, uacUpdated.getCaseId());
  }

  private void VerifyQuestionnaireLinkedEventSent(String questionnaireId, String caseId, String individualCaseId) {
    ArgumentCaptor<QuestionnaireLinkedDetails> questionnaireLinkedCapture =
        ArgumentCaptor.forClass(QuestionnaireLinkedDetails.class);
    verify(eventPublisher, times(1))
      .sendEvent(
        eq(EventType.QUESTIONNAIRE_LINKED),
        eq(Source.RESPONDENT_HOME),
        eq(Channel.RH),
        questionnaireLinkedCapture.capture());

    QuestionnaireLinkedDetails questionnaireLinked = questionnaireLinkedCapture.getValue();
    assertEquals(questionnaireId, questionnaireLinked.getQuestionnaireId());
    assertEquals(UUID.fromString(caseId), questionnaireLinked.getCaseId());
    if (individualCaseId == null) {
      assertNull(questionnaireLinked.getIndividualCaseId());
    } else {
      assertEquals(individualCaseId, questionnaireLinked.getIndividualCaseId().toString());
    }
  }

  private void verifyRespondentAuthenticatedEventSent(String questionnaireId, String caseId) {
    ArgumentCaptor<RespondentAuthenticatedResponse> payloadCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedResponse.class);
    verify(eventPublisher, times(1))
      .sendEvent(
        eq(EventType.RESPONDENT_AUTHENTICATED),
        eq(Source.RESPONDENT_HOME),
        eq(Channel.RH),
        payloadCapture.capture());

    RespondentAuthenticatedResponse respondentAuthenticated = payloadCapture.getValue();
    assertEquals(questionnaireId, respondentAuthenticated.getQuestionnaireId());
    assertEquals(UUID.fromString(caseId), respondentAuthenticated.getCaseId());
  }

  private void verifyTotalNumberEventsSent(int expectedNumEventsSent) {
    verify(eventPublisher, times(expectedNumEventsSent)).sendEvent(any(), any(), any(), any());
  }

  private void verifyLinkingResult(UniqueAccessCodeDTO uniqueAccessCodeDTO, String expectedCaseId, CaseType expectedCaseType, UAC uacTest, Address address) {
    assertEquals(UAC_HASH, uniqueAccessCodeDTO.getUacHash());
    assertEquals(true, uniqueAccessCodeDTO.isActive());
    assertEquals(CaseStatus.OK, uniqueAccessCodeDTO.getCaseStatus());
    assertEquals(uacTest.getQuestionnaireId(), uniqueAccessCodeDTO.getQuestionnaireId());
    assertEquals(expectedCaseType.name(), uniqueAccessCodeDTO.getCaseType());
    assertEquals(uacTest.getRegion(), uniqueAccessCodeDTO.getRegion());
    assertEquals(expectedCaseId, uniqueAccessCodeDTO.getCaseId().toString());
    assertEquals(uacTest.getCollectionExerciseId(), uniqueAccessCodeDTO.getCollectionExerciseId().toString());
    assertAddressesEqual(address, uniqueAccessCodeDTO.getAddress());
    assertEquals(uacTest.getFormType(), uniqueAccessCodeDTO.getFormType());
    assertEquals(false, uniqueAccessCodeDTO.isHandDelivery());
  }
}