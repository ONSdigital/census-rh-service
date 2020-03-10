package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressModified;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

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

    MockitoAnnotations.initMocks(this);
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.addressChangeDTO = FixtureHelper.loadClassFixtures(AddressChangeDTO[].class);
  }

  /** Test returns valid CaseDTO for valid UPRN */
  @Test
  public void getHHCaseByUPRNFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(collectionCase);

    CollectionCase hhCase = this.collectionCase.get(0);

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);
    CaseDTO rmCase = caseDTO.get(0);

    assertThat(caseDTO, hasSize(1));
    assertEquals(hhCase.getId(), rmCase.getCaseId().toString());
    assertEquals(hhCase.getCaseRef(), rmCase.getCaseRef());
    assertEquals(hhCase.getCaseType(), rmCase.getCaseType());
    assertEquals(hhCase.getAddress().getAddressType(), rmCase.getAddressType());
    assertEquals(hhCase.getAddress().getAddressLine1(), rmCase.getAddress().getAddressLine1());
    assertEquals(hhCase.getAddress().getAddressLine2(), rmCase.getAddress().getAddressLine2());
    assertEquals(hhCase.getAddress().getAddressLine3(), rmCase.getAddress().getAddressLine3());
    assertEquals(hhCase.getAddress().getTownName(), rmCase.getAddress().getTownName());
    assertEquals(hhCase.getAddress().getRegion(), rmCase.getRegion());
    assertEquals(hhCase.getAddress().getPostcode(), rmCase.getAddress().getPostcode());
    assertEquals(
        hhCase.getAddress().getUprn(), Long.toString(rmCase.getAddress().getUprn().getValue()));
  }

  /** Test returns empty list where only non HH cases returned from repository */
  @Test
  public void getHHCaseByUPRNHICasesOnly() throws Exception {

    List<CollectionCase> nonHHCases =
        collectionCase
            .stream()
            .filter(c -> !c.getCaseType().equals(Product.CaseType.HH.name()))
            .collect(Collectors.toList());

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue()))).thenReturn(nonHHCases);

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);

    assertThat(nonHHCases, hasSize(2));
    assertThat(caseDTO, hasSize(0));
  }

  /** Test returns empty list where no cases returned from repository */
  @Test
  public void getHHCaseByUPRNNotFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Collections.emptyList());

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);

    assertThat(caseDTO, hasSize(0));
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
    AddressModified originalAddress = payload.getOriginalAddress();
    AddressModified newAddress = payload.getNewAddress();
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
    assertEquals(rmCase.getAddress().getArid(), originalAddress.getArid());

    assertEquals(addressUpdate.getAddressLine1(), newAddress.getAddressLine1());
    assertEquals(addressUpdate.getAddressLine2(), newAddress.getAddressLine2());
    assertEquals(addressUpdate.getAddressLine3(), newAddress.getAddressLine3());
    assertEquals(addressUpdate.getTownName(), newAddress.getTownName());
    assertEquals(addressUpdate.getPostcode(), newAddress.getPostcode());
    assertEquals(rmCase.getAddress().getRegion(), newAddress.getRegion());
    assertEquals(rmCase.getAddress().getUprn(), newAddress.getUprn());
    assertEquals(rmCase.getAddress().getArid(), newAddress.getArid());
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
