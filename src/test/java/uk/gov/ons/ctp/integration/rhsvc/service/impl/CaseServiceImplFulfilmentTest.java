package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
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
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;

@RunWith(MockitoJUnitRunner.class)
public class CaseServiceImplFulfilmentTest {

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Mock private ProductReference productReference;

  private List<CollectionCase> collectionCase;
  private SMSFulfilmentRequestDTO smsRequest;
  private PostalFulfilmentRequestDTO postalRequest;

  @Before
  public void setUp() throws Exception {
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.smsRequest = FixtureHelper.loadClassFixtures(SMSFulfilmentRequestDTO[].class).get(0);
    this.postalRequest = FixtureHelper.loadClassFixtures(PostalFulfilmentRequestDTO[].class).get(0);
  }

  // --- fulfilment by SMS

  @Test
  public void shouldFulfilRequestBySmsForHousehold() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.HH, false);
    // Individual case id field should not be set for non-individual
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestBySmsForHouseholdWhenProductReturnsNullIndividual()
      throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.HH, null);
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestBySmsForIndividual() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.HH, true);

    // Individual case id field should be populated as case+product is for an individual
    String individualUuid = eventPayload.getIndividualCaseId();
    assertNotNull(individualUuid);
    assertNotNull(UUID.fromString(individualUuid)); // must be valid UUID
  }

  @Test
  public void shouldFulfilRequestBySmsForIndividualSpecialCase() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.SPG, true);
    assertNull(eventPayload.getIndividualCaseId());
  }

  private FulfilmentRequest doFulfilmentRequestBySMS(Product.CaseType caseType, Boolean individual)
      throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(caseType, individual);
    UUID caseId = UUID.fromString(caseDetails.getId());
    smsRequest.setCaseId(caseId);
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));
    mockProductSearch(caseType, individual, DeliveryChannel.SMS);
    caseSvc.fulfilmentRequestBySMS(smsRequest);
    FulfilmentRequest eventPayload = getAndValidatePublishedEvent(caseDetails);
    assertEquals("07714111222", eventPayload.getContact().getTelNo());
    return eventPayload;
  }

  @Test
  public void shouldRejectSmsFulfilmentForUnknownCase() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.empty());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));
    assertTrue(e.toString(), e.getMessage().contains("Case not found"));
  }

  @Test
  public void shouldRejectSmsFulfilmentForUnknownProduct() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.of(collectionCase.get(0)));
    when(productReference.searchProducts(any())).thenReturn(new ArrayList<>());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));
    assertTrue(e.toString(), e.getMessage().contains("Compatible product cannot be found"));
  }

  // --- fulfilment by post

  @Test
  public void shouldFulfilRequestByPostForHousehold() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.HH, false);

    // Individual case id field should not be set for non-individual
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestByPostForHouseholdWhenProductReturnsNullIndividual()
      throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.HH, null);
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestByPostForIndividual() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.HH, true);

    // Individual case id field should be populated as case+product is for an individual
    String individualUuid = eventPayload.getIndividualCaseId();
    assertNotNull(individualUuid);
    assertNotNull(UUID.fromString(individualUuid)); // must be valid UUID
  }

  @Test
  public void shouldFulfilRequestByPostForIndividualSpecialCase() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.SPG, true);
    assertNull(eventPayload.getIndividualCaseId());
  }

  private FulfilmentRequest doFulfilmentRequestByPost(Product.CaseType caseType, Boolean individual)
      throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(caseType, individual);
    UUID caseId = UUID.fromString(caseDetails.getId());
    postalRequest.setCaseId(caseId);
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));
    mockProductSearch(caseType, individual, DeliveryChannel.POST);
    caseSvc.fulfilmentRequestByPost(postalRequest);
    FulfilmentRequest eventPayload = getAndValidatePublishedEvent(caseDetails);
    Contact contact = eventPayload.getContact();
    assertNotNull(contact);
    assertEquals("Mrs", contact.getTitle());
    assertEquals("Ethel", contact.getForename());
    assertEquals("Brown", contact.getSurname());
    assertNull(contact.getTelNo());
    return eventPayload;
  }

  private void assertRejectPostalFulfilmentForIndividualWithoutFullContact() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, true);
    UUID caseId = UUID.fromString(caseDetails.getId());
    postalRequest.setCaseId(caseId);
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));
    mockProductSearch(Product.CaseType.HH, true, DeliveryChannel.POST);
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(postalRequest));
    assertTrue(
        e.toString(),
        e.getMessage()
            .contains(
                "The fulfilment is for an individual so none of the following fields can be empty"));
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithoutTitle() throws Exception {
    postalRequest.setTitle(null);
    assertRejectPostalFulfilmentForIndividualWithoutFullContact();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithoutForename() throws Exception {
    postalRequest.setForename(null);
    assertRejectPostalFulfilmentForIndividualWithoutFullContact();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithoutSurname() throws Exception {
    postalRequest.setSurname(null);
    assertRejectPostalFulfilmentForIndividualWithoutFullContact();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithEmptyTitle() throws Exception {
    postalRequest.setTitle("");
    assertRejectPostalFulfilmentForIndividualWithoutFullContact();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithEmptyForename() throws Exception {
    postalRequest.setForename("");
    assertRejectPostalFulfilmentForIndividualWithoutFullContact();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithEmptySurname() throws Exception {
    postalRequest.setSurname("");
    assertRejectPostalFulfilmentForIndividualWithoutFullContact();
  }

  @Test
  public void shouldRejectPostalFulfilmentForUnknownCase() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.empty());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(postalRequest));
    assertTrue(e.toString(), e.getMessage().contains("Case not found"));
  }

  @Test
  public void shouldRejectPostalFulfilmentForUnknownProduct() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.of(collectionCase.get(0)));
    when(productReference.searchProducts(any())).thenReturn(new ArrayList<>());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(postalRequest));
    assertTrue(e.toString(), e.getMessage().contains("Compatible product cannot be found"));
  }

  // --- helpers

  private Product createProductForSearch(DeliveryChannel channel) {
    Product product = new Product();
    product.setRequestChannels(Arrays.asList(Product.RequestChannel.RH));
    product.setRegions(Arrays.asList(Product.Region.E));
    product.setDeliveryChannel(channel);
    product.setFulfilmentCode("F1");
    return product;
  }

  private void mockProductSearch(
      Product.CaseType caseType, Boolean individual, DeliveryChannel channel) throws Exception {
    Product expectedSearchProduct = createProductForSearch(channel);
    Product productToReturn = new Product();
    productToReturn.setFulfilmentCode("F1");
    productToReturn.setCaseTypes(Arrays.asList(caseType));
    productToReturn.setIndividual(individual);
    when(productReference.searchProducts(eq(expectedSearchProduct)))
        .thenReturn(Arrays.asList(productToReturn));
  }

  private CollectionCase selectCollectionCaseForTest(
      Product.CaseType caseType, Boolean individual) {
    // Setup case data with required case type
    String caseTypeToSearch = individual != null && individual ? "HI" : "HH";
    CollectionCase caseDetails =
        collectionCase
            .stream()
            .filter(c -> c.getCaseType().equals(caseTypeToSearch))
            .findFirst()
            .get();
    caseDetails.getAddress().setAddressType(caseType.toString());
    return caseDetails;
  }

  private FulfilmentRequest getAndValidatePublishedEvent(CollectionCase caseDetails) {
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
    FulfilmentRequest eventPayload = fulfilmentRequestCaptor.getValue();
    assertEquals("F1", eventPayload.getFulfilmentCode());
    assertEquals(caseDetails.getId(), eventPayload.getCaseId());
    return eventPayload;
  }
}
