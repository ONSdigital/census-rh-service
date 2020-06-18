package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplTest {

  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber("123456");

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Mock private ProductReference productReference;

  private List<CollectionCase> collectionCase;
  private List<AddressChangeDTO> addressChangeDTO;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.addressChangeDTO = FixtureHelper.loadClassFixtures(AddressChangeDTO[].class);
  }

  /** Test returns valid CaseDTO for valid UPRN */
  @Test
  public void getCaseByUPRNFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(collectionCase);

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

  /** Test throws a CTPException where only Invalid Address cases are returned from repository */
  @Test(expected = CTPException.class)
  public void getInvalidAddressCaseByUPRNOnly() throws Exception {

    List<CollectionCase> invalidAddressList = Collections.singletonList(collectionCase.get(0));
    invalidAddressList.get(0).setAddressInvalid(Boolean.TRUE);
    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue()))).thenReturn(invalidAddressList);
    caseSvc.getLatestValidNonHICaseByUPRN(UPRN);
  }

  /** Test throws a CTPException where only HI cases are returned from repository */
  @Test(expected = CTPException.class)
  public void getOnlyHICaseByUPRNOnly() throws Exception {

    List<CollectionCase> invalidAddressList = Collections.singletonList(collectionCase.get(0));
    invalidAddressList.get(0).setCaseType("HI");
    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue()))).thenReturn(invalidAddressList);
    caseSvc.getLatestValidNonHICaseByUPRN(UPRN);
  }

  /** Test retrieves latest case when all valid HH */
  @Test
  public void getLatestCaseByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);

    collectionCase.forEach( cc -> cc.setCaseType("HH"));

    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(2).setCreatedDateTime(earliest);
    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue()))).thenReturn(collectionCase);
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals("Resultant Case created date should match expected case with latest date", UUID.fromString(collectionCase.get(1).getId()), result.getCaseId());
  }

  /** Test retrieves latest valid case when actual latest date is an HI case */
  @Test
  public void getLatestCaseNoneHIByUPRNOnly() throws Exception {

    final Date earliest = new Date();
    final Date mid = DateUtils.addDays(new Date(), 1);
    final Date latest = DateUtils.addDays(new Date(), 2);
    collectionCase.get(0).setCreatedDateTime(mid);
    collectionCase.get(0).setCaseType("HH");
    collectionCase.get(1).setCreatedDateTime(latest);
    collectionCase.get(1).setCaseType("HI");
    collectionCase.get(2).setCreatedDateTime(earliest);
    collectionCase.get(2).setCaseType("HH");
    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue()))).thenReturn(collectionCase);
    CaseDTO result = caseSvc.getLatestValidNonHICaseByUPRN(UPRN);

    assertEquals("Resultant Case created date should match expected case with latest date", UUID.fromString(collectionCase.get(0).getId()), result.getCaseId());
  }





  /** Test Test throws a CTPException where no cases returned from repository */
  @Test(expected = CTPException.class)
  public void getHHCaseByUPRNNotFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Collections.emptyList());

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
  public void testFulfilmentRequestBySMS_Household() throws Exception {
    FulfilmentRequest actualFulfilmentRequest =
        doFulfilmentRequestBySMS(Product.CaseType.HH, false);

    /*
       I am unsure how to proceed here. The removal of CaseType.HI means that
       the *individual* flag needs to permeate into the CaseEndpoint, its related
       services and DTOs. Which was not mentioned on the ticket. Probably what's intended
       but I'm not going to change these APIs without consulting someone else
    */

    // Individual case id field should not be set for non-individual
    assertNull(actualFulfilmentRequest.getIndividualCaseId());
  }

  @Test
  public void testFulfilmentRequestBySMS_Individual() throws Exception {
    FulfilmentRequest actualFulfilmentRequest = doFulfilmentRequestBySMS(Product.CaseType.HH, true);

    // Individual case id field should be populated as case+product is for an individual
    String individualUuid = actualFulfilmentRequest.getIndividualCaseId();
    assertNotNull(individualUuid);
    assertNotNull(UUID.fromString(individualUuid)); // must be valid UUID
  }

  private FulfilmentRequest doFulfilmentRequestBySMS(Product.CaseType caseType, boolean individual)
      throws Exception {
    // Setup case data with required case type
    String caseTypeToSearch = individual ? "HI" : "HH";
    CollectionCase caseDetails =
        collectionCase
            .stream()
            .filter(c -> c.getCaseType().equals(caseTypeToSearch))
            .findFirst()
            .get();
    caseDetails.getAddress().setAddressType(caseType.toString());
    UUID caseId = UUID.fromString(caseDetails.getId());

    // Simulate firestore
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    // Create example product that we expect the product search to be called with
    Product expectedSearchProduct = new Product();
    expectedSearchProduct.setRequestChannels(Arrays.asList(Product.RequestChannel.RH));
    expectedSearchProduct.setRegions(Arrays.asList(Product.Region.E));
    expectedSearchProduct.setDeliveryChannel(DeliveryChannel.SMS);
    expectedSearchProduct.setFulfilmentCode("F1");

    // Simulate the behaviour of the ProductReference
    Product productToReturn = new Product();
    productToReturn.setFulfilmentCode("F1");
    productToReturn.setCaseTypes(Arrays.asList(caseType));
    productToReturn.setIndividual(individual);
    List<Product> foundProducts = new ArrayList<>();
    foundProducts.add(productToReturn);
    when(productReference.searchProducts(eq(expectedSearchProduct))).thenReturn(foundProducts);

    // Build request body
    SMSFulfilmentRequestDTO requestBodyDTO = new SMSFulfilmentRequestDTO();
    requestBodyDTO.setCaseId(caseId);
    requestBodyDTO.setTelNo("07714111222");
    requestBodyDTO.setFulfilmentCode("F1");
    requestBodyDTO.setDateTime(new Date());

    // Invoke method under test
    caseSvc.fulfilmentRequestBySMS(requestBodyDTO);

    // Grab the published event
    ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
    ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
    ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
    ArgumentCaptor<FulfilmentRequest> fulfilmentRequestCaptor =
        ArgumentCaptor.forClass(FulfilmentRequest.class);
    verify(eventPublisher)
        .sendEvent(
            eventTypeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            fulfilmentRequestCaptor.capture());

    // Validate message routing
    assertEquals("FULFILMENT_REQUESTED", eventTypeCaptor.getValue().toString());
    assertEquals("RESPONDENT_HOME", sourceCaptor.getValue().toString());
    assertEquals("RH", channelCaptor.getValue().toString());

    // Validate content of generated event
    FulfilmentRequest actualFulfilmentRequest = fulfilmentRequestCaptor.getValue();
    assertEquals("F1", actualFulfilmentRequest.getFulfilmentCode());
    assertEquals(caseDetails.getId(), actualFulfilmentRequest.getCaseId());
    assertEquals("07714111222", actualFulfilmentRequest.getContact().getTelNo());

    return actualFulfilmentRequest;
  }

  @Test
  public void fulfilmentRequestBySMS_unknownCase() throws Exception {
    // Simulate firestore not finding the case
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.empty());

    // Build request
    SMSFulfilmentRequestDTO requestBodyDTO = new SMSFulfilmentRequestDTO();
    requestBodyDTO.setCaseId(UUID.fromString(collectionCase.get(0).getId()));

    // Invoke method under test
    boolean caughtException = false;
    try {
      caseSvc.fulfilmentRequestBySMS(requestBodyDTO);
    } catch (Exception e) {
      caughtException = true;
      assertTrue(e.toString(), e.getMessage().contains("Case not found"));
    }
    assertTrue(caughtException);
  }

  @Test
  public void fulfilmentRequestBySMS_unknownProduct() throws Exception {
    // Simulate firestore
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.of(collectionCase.get(0)));

    // Simulate ProductReference not finding a product
    when(productReference.searchProducts(any())).thenReturn(new ArrayList<>());

    // Build request body
    SMSFulfilmentRequestDTO requestBodyDTO = new SMSFulfilmentRequestDTO();
    requestBodyDTO.setFulfilmentCode("XYZ");
    requestBodyDTO.setCaseId(UUID.randomUUID());

    // Invoke method under test
    boolean caughtException = false;
    try {
      caseSvc.fulfilmentRequestBySMS(requestBodyDTO);
    } catch (Exception e) {
      caughtException = true;
      assertTrue(e.toString(), e.getMessage().contains("Compatible product cannot be found"));
    }
    assertTrue(caughtException);
  }
}
