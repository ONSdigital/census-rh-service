package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import ma.glasnost.orika.MapperFacade;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
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
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.config.RateLimiterConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(
    classes = {WebformServiceImpl.class, AppConfig.class, ValidationAutoConfiguration.class})
public class CaseServiceImplFulfilmentTest {

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private AppConfig appConfig;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Mock private RateLimiterClient rateLimiterClient;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  @Mock private ProductReference productReference;

  @Mock private CallNotPermittedException circuitBreakerOpenException;

  @Captor private ArgumentCaptor<Product> productCaptor;

  private List<CollectionCase> collectionCase;
  private SMSFulfilmentRequestDTO smsRequest;
  private PostalFulfilmentRequestDTO postalRequest;

  @Before
  public void setUp() throws Exception {
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.smsRequest = FixtureHelper.loadClassFixtures(SMSFulfilmentRequestDTO[].class).get(0);
    this.postalRequest = FixtureHelper.loadClassFixtures(PostalFulfilmentRequestDTO[].class).get(0);
    when(appConfig.getRateLimiter()).thenReturn(rateLimiterConfig(true));
  }

  private RateLimiterConfig rateLimiterConfig(boolean enabled) {
    RateLimiterConfig rateLimiterConfig = new RateLimiterConfig();
    rateLimiterConfig.setEnabled(enabled);
    return rateLimiterConfig;
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
  public void shouldFulfilRequestBySmsForIndividualWhereProductHasMultipleCaseTypes()
      throws Exception {
    CollectionCase caseDetails = collectionCase.get(0);
    FulfilmentRequest eventPayload =
        doFulfilmentRequestBySMS(true, caseDetails, Product.CaseType.CE, Product.CaseType.HH);

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

  @Test
  public void shouldFulfilRequestBySmsForIndividualCE() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestBySMS(Product.CaseType.CE, true);
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestBySmsForIndividualCeWhereProductHasMultipleCaseTypes()
      throws Exception {
    CollectionCase caseDetails = collectionCase.get(0);
    caseDetails.getAddress().setAddressType(Product.CaseType.CE.toString());
    caseDetails.setCaseType(Product.CaseType.CE.toString());
    FulfilmentRequest eventPayload =
        doFulfilmentRequestBySMS(true, caseDetails, Product.CaseType.CE, Product.CaseType.HH);
    assertNull(eventPayload.getIndividualCaseId());
  }

  private FulfilmentRequest doFulfilmentRequestBySMS(Product.CaseType caseType, Boolean individual)
      throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(caseType, individual);
    return doFulfilmentRequestBySMS(individual, caseDetails, caseType);
  }

  private FulfilmentRequest doFulfilmentRequestBySMS(
      Boolean individual, CollectionCase caseDetails, Product.CaseType... caseTypes)
      throws Exception {
    UUID caseId = UUID.fromString(caseDetails.getId());
    smsRequest.setCaseId(caseId);
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));
    mockProductSearch("F1", individual, DeliveryChannel.SMS, caseTypes);
    caseSvc.fulfilmentRequestBySMS(smsRequest);

    String phoneNo = "07714111222";
    verifyRateLimiterCall(1, phoneNo, smsRequest.getClientIP(), caseDetails);
    Contact contact = new Contact();
    contact.setTelNo(phoneNo);
    FulfilmentRequest eventPayload =
        getAndValidatePublishedEvent(caseDetails, contact, "F1").get(0);
    return eventPayload;
  }

  @Test
  public void shouldRejectSmsFulfilmentForUnknownCase() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.empty());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));
    assertTrue(e.toString(), e.getMessage().contains("Case not found"));
    verifyRateLimiterNotCalled();
  }

  @Test
  public void shouldRejectSmsFulfilmentForUnknownProduct() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.of(collectionCase.get(0)));
    when(productReference.searchProducts(any())).thenReturn(new ArrayList<>());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));
    assertTrue(e.toString(), e.getMessage().contains("Compatible product cannot be found"));
    verifyRateLimiterNotCalled();
  }

  // --- fulfilment by post

  @Test
  public void shouldFulfilRequestByPostForHousehold() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.HH, false, "Mrs");

    // Individual case id field should not be set for non-individual
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestByPostForHouseholdWhenProductReturnsNullIndividual()
      throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.HH, null, "Mrs");
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestByPostForHouseholdWithNullTitle() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.HH, null, null);
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestByPostForIndividual() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.HH, true, "Mrs");

    // Individual case id field should be populated as case+product is for an individual
    String individualUuid = eventPayload.getIndividualCaseId();
    assertNotNull(individualUuid);
    assertNotNull(UUID.fromString(individualUuid)); // must be valid UUID
  }

  @Test
  public void shouldFulfilRequestByPostForIndividualWhereProductHasMultipleCaseTypes()
      throws Exception {
    CollectionCase caseDetails = collectionCase.get(0);
    FulfilmentRequest eventPayload =
        doFulfilmentRequestByPost(
            true, caseDetails, "Mr", Product.CaseType.CE, Product.CaseType.HH);

    // Individual case id field should be populated as case+product is for an individual
    String individualUuid = eventPayload.getIndividualCaseId();
    assertNotNull(individualUuid);
    assertNotNull(UUID.fromString(individualUuid)); // must be valid UUID
  }

  @Test
  public void shouldFulfilRequestByPostForIndividualSpecialCase() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.SPG, true, "Mrs");
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestByPostForIndividualCE() throws Exception {
    FulfilmentRequest eventPayload = doFulfilmentRequestByPost(Product.CaseType.CE, true, "Mr");
    assertNull(eventPayload.getIndividualCaseId());
  }

  @Test
  public void shouldFulfilRequestByPostForIndividualCeWhereProductHasMultipleCaseTypes()
      throws Exception {
    CollectionCase caseDetails = collectionCase.get(0);
    caseDetails.getAddress().setAddressType(Product.CaseType.CE.toString());
    caseDetails.setCaseType(Product.CaseType.CE.toString());
    FulfilmentRequest eventPayload =
        doFulfilmentRequestByPost(
            true, caseDetails, "Mr", Product.CaseType.CE, Product.CaseType.HH);
    assertNull(eventPayload.getIndividualCaseId());
  }

  private FulfilmentRequest doFulfilmentRequestByPost(
      Product.CaseType caseType, Boolean individual, String requestorsTitle) throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(caseType, individual);
    return doFulfilmentRequestByPost(individual, caseDetails, requestorsTitle, caseType);
  }

  private FulfilmentRequest doFulfilmentRequestByPost(
      Boolean individual,
      CollectionCase caseDetails,
      String requestorsTitle,
      Product.CaseType... caseTypes)
      throws Exception {
    UUID caseId = UUID.fromString(caseDetails.getId());
    postalRequest.setCaseId(caseId);
    postalRequest.setTitle(requestorsTitle);
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));
    mockProductSearch("F1", individual, DeliveryChannel.POST, caseTypes);
    caseSvc.fulfilmentRequestByPost(postalRequest);

    verifyRateLimiterCall(1, null, postalRequest.getClientIP(), caseDetails);
    Contact contact = new Contact();
    contact.setTitle(requestorsTitle);
    contact.setForename("Ethel");
    contact.setSurname("Brown");
    FulfilmentRequest eventPayload =
        getAndValidatePublishedEvent(caseDetails, contact, "F1").get(0);
    return eventPayload;
  }

  private void assertRejectPostalFulfilmentForIndividualWithoutContactName() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, true);
    UUID caseId = UUID.fromString(caseDetails.getId());
    postalRequest.setCaseId(caseId);
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));
    mockProductSearch("F1", true, DeliveryChannel.POST, Product.CaseType.HH);
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(postalRequest));
    assertTrue(
        e.toString(),
        e.getMessage()
            .contains(
                "The fulfilment is for an individual so none of the following fields can be empty"));
    verifyRateLimiterNotCalled();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithoutForename() throws Exception {
    postalRequest.setForename(null);
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithoutSurname() throws Exception {
    postalRequest.setSurname(null);
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithEmptyForename() throws Exception {
    postalRequest.setForename("");
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentForIndividualWithEmptySurname() throws Exception {
    postalRequest.setSurname("");
    assertRejectPostalFulfilmentForIndividualWithoutContactName();
  }

  @Test
  public void shouldRejectPostalFulfilmentForUnknownCase() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.empty());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(postalRequest));
    assertTrue(e.toString(), e.getMessage().contains("Case not found"));
    verifyRateLimiterNotCalled();
  }

  @Test
  public void shouldRejectPostalFulfilmentForUnknownProduct() throws Exception {
    when(dataRepo.readCollectionCase(any())).thenReturn(Optional.of(collectionCase.get(0)));
    when(productReference.searchProducts(any())).thenReturn(new ArrayList<>());
    CTPException e =
        assertThrows(CTPException.class, () -> caseSvc.fulfilmentRequestByPost(postalRequest));
    assertTrue(e.toString(), e.getMessage().contains("Compatible product cannot be found"));
    verifyRateLimiterNotCalled();
  }

  // --- multi postal fulfilment tests

  @Test
  public void shouldFulfilRequestByPostForMultipleFulfilmentCodes() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, false);
    UUID caseId = UUID.fromString(caseDetails.getId());
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    postalRequest.setCaseId(caseId);
    postalRequest.setTitle("Mrs");
    postalRequest.setFulfilmentCodes(Arrays.asList("F1", "F2", "F3"));

    Product p1 = mockProductSearch("F1", false, DeliveryChannel.POST, Product.CaseType.HH);
    Product p2 = mockProductSearch("F2", false, DeliveryChannel.POST, Product.CaseType.HH);
    Product p3 = mockProductSearch("F3", false, DeliveryChannel.POST, Product.CaseType.HH);

    caseSvc.fulfilmentRequestByPost(postalRequest);

    Contact contact = new Contact();
    contact.setTitle("Mrs");
    contact.setForename("Ethel");
    contact.setSurname("Brown");
    getAndValidatePublishedEvent(caseDetails, contact, "F1", "F2", "F3");

    verifyRateLimiterCall(3, null, postalRequest.getClientIP(), caseDetails);

    assertEquals(p1, productCaptor.getAllValues().get(0));
    assertEquals(p2, productCaptor.getAllValues().get(1));
    assertEquals(p3, productCaptor.getAllValues().get(2));
  }

  // simulate RHUI continuation pages using same fulfilment code.
  @Test
  public void shouldFulfilRequestByPostForMultipleRepeatedFulfilmentCodes() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, false);
    UUID caseId = UUID.fromString(caseDetails.getId());
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    postalRequest.setCaseId(caseId);
    postalRequest.setTitle("Mrs");
    postalRequest.setFulfilmentCodes(Arrays.asList("F1", "CLONE", "CLONE", "CLONE"));

    Product p1 = mockProductSearch("F1", false, DeliveryChannel.POST, Product.CaseType.HH);
    Product p2 = mockProductSearch("CLONE", false, DeliveryChannel.POST, Product.CaseType.HH);

    caseSvc.fulfilmentRequestByPost(postalRequest);

    Contact contact = new Contact();
    contact.setTitle("Mrs");
    contact.setForename("Ethel");
    contact.setSurname("Brown");
    getAndValidatePublishedEvent(caseDetails, contact, "F1", "CLONE", "CLONE", "CLONE");

    verifyRateLimiterCall(4, null, postalRequest.getClientIP(), caseDetails);

    assertEquals(p1, productCaptor.getAllValues().get(0));
    assertEquals(p2, productCaptor.getAllValues().get(1));
    assertEquals(p2, productCaptor.getAllValues().get(2));
    assertEquals(p2, productCaptor.getAllValues().get(3));
  }

  @Test
  public void shouldRejectPostalFulfilmentWhenRateLimiterRejects() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, false);
    UUID caseId = UUID.fromString(caseDetails.getId());
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
        .when(rateLimiterClient).checkFulfilmentRateLimit(any(), any(), any(), any(), any(), any());
 
    postalRequest.setCaseId(caseId);
    mockProductSearch("F1", false, DeliveryChannel.POST, Product.CaseType.HH);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> caseSvc.fulfilmentRequestByPost(postalRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
    verifyRateLimiterCall(1, null, postalRequest.getClientIP(), caseDetails);
  }

  // multi sms fulfilment tests

  @Test
  public void shouldFulfilRequestBySmsForMultipleFulfilmentCodes() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, false);
    UUID caseId = UUID.fromString(caseDetails.getId());
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    String phoneNo = "07714111222";

    smsRequest.setTelNo(phoneNo);
    smsRequest.setCaseId(caseId);
    smsRequest.setFulfilmentCodes(Arrays.asList("F1", "F2", "F3"));

    Product p1 = mockProductSearch("F1", false, DeliveryChannel.SMS, Product.CaseType.HH);
    Product p2 = mockProductSearch("F2", false, DeliveryChannel.SMS, Product.CaseType.HH);
    Product p3 = mockProductSearch("F3", false, DeliveryChannel.SMS, Product.CaseType.HH);

    caseSvc.fulfilmentRequestBySMS(smsRequest);

    Contact contact = new Contact();
    contact.setTelNo(phoneNo);
    getAndValidatePublishedEvent(caseDetails, contact, "F1", "F2", "F3");

    verifyRateLimiterCall(3, phoneNo, smsRequest.getClientIP(), caseDetails);

    assertEquals(p1, productCaptor.getAllValues().get(0));
    assertEquals(p2, productCaptor.getAllValues().get(1));
    assertEquals(p3, productCaptor.getAllValues().get(2));
  }

  @Test
  public void shouldRejectSmsFulfilmentWhenRateLimiterRejects() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, false);
    UUID caseId = UUID.fromString(caseDetails.getId());
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
        .when(rateLimiterClient).checkFulfilmentRateLimit(any(), any(), any(), any(), any(), any());

    String phoneNo = "07714111222";
    smsRequest.setCaseId(caseId);
    smsRequest.setTelNo(phoneNo);
    mockProductSearch("F1", false, DeliveryChannel.SMS, Product.CaseType.HH);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> caseSvc.fulfilmentRequestBySMS(smsRequest));

    assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    verify(eventPublisher, never()).sendEvent(any(), any(), any(), any());
    verifyRateLimiterCall(1, phoneNo, smsRequest.getClientIP(), caseDetails);
  }

  // --- check when rate limiter turned off

  @Test
  public void shouldFulfilRequestByPostWhenRateLimiterNotEnabled() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, false);
    UUID caseId = UUID.fromString(caseDetails.getId());
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    postalRequest.setCaseId(caseId);
    postalRequest.setTitle("Mrs");
    postalRequest.setFulfilmentCodes(Arrays.asList("F1"));

    mockProductSearch("F1", false, DeliveryChannel.POST, Product.CaseType.HH);
    when(appConfig.getRateLimiter()).thenReturn(rateLimiterConfig(false));

    caseSvc.fulfilmentRequestByPost(postalRequest);
    verifyRateLimiterNotCalled();
  }

  @Test
  public void shouldFulfilRequestBySmsWhenRateLimiterNotEnabled() throws Exception {
    CollectionCase caseDetails = selectCollectionCaseForTest(Product.CaseType.HH, false);
    UUID caseId = UUID.fromString(caseDetails.getId());
    when(dataRepo.readCollectionCase(eq(caseId.toString()))).thenReturn(Optional.of(caseDetails));

    String phoneNo = "07714111222";

    smsRequest.setTelNo(phoneNo);
    smsRequest.setCaseId(caseId);
    smsRequest.setFulfilmentCodes(Arrays.asList("F1"));

    mockProductSearch("F1", false, DeliveryChannel.SMS, Product.CaseType.HH);
    when(appConfig.getRateLimiter()).thenReturn(rateLimiterConfig(false));

    caseSvc.fulfilmentRequestBySMS(smsRequest);
    verifyRateLimiterNotCalled();
  }

  // --- helpers

  private void verifyRateLimiterCall(
      int numTimes, String phoneNo, String clientIp, CollectionCase caseDetails) throws Exception {
    UniquePropertyReferenceNumber uprn =
        UniquePropertyReferenceNumber.create(caseDetails.getAddress().getUprn());
    verify(rateLimiterClient, times(numTimes))
        .checkFulfilmentRateLimit(
            eq(Domain.RH),
            productCaptor.capture(),
            eq(CaseType.valueOf(caseDetails.getCaseType())),
            eq(clientIp),
            eq(uprn),
            eq(phoneNo));
  }

  private void verifyRateLimiterNotCalled() throws Exception {
    verify(rateLimiterClient, never())
        .checkFulfilmentRateLimit(any(), any(), any(), any(), any(), any());
  }

  private Product createProductForSearch(String fulfilmentCode, DeliveryChannel channel) {
    Product product = new Product();
    product.setRequestChannels(Arrays.asList(Product.RequestChannel.RH));
    product.setRegions(Arrays.asList(Product.Region.E));
    product.setDeliveryChannel(channel);
    product.setFulfilmentCode(fulfilmentCode);
    return product;
  }

  private Product mockProductSearch(
      String fulfilmentCode,
      Boolean individual,
      DeliveryChannel channel,
      Product.CaseType... caseTypes)
      throws Exception {
    Product expectedSearchProduct = createProductForSearch(fulfilmentCode, channel);
    Product productToReturn = new Product();
    productToReturn.setFulfilmentCode(fulfilmentCode);
    productToReturn.setCaseTypes(Arrays.asList(caseTypes));
    productToReturn.setIndividual(individual);
    when(productReference.searchProducts(eq(expectedSearchProduct)))
        .thenReturn(Arrays.asList(productToReturn));
    return productToReturn;
  }

  private CollectionCase selectCollectionCaseForTest(
      Product.CaseType caseType, Boolean individual) {
    // Setup case data with required case type
    String caseTypeToSearch = individual != null && individual ? "HI" : "HH";
    CollectionCase caseDetails =
        collectionCase.stream()
            .filter(c -> c.getCaseType().equals(caseTypeToSearch))
            .findFirst()
            .get();
    caseDetails.getAddress().setAddressType(caseType.toString());
    caseDetails.setCaseType(caseType.toString());
    return caseDetails;
  }

  private List<FulfilmentRequest> getAndValidatePublishedEvent(
      CollectionCase caseDetails, Contact expectedContact, String... fulfilmentCodes) {
    ArgumentCaptor<EventType> eventTypeCaptor = ArgumentCaptor.forClass(EventType.class);
    ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
    ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
    ArgumentCaptor<FulfilmentRequest> fulfilmentRequestCaptor =
        ArgumentCaptor.forClass(FulfilmentRequest.class);

    verify(eventPublisher, Mockito.times(fulfilmentCodes.length))
        .sendEvent(
            eventTypeCaptor.capture(),
            sourceCaptor.capture(),
            channelCaptor.capture(),
            fulfilmentRequestCaptor.capture());

    List<FulfilmentRequest> events = new ArrayList<>();

    for (int i = 0; i < fulfilmentCodes.length; i++) {
      // Validate message routing
      assertEquals("FULFILMENT_REQUESTED", eventTypeCaptor.getAllValues().get(i).toString());
      assertEquals("RESPONDENT_HOME", sourceCaptor.getAllValues().get(i).toString());
      assertEquals("RH", channelCaptor.getAllValues().get(i).toString());

      // Validate content of generated event
      FulfilmentRequest eventPayload = fulfilmentRequestCaptor.getAllValues().get(i);
      assertEquals(fulfilmentCodes[i], eventPayload.getFulfilmentCode());
      assertEquals(caseDetails.getId(), eventPayload.getCaseId());
      assertEquals(expectedContact, eventPayload.getContact());
      events.add(eventPayload);
    }
    return events;
  }
}
