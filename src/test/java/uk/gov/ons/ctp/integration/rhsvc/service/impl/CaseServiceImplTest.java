package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

public class CaseServiceImplTest {

  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber("123456");

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Mock private ProductReference productReference;

  @Mock EventPublisher publisher;

  private List<CollectionCase> collectionCase;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
  }

  /** Test returns valid JSON for valid UPRN */
  @Test
  public void getHHCaseByUPRNFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(collectionCase);

    CollectionCase hhCase = this.collectionCase.get(0);

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);
    CaseDTO rmCase = caseDTO.get(0);

    assertThat(caseDTO, hasSize(1));
    assertEquals(hhCase.getId(), rmCase.getId().toString());
    assertEquals(hhCase.getCaseRef(), rmCase.getCaseRef());
    assertEquals(hhCase.getState(), rmCase.getState());
    assertEquals(hhCase.getAddress().getAddressType(), rmCase.getAddressType());
    assertEquals(hhCase.getAddress().getAddressLine1(), rmCase.getAddressLine1());
    assertEquals(hhCase.getAddress().getAddressLine2(), rmCase.getAddressLine2());
    assertEquals(hhCase.getAddress().getAddressLine3(), rmCase.getAddressLine3());
    assertEquals(hhCase.getAddress().getTownName(), rmCase.getTownName());
    assertEquals(hhCase.getAddress().getRegion(), rmCase.getRegion());
    assertEquals(hhCase.getAddress().getPostcode(), rmCase.getPostcode());
    assertEquals(hhCase.getAddress().getUprn(), Long.toString(rmCase.getUprn().getValue()));
  }

  /** Test returns empty list where only non HH cases returned from repository */
  @Test
  public void getHHCaseByUPRNHICasesOnly() throws Exception {

    List<CollectionCase> nonHHCases =
        collectionCase
            .stream()
            .filter(c -> !c.getAddress().getAddressType().equals(Product.CaseType.HH.name()))
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

  @Test
  public void testFulfilmentRequestBySMS_Household() throws Exception {
    FulfilmentRequest actualFulfilmentRequest = doFulfilmentRequestBySMS(Product.CaseType.HH);

    // Individual case id field should not be set for non-individual
    assertNull(actualFulfilmentRequest.getIndividualCaseId());
  }

  @Test
  public void testFulfilmentRequestBySMS_Individual() throws Exception {
    FulfilmentRequest actualFulfilmentRequest = doFulfilmentRequestBySMS(Product.CaseType.HI);

    // Individual case id field should be populated as case+product is for an individual
    String individualUuid = actualFulfilmentRequest.getIndividualCaseId();
    assertNotNull(individualUuid);
    assertNotNull(UUID.fromString(individualUuid.toString())); // must be valid UUID
  }

  private FulfilmentRequest doFulfilmentRequestBySMS(Product.CaseType caseType) throws Exception {
    // Setup case data with required case type
    CollectionCase caseDetails = collectionCase.get(0);
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
    Product product = new Product();
    product.setCaseType(Product.CaseType.HH);
    product.setFulfilmentCode("F1");
    product.setCaseType(caseType);
    List<Product> foundProducts = new ArrayList<>();
    foundProducts.add(product);
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
    verify(publisher)
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
