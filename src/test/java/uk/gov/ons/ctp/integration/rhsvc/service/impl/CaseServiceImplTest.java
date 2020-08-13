package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressLevel;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseRequestDTO;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplTest {

  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber("123456");

  // the actual census id as per the application.yml and also RM
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Mock private ProductReference productReference;

  @Spy private AppConfig appConfig = new AppConfig();

  private TestUtil testUtil;

  private List<CollectionCase> collectionCase;
  private List<AddressChangeDTO> addressChangeDTO;

  /** Setup tests */
  @Before
  public void setUp() {
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.addressChangeDTO = FixtureHelper.loadClassFixtures(AddressChangeDTO[].class);

    appConfig.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    ReflectionTestUtils.setField(caseSvc, "appConfig", appConfig);

    testUtil = new TestUtil(dataRepo, eventPublisher);
  }

  /** Test returns valid CaseDTO for valid UPRN */
  @Test
  public void getCaseByUPRNFound() throws Exception {

    when(dataRepo.readNonHILatestValidCollectionCaseByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Optional.of(collectionCase.get(0)));

    CollectionCase nonHICase = this.collectionCase.get(0);

    CaseDTO rmCase = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertNotNull(rmCase);
    assertEquals(nonHICase.getId(), rmCase.getCaseId().toString());
    assertEquals(nonHICase.getCaseRef(), rmCase.getCaseRef());
    assertEquals(nonHICase.getCaseType(), rmCase.getCaseType());
    assertEquals(nonHICase.getAddress().getAddressType(), rmCase.getAddressType());
    assertEquals(nonHICase.getAddress().getAddressLine1(), rmCase.getAddress().getAddressLine1());
    assertEquals(nonHICase.getAddress().getAddressLine2(), rmCase.getAddress().getAddressLine2());
    assertEquals(nonHICase.getAddress().getAddressLine3(), rmCase.getAddress().getAddressLine3());
    assertEquals(nonHICase.getAddress().getTownName(), rmCase.getAddress().getTownName());
    assertEquals(nonHICase.getAddress().getRegion(), rmCase.getRegion());
    assertEquals(nonHICase.getAddress().getPostcode(), rmCase.getAddress().getPostcode());
    assertEquals(
        nonHICase.getAddress().getUprn(), Long.toString(rmCase.getAddress().getUprn().getValue()));
  }

  /** Test throws a CTPException where no valid Address cases are returned from repository */
  @Test(expected = CTPException.class)
  public void getInvalidAddressCaseByUPRNOnly() throws Exception {
    when(dataRepo.readNonHILatestValidCollectionCaseByUprn(Long.toString(UPRN.getValue())))
        .thenThrow(new CTPException(null));
    caseSvc.getLatestValidNonHICaseByUPRN(UPRN);
  }

  /** Test retrieves latest case when all valid HH */
  @Test
  public void getLatestCaseByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);

    collectionCase.forEach(cc -> cc.setCaseType("HH"));

    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(1).setCreatedDateTime(latest); // EXPECTED
    collectionCase.get(2).setCreatedDateTime(earliest);
    when(dataRepo.readNonHILatestValidCollectionCaseByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Optional.of(collectionCase.get(1)));
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals(
        "Resultant Case created date should match expected case with latest date",
        UUID.fromString(collectionCase.get(1).getId()),
        result.getCaseId());
  }

  /** Test retrieves latest valid case when actual latest date is an HI case */
  @Test
  public void getLatestCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH"); // EXPECTED
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI"); // INVALID
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH"); // VALID
    when(dataRepo.readNonHILatestValidCollectionCaseByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Optional.of(collectionCase.get(0)));
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals(
        "Resultant Case created date should match expected case with latest date",
        UUID.fromString(collectionCase.get(0).getId()),
        result.getCaseId());
  }

  /** Test retrieves latest Address valid case when actual latest date is an HI case */
  @Test
  public void getLatestAddressValidCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH");
    collectionCase.get(0).setAddressInvalid(Boolean.TRUE); // INVALID
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI"); // INVALID
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH"); // VALID / EXPECTED
    when(dataRepo.readNonHILatestValidCollectionCaseByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Optional.of(collectionCase.get(2)));
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals(
        "Resultant Case created date should match expected case with latest date and Valid Address",
        UUID.fromString(collectionCase.get(2).getId()),
        result.getCaseId());
  }

  /** Test Test throws a CTPException where no cases returned from repository */
  @Test(expected = CTPException.class)
  public void getCaseByUPRNNotFound() throws Exception {

    when(dataRepo.readNonHILatestValidCollectionCaseByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Optional.empty());

    caseSvc.getLatestValidNonHICaseByUPRN(UPRN);
  }

  /** Test returns valid CaseDTO and sends address modified event message for valid CaseID */
  @Test
  public void modifyAddressByCaseIdFound() throws Exception {

    CollectionCase rmCase = collectionCase.get(0);
    AddressChangeDTO addressChange = addressChangeDTO.get(0);
    ArgumentCaptor<AddressModification> payloadCapture =
        ArgumentCaptor.forClass(AddressModification.class);

    when(dataRepo.readCollectionCase(rmCase.getId())).thenReturn(Optional.of(rmCase));

    CaseDTO caseDTO = caseSvc.modifyAddress(addressChange);

    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.ADDRESS_MODIFIED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    AddressModification payload = payloadCapture.getValue();
    AddressCompact originalAddress = payload.getOriginalAddress();
    AddressCompact newAddress = payload.getNewAddress();
    AddressDTO addressUpdate = addressChange.getAddress();

    assertEquals(rmCase.getId(), caseDTO.getCaseId().toString());
    assertEquals(rmCase.getCaseRef(), caseDTO.getCaseRef());
    assertEquals(rmCase.getCaseType(), caseDTO.getCaseType());
    assertEquals(rmCase.getAddress().getAddressType(), caseDTO.getAddressType());
    assertSame(addressChange.getAddress(), caseDTO.getAddress());
    assertEquals(rmCase.getAddress().getRegion(), caseDTO.getRegion());

    assertEquals(payload.getCollectionCase().getId().toString(), rmCase.getId());

    assertEquals(rmCase.getAddress().getAddressLine1(), originalAddress.getAddressLine1());
    assertEquals(rmCase.getAddress().getAddressLine2(), originalAddress.getAddressLine2());
    assertEquals(rmCase.getAddress().getAddressLine3(), originalAddress.getAddressLine3());
    assertEquals(rmCase.getAddress().getTownName(), originalAddress.getTownName());
    assertEquals(rmCase.getAddress().getPostcode(), originalAddress.getPostcode());
    assertEquals(rmCase.getAddress().getRegion(), originalAddress.getRegion());
    assertEquals(rmCase.getAddress().getUprn(), originalAddress.getUprn());

    assertEquals(addressUpdate.getAddressLine1(), newAddress.getAddressLine1());
    assertEquals(addressUpdate.getAddressLine2(), newAddress.getAddressLine2());
    assertEquals(addressUpdate.getAddressLine3(), newAddress.getAddressLine3());
    assertEquals(addressUpdate.getTownName(), newAddress.getTownName());
    assertEquals(addressUpdate.getPostcode(), newAddress.getPostcode());
    assertEquals(rmCase.getAddress().getRegion(), newAddress.getRegion());
    assertEquals(rmCase.getAddress().getUprn(), newAddress.getUprn());
  }

  /** Test request to modify address where caseId not found */
  @Test
  public void modifyAddressByCaseIdNotFound() throws Exception {

    CollectionCase rmCase = collectionCase.get(0);
    String caseId = rmCase.getId();
    AddressChangeDTO addressChange = addressChangeDTO.get(0);

    when(dataRepo.readCollectionCase(caseId)).thenReturn(Optional.empty());

    boolean exceptionThrown = false;
    try {
      caseSvc.modifyAddress(addressChange);
    } catch (CTPException e) {
      assertEquals(CTPException.Fault.RESOURCE_NOT_FOUND, e.getFault());
      exceptionThrown = true;
    }

    verify(dataRepo, times(1)).readCollectionCase(caseId);
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());

    assertTrue(exceptionThrown);
  }

  /** Test request to modify address where caseId does not return matching UPRN */
  @Test
  public void modifyAddressByCaseIdDifferentUPRN() throws Exception {

    CollectionCase rmCase = collectionCase.get(0);
    String caseId = rmCase.getId();
    AddressChangeDTO addressChange = addressChangeDTO.get(0);
    addressChange.getAddress().getUprn().setValue(0L);

    when(dataRepo.readCollectionCase(caseId)).thenReturn(Optional.of(rmCase));

    boolean exceptionThrown = false;
    try {
      caseSvc.modifyAddress(addressChange);
    } catch (CTPException e) {
      assertEquals(CTPException.Fault.BAD_REQUEST, e.getFault());
      exceptionThrown = true;
    }

    verify(dataRepo, times(1)).readCollectionCase(caseId);
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());

    assertTrue(exceptionThrown);
  }

  @Test
  public void createNewCase_withExistingCase() throws Exception {
    CaseRequestDTO request = FixtureHelper.loadClassFixtures(CaseRequestDTO[].class).get(0);
    String uprn = Long.toString(request.getUprn().getValue());

    // There is already an existing case with that uprn
    CollectionCase existingCase = FixtureHelper.loadClassFixtures(CollectionCase[].class).get(0);
    existingCase.getAddress().setUprn(uprn);
    Optional<CollectionCase> existingCaseResult = Optional.of(existingCase);
    when(dataRepo.readLatestCollectionCaseByUprn(eq(uprn))).thenReturn(existingCaseResult);

    // Invoke code under test
    CaseDTO newCase = caseSvc.createNewCase(request);

    // Verify that returned case holds details for the pre-existing case
    AddressLevel expectedAddressLevel = AddressLevel.U;
    testUtil.validateCaseDTO(existingCase, expectedAddressLevel, newCase);

    // Verify nothing written to Firestore and no events sent
    verify(dataRepo, times(0)).writeCollectionCase(any());
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());
  }

  @Test
  public void createNewCase_CE() throws Exception {
    doCreateNewCaseTest(EstabType.HOLIDAY_PARK, AddressType.CE, CaseType.CE, AddressLevel.E);
  }

  @Test
  public void createNewCase_HH() throws Exception {
    doCreateNewCaseTest(EstabType.HOUSEHOLD, AddressType.HH, CaseType.HH, AddressLevel.U);
  }

  @Test
  public void createNewCase_withNoAddressTypeForEstab() throws Exception {
    // In this test the Address type will be used to set the case type
    doCreateNewCaseTest(EstabType.OTHER, AddressType.SPG, CaseType.SPG, AddressLevel.U);
  }

  private void doCreateNewCaseTest(
      EstabType estabType,
      AddressType addressType,
      CaseType expectedCaseType,
      AddressLevel expectedAddressLevel)
      throws Exception {
    CaseRequestDTO request = FixtureHelper.loadClassFixtures(CaseRequestDTO[].class).get(0);
    request.setEstabType(estabType.getCode());
    request.setAddressType(addressType);

    // Invoke code under test
    CaseDTO newCase = caseSvc.createNewCase(request);

    Address expectedAddress = mapperFacade.map(request, Address.class);

    // Verify returned case
    testUtil.validateCaseDTO(expectedCaseType, expectedAddress, expectedAddressLevel, newCase);

    testUtil.verifyCollectionCaseSavedToFirestore(expectedCaseType, expectedAddress);

    testUtil.verifyNewAddressEventSent(
        newCase.getCaseId().toString(), expectedCaseType, expectedAddress);
  }
}
