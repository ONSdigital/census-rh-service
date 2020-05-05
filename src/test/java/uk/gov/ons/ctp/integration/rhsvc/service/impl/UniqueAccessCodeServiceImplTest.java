package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
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
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.RespondentAuthenticatedResponse;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.model.CaseType;
import uk.gov.ons.ctp.common.model.FormType;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.UACLinkRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO.CaseStatus;

/** Unit tests of the Unique Access Code Service */
public class UniqueAccessCodeServiceImplTest {

  private static final String UAC_HASH =
      "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4";
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";

  @InjectMocks private UniqueAccessCodeServiceImpl uacSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  private List<UAC> uac;
  private List<CollectionCase> collectionCase;
  private UACLinkRequestDTO linkRequest;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.uac = FixtureHelper.loadClassFixtures(UAC[].class);
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
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
        UUID.fromString(uacTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
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

  @Test
  public void linkUACtoCase() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    uacSvc.linkUACCase(UAC_HASH, linkRequest);
  }

  @Test
  public void linkUACtoExistingCase() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    CollectionCase hiCase1 = collectionCase.get(0);
    hiCase1.setCaseType(CaseType.HI.name());
    CollectionCase hiCase2 = collectionCase.get(0);
    hiCase2.setCaseType(CaseType.HI.name());
    CollectionCase hhCase = collectionCase.get(0);
    hhCase.setCaseType(CaseType.HH.name());
    List<CollectionCase> cases = Stream.of(hiCase1, hiCase2, hhCase).collect(Collectors.toList());
    when(dataRepo.readCollectionCasesByUprn(eq(linkRequest.getUprn().asString())))
        .thenReturn(cases);

    // Attempt linking
    uacSvc.linkUACCase(UAC_HASH, linkRequest);
  }

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

  private enum LinkingExpectation {
    OK,
    INVALID
  }

  /**
   * This test runs the UAC form type and case type permutations defined in the unlinked
   * authentication wiki page. It's based on the permutations listed in:
   * https://collaborate2.ons.gov.uk/confluence/display/SDC/Auth.05+-+Unlinked+Authentication#Matrix
   *
   * @throws CTPException
   */
  @Test
  public void testLinkingMatrix() throws CTPException {
    doLinkingTest(FormType.H, CaseType.HH, CaseType.HH, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.HH, CaseType.SPG, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.HH, CaseType.CE, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.SPG, CaseType.HH, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.SPG, CaseType.SPG, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.SPG, CaseType.CE, LinkingExpectation.OK);
    doLinkingTest(FormType.H, CaseType.CE, CaseType.HH, LinkingExpectation.INVALID);
    doLinkingTest(FormType.H, CaseType.CE, CaseType.SPG, LinkingExpectation.INVALID);
    doLinkingTest(FormType.H, CaseType.CE, CaseType.CE, LinkingExpectation.INVALID);

    doLinkingTest(FormType.I, CaseType.HH, CaseType.HH, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.HH, CaseType.SPG, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.HH, CaseType.CE, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.SPG, CaseType.HH, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.SPG, CaseType.SPG, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.SPG, CaseType.CE, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.CE, CaseType.HH, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.CE, CaseType.SPG, LinkingExpectation.OK);
    doLinkingTest(FormType.I, CaseType.CE, CaseType.CE, LinkingExpectation.OK);

    doLinkingTest(FormType.CE1, CaseType.HH, CaseType.HH, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.HH, CaseType.SPG, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.HH, CaseType.CE, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.SPG, CaseType.HH, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.SPG, CaseType.SPG, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.SPG, CaseType.CE, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.CE, CaseType.HH, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.CE, CaseType.SPG, LinkingExpectation.INVALID);
    doLinkingTest(FormType.CE1, CaseType.CE, CaseType.CE, LinkingExpectation.OK);
  }

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
    boolean incompatibleUACandCase;
    try {
      uacSvc.linkUACCase(UAC_HASH, linkRequest);
      incompatibleUACandCase = false;
    } catch (CTPException e) {
      if (e.getMessage().contains("incompatible")) {
        incompatibleUACandCase = true;
      } else {
        throw e;
      }
    }

    // Decide if code under test behaved correctly
    if (linkAllowed == LinkingExpectation.OK && incompatibleUACandCase) {
      fail();
    } else if (linkAllowed == LinkingExpectation.INVALID && !incompatibleUACandCase) {
      fail();
    }
  }
}
