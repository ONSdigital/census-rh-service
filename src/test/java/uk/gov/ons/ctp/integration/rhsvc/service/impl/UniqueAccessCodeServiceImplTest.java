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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressLevel;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.FormType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.QuestionnaireLinkedDetails;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;

/** Unit tests of the Unique Access Code Service */
@RunWith(MockitoJUnitRunner.class)
public class UniqueAccessCodeServiceImplTest {

  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";
  private static final String CASE_ID = "bfb5cdca-3119-4d2c-a807-51ae55443b33";

  // the actual census id as per the application.yml and also RM
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @InjectMocks private UniqueAccessCodeServiceImpl uacSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Spy private AppConfig appConfig = new AppConfig();

  private TestUtil testUtil;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    appConfig.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    ReflectionTestUtils.setField(uacSvc, "appConfig", appConfig);

    testUtil = new TestUtil(dataRepo, eventPublisher);
  }

  @Test
  public void getUACLinkedToExistingCase() throws Exception {

    ArgumentCaptor<RespondentAuthenticatedResponse> payloadCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedResponse.class);

    UAC uacTest = getUAC("linkedHousehold");
    CollectionCase caseTest = getCase("household");

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
    assertEquals(caseTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(caseTest.getAddress().getEstabType(), uacDTO.getEstabType());
    assertEquals(caseTest.getAddress().getRegion(), uacDTO.getRegion());
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

  @Test
  public void getUACLinkedToCaseThatCannotBeFound() throws Exception {

    ArgumentCaptor<RespondentAuthenticatedResponse> payloadCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedResponse.class);

    UAC uacTest = getUAC("linkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

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
    assertEquals(CaseStatus.UNLINKED, uacDTO.getCaseStatus());
    assertNull(uacDTO.getCaseId());
    assertNull(uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertNull(uacDTO.getCaseType());
    assertNull(uacDTO.getRegion());
    assertNull(uacDTO.getEstabType());

    assertNull(uacDTO.getAddress());

    RespondentAuthenticatedResponse payload = payloadCapture.getValue();
    assertNull(payload.getCaseId());
    assertEquals(uacDTO.getQuestionnaireId(), payload.getQuestionnaireId());
  }

  @Test
  public void getUACNotLInkedToCase() throws Exception {

    ArgumentCaptor<RespondentAuthenticatedResponse> payloadCapture =
        ArgumentCaptor.forClass(RespondentAuthenticatedResponse.class);

    UAC uacTest = getUAC("unlinkedHousehold");

    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getAndAuthenticateUAC(UAC_HASH);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCollectionCase(CASE_ID);
    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.RESPONDENT_AUTHENTICATED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    assertEquals(UAC_HASH, uacDTO.getUacHash());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(CaseStatus.UNLINKED, uacDTO.getCaseStatus());
    assertNull(uacDTO.getCaseId());
    assertNull(uacDTO.getCollectionExerciseId());
    assertEquals(uacTest.getQuestionnaireId(), uacDTO.getQuestionnaireId());
    assertNull(uacDTO.getCaseType());
    assertNull(uacDTO.getRegion());
    assertNull(uacDTO.getEstabType());

    assertNull(uacDTO.getAddress());

    RespondentAuthenticatedResponse payload = payloadCapture.getValue();
    assertEquals(uacDTO.getCaseId(), payload.getCaseId());
    assertEquals(uacDTO.getQuestionnaireId(), payload.getQuestionnaireId());
  }

  /** Test request for claim object where UAC not found */
  @Test
  public void getUACNotFound() throws Exception {

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

  @Test
  public void linkHouseholdUACToExistingHouseholdCase() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    UAC householdUAC = getUAC("unlinkedHousehold");
    mockDataRepoForReadUAC(UAC_HASH, householdUAC);

    CollectionCase householdCase = getCase("household");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), householdCase);

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    grabRepoWriteCollectionCaseValues(0); // No cases created

    verifyUACUpdated(UAC_HASH, householdCase.getId());

    VerifyQuestionnaireLinkedEventSent(
        householdUAC.getQuestionnaireId(), householdCase.getId(), null);

    verifyRespondentAuthenticatedEventSent(
        householdUAC.getQuestionnaireId(), householdCase.getId());

    verifyTotalNumberEventsSent(2);

    verifyLinkingResult(
        uniqueAccessCodeDTO, householdCase.getId(), CaseType.HH, householdUAC, householdCase);
  }
  /**
   * Isolated assertion that the HH and not the HI
   *
   * @throws Exception ugh
   */
  @Test
  public void linkHouseholdUACToSingleHousehold() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    UAC householdUAC = getUAC("unlinkedHousehold");
    mockDataRepoForReadUAC(UAC_HASH, householdUAC);

    List<CollectionCase> cases = getCases("HHandHI");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), cases.get(0));

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // assert that the case selected was the HH and not the HI
    assertEquals(CASE_ID, uniqueAccessCodeDTO.getCaseId().toString());
  }

  /**
   * Isolated assertion that the HH with addressInvalid is not selected
   *
   * @throws Exception ugh
   */
  @Test
  public void linkHouseholdUACToValidHousehold() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    UAC householdUAC = getUAC("unlinkedHousehold");
    mockDataRepoForReadUAC(UAC_HASH, householdUAC);

    List<CollectionCase> cases = getCases("HH-addressInvalid");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), cases.get(1));

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // assert that the case selected was the HH and not the HI
    assertEquals(CASE_ID, uniqueAccessCodeDTO.getCaseId().toString());
  }

  /**
   * Isolated assertion that the HH with addressInvalid is not selected
   *
   * @throws Exception ugh
   */
  @Test
  public void linkHouseholdUACtoLatestValidHousehold() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    UAC householdUAC = getUAC("unlinkedHousehold");
    mockDataRepoForReadUAC(UAC_HASH, householdUAC);

    List<CollectionCase> cases = getCases("HH-multiples");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), cases.get(1));

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // assert that the case selected was the HH and not the HI
    assertEquals(CASE_ID, uniqueAccessCodeDTO.getCaseId().toString());
  }

  @Test
  public void linkCE1UACToExistingCECase() throws Exception {
    CaseRequestDTO request = getRequest("CEAddress");

    UAC ceUAC = getUAC("unlinkedCE");
    mockDataRepoForReadUAC(UAC_HASH, ceUAC);

    CollectionCase ceCase = getCase("CE");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), ceCase);

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    grabRepoWriteCollectionCaseValues(0); // No cases created

    verifyUACUpdated(UAC_HASH, ceCase.getId());

    VerifyQuestionnaireLinkedEventSent(ceUAC.getQuestionnaireId(), ceCase.getId(), null);

    verifyRespondentAuthenticatedEventSent(ceUAC.getQuestionnaireId(), ceCase.getId());

    verifyTotalNumberEventsSent(2);

    verifyLinkingResult(uniqueAccessCodeDTO, ceCase.getId(), CaseType.CE, ceUAC, ceCase);
  }

  @Test
  public void linkHouseholdUACToExistingSPGCase() throws Exception {
    CaseRequestDTO request = getRequest("SPGAddress");

    UAC ceUAC = getUAC("unlinkedHousehold");
    mockDataRepoForReadUAC(UAC_HASH, ceUAC);

    CollectionCase ceCase = getCase("SPG");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), ceCase);

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    grabRepoWriteCollectionCaseValues(0); // No cases created

    verifyUACUpdated(UAC_HASH, ceCase.getId());

    VerifyQuestionnaireLinkedEventSent(ceUAC.getQuestionnaireId(), ceCase.getId(), null);

    verifyRespondentAuthenticatedEventSent(ceUAC.getQuestionnaireId(), ceCase.getId());

    verifyTotalNumberEventsSent(2);

    verifyLinkingResult(uniqueAccessCodeDTO, ceCase.getId(), CaseType.SPG, ceUAC, ceCase);
  }

  @Test
  public void linkIndividualUACToExistingHouseholdCase() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    UAC individualUAC = getUAC("unlinkedIndividual");
    mockDataRepoForReadUAC(UAC_HASH, individualUAC);

    CollectionCase householdCase = getCase("household");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), householdCase);

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // Build expectation for the the address that will have been created
    Address expectedAddress = createAddressFromLinkRequest(request, CaseType.HI);
    // Verify that new individual case has been created
    List<CollectionCase> newCases = grabRepoWriteCollectionCaseValues(1);
    CollectionCase newIndividualCase = newCases.get(0);
    testUtil.validateCase(CaseType.HI, expectedAddress, newIndividualCase);

    verifyUACUpdated(UAC_HASH, newIndividualCase.getId());

    VerifyQuestionnaireLinkedEventSent(
        individualUAC.getQuestionnaireId(), householdCase.getId(), newIndividualCase.getId());

    verifyRespondentAuthenticatedEventSent(
        individualUAC.getQuestionnaireId(), newIndividualCase.getId());

    verifyTotalNumberEventsSent(2);

    verifyLinkingResult(
        uniqueAccessCodeDTO, newIndividualCase.getId(), CaseType.HI, individualUAC, householdCase);
  }

  // Happy path test for linking when the UAC cannot be linked to an existing case, and one needs to
  // be
  // created.
  // UAC is HH so no new HI case is created.
  @Test
  public void linkHouseholdUACToNewHouseholdCase() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    UAC householdUAC = getUAC("unlinkedHousehold");
    mockDataRepoForReadUAC(UAC_HASH, householdUAC);

    // Default - don't find any cases when searching by UPRN

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // Build expectation for the the address that will have been created
    Address expectedAddress = createAddressFromLinkRequest(request, CaseType.HH);

    // Verify that a new case has been created
    List<CollectionCase> newCases = grabRepoWriteCollectionCaseValues(1);
    CollectionCase newCase = newCases.get(0);
    testUtil.validateCase(CaseType.HH, expectedAddress, newCase);

    testUtil.verifyNewAddressEventSent(newCase.getId(), CaseType.HH, expectedAddress);

    verifyUACUpdated(UAC_HASH, newCase.getId());

    VerifyQuestionnaireLinkedEventSent(householdUAC.getQuestionnaireId(), newCase.getId(), null);

    verifyRespondentAuthenticatedEventSent(householdUAC.getQuestionnaireId(), newCase.getId());

    verifyTotalNumberEventsSent(3);

    verifyLinkingResult(uniqueAccessCodeDTO, newCase.getId(), CaseType.HH, householdUAC, newCase);
  }

  // Happy path test for linking when the UAC doesn't link to an existing case, and one needs to be
  // created.
  // As the UAC is HI a new HI case is also created.
  @Test
  public void linkIndividualUACToNewHouseholdCase() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    UAC individualUAC = getUAC("unlinkedIndividual");
    mockDataRepoForReadUAC(UAC_HASH, individualUAC);

    // Don't find any cases when searching by UPRN

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // Build expectation for the the address that will have been created
    Address expectedAddressHH = createAddressFromLinkRequest(request, CaseType.HH);
    Address expectedAddressHI = createAddressFromLinkRequest(request, CaseType.HI);

    // Verify that 2 new cases have been created
    List<CollectionCase> newCases = grabRepoWriteCollectionCaseValues(2);
    CollectionCase newCase = newCases.get(0);
    CollectionCase newHiCase = newCases.get(1);
    testUtil.validateCase(CaseType.HH, expectedAddressHH, newCase);
    testUtil.validateCase(CaseType.HI, expectedAddressHI, newHiCase);

    testUtil.verifyNewAddressEventSent(newCase.getId(), CaseType.HH, expectedAddressHH);

    verifyUACUpdated(UAC_HASH, newHiCase.getId());

    VerifyQuestionnaireLinkedEventSent(
        individualUAC.getQuestionnaireId(), newCase.getId(), newHiCase.getId());

    verifyRespondentAuthenticatedEventSent(individualUAC.getQuestionnaireId(), newHiCase.getId());

    verifyTotalNumberEventsSent(3);

    verifyLinkingResult(
        uniqueAccessCodeDTO, newHiCase.getId(), CaseType.HI, individualUAC, newHiCase);
  }

  // Happy path test for attempting to link a UAC to the case which it's already linked to.
  // This should be detected and virtually no work will be done.
  @Test
  public void linkUACToCurrentCase() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    // Setup fake Case
    CollectionCase caseToLinkTo = getCase("CE");
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), caseToLinkTo);

    // Setup UAC which is already linked to the fake case
    UAC individualUAC = getUAC("linkedIndividual");
    individualUAC.setCaseId(caseToLinkTo.getId());
    mockDataRepoForReadUAC(UAC_HASH, individualUAC);

    // Run code under test: Attempt linking
    UniqueAccessCodeDTO uniqueAccessCodeDTO = uacSvc.linkUACCase(UAC_HASH, request);

    // Virtually nothing should be done. No case written, no UAC updated & no events sent
    grabRepoWriteCollectionCaseValues(0);
    Mockito.verify(dataRepo, times(0)).writeUAC(any());
    verifyTotalNumberEventsSent(0);

    verifyLinkingResult(
        uniqueAccessCodeDTO, caseToLinkTo.getId(), CaseType.CE, individualUAC, caseToLinkTo);
  }

  // Test that linking fails when the UAC is not found in Firestore
  @Test
  public void attemptToLinkUACtoUnknownCase() throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    try {
      uacSvc.linkUACCase(UAC_HASH, request);
      fail("Should have failed to find UAC");
    } catch (CTPException e) {
      assertEquals(Fault.RESOURCE_NOT_FOUND, e.getFault());
      assertTrue(e.getMessage(), e.getMessage().contains("UAC"));
    }
  }

  private void assertAddressesEqual(Address expected, AddressDTO actual) {
    assertEquals(expected.getUprn(), Long.toString(actual.getUprn().getValue()));
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
   * This test calls the Link UAC endpoint with the UAC form type and case type permutations listed
   * in the unlinked authentication wiki page.
   *
   * <p>The code is based on the permutations listed in:
   * https://collaborate2.ons.gov.uk/confluence/display/SDC/RH+-+Authentication+-+Unlinked+UAC
   * https://collaborate2.ons.gov.uk/confluence/display/SDC/Business+Rules
   *
   * @throws CTPException - exception
   */
  @Test
  public void testLinkingMatrix() throws Exception {
    doLinkingTest(FormType.H, CaseType.HH, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.SPG, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.CE, LinkingExpectation.INVALID);

    doLinkingTest(FormType.I, CaseType.HH, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.SPG, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.CE, LinkingExpectation.OK);

    doLinkingTest(FormType.C, CaseType.HH, LinkingExpectation.INVALID);
    doLinkingTest(FormType.C, CaseType.SPG, LinkingExpectation.INVALID);
    doLinkingTest(FormType.C, CaseType.CE, LinkingExpectation.OK);
  }

  private void doLinkingTest(
      FormType uacFormType, CaseType caseCaseType, LinkingExpectation linkAllowed)
      throws Exception {
    CaseRequestDTO request = getRequest("householdAddress");

    // Setup fake UAC
    UAC uacTest = getUAC("unlinkedHousehold"); // any old uac as form type to be overridden
    uacTest.setFormType(uacFormType.name());
    mockDataRepoForReadUAC(UAC_HASH, uacTest);

    // Setup fake Case
    CollectionCase caseToLinkTo = getCase("household");
    caseToLinkTo.setCaseType(caseCaseType.name());
    mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(request.getUprn(), caseToLinkTo);

    // Invoke code under test and decide if it threw an incompatible UAC/Case exception
    boolean incompatibleUACandCaseThrown;
    try {
      uacSvc.linkUACCase(UAC_HASH, request);
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

  private void mockDataRepoForReadNonHILatestValidCollectionCaseByUprn(
      UniquePropertyReferenceNumber uprn, CollectionCase fakeCase) throws CTPException {
    List<CollectionCase> cases = Stream.of(fakeCase).collect(Collectors.toList());
    when(dataRepo.readNonHILatestCollectionCaseByUprn(eq(Long.toString(uprn.getValue())), eq(true)))
        .thenReturn(Optional.of(cases.get(0)));
  }

  private void mockDataRepoForReadUAC(String uacHash, UAC fakeUAC) throws CTPException {
    when(dataRepo.readUAC(uacHash)).thenReturn(Optional.of(fakeUAC));
  }

  private Address createAddressFromLinkRequest(CaseRequestDTO request, CaseType caseType) {
    Address expectedAddress = new Address();

    expectedAddress.setAddressLine1(request.getAddressLine1());
    expectedAddress.setAddressLine2(request.getAddressLine2());
    expectedAddress.setAddressLine3(request.getAddressLine3());
    expectedAddress.setTownName(request.getTownName());
    expectedAddress.setPostcode(request.getPostcode());
    expectedAddress.setRegion(request.getRegion().name());
    expectedAddress.setUprn(Long.toString(request.getUprn().getValue()));
    expectedAddress.setAddressType(caseType.name());
    expectedAddress.setAddressLevel(AddressLevel.U.name());
    expectedAddress.setEstabType(EstabType.forCode(request.getEstabType()).getCode());

    return expectedAddress;
  }

  // Support method to get the newly created cases from dataRepo.writeCollectionCase and return the
  // cases
  private List<CollectionCase> grabRepoWriteCollectionCaseValues(int expectedNumberCasesCreated)
      throws CTPException {
    ArgumentCaptor<CollectionCase> caseCapture = ArgumentCaptor.forClass(CollectionCase.class);
    verify(dataRepo, times(expectedNumberCasesCreated)).writeCollectionCase(caseCapture.capture());

    return caseCapture.getAllValues();
  }

  private void verifyUACUpdated(String uacHash, String expectedCaseId) throws CTPException {
    ArgumentCaptor<UAC> uacUpdateCapture = ArgumentCaptor.forClass(UAC.class);
    verify(dataRepo, times(1)).writeUAC(uacUpdateCapture.capture());

    UAC uacUpdated = uacUpdateCapture.getValue();
    assertEquals(UAC_HASH, uacUpdated.getUacHash());
    assertEquals(expectedCaseId, uacUpdated.getCaseId());
  }

  private void VerifyQuestionnaireLinkedEventSent(
      String questionnaireId, String caseId, String individualCaseId) {
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

  private void verifyLinkingResult(
      UniqueAccessCodeDTO uniqueAccessCodeDTO,
      String expectedCaseId,
      CaseType expectedCaseType,
      UAC uacTest,
      CollectionCase caseTest) {
    assertEquals(UAC_HASH, uniqueAccessCodeDTO.getUacHash());
    assertTrue(uniqueAccessCodeDTO.isActive());
    assertEquals(CaseStatus.OK, uniqueAccessCodeDTO.getCaseStatus());
    assertEquals(uacTest.getQuestionnaireId(), uniqueAccessCodeDTO.getQuestionnaireId());
    assertEquals(expectedCaseType.name(), uniqueAccessCodeDTO.getCaseType());
    assertEquals(caseTest.getAddress().getRegion(), uniqueAccessCodeDTO.getRegion());
    assertEquals(expectedCaseId, uniqueAccessCodeDTO.getCaseId().toString());
    assertEquals(
        caseTest.getCollectionExerciseId(),
        uniqueAccessCodeDTO.getCollectionExerciseId().toString());
    assertAddressesEqual(caseTest.getAddress(), uniqueAccessCodeDTO.getAddress());
    assertEquals(uacTest.getFormType(), uniqueAccessCodeDTO.getFormType());
    assertFalse(uniqueAccessCodeDTO.isHandDelivery());
  }

  private UAC getUAC(String qualifier) {
    return FixtureHelper.loadClassFixtures(UAC[].class, qualifier).get(0);
  }

  private CollectionCase getCase(String qualifier) {
    return FixtureHelper.loadClassFixtures(CollectionCase[].class, qualifier).get(0);
  }

  private List<CollectionCase> getCases(String qualifier) {
    return FixtureHelper.loadClassFixtures(CollectionCase[].class, qualifier);
  }

  private CaseRequestDTO getRequest(String qualifier) {
    return FixtureHelper.loadClassFixtures(CaseRequestDTO[].class, qualifier).get(0);
  }
}
