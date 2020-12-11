package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static java.util.stream.Collectors.toList;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient;
import uk.gov.ons.ctp.integration.ratelimiter.client.RateLimiterClient.Domain;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.FulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.PostalFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** Implementation to deal with Case data */
@Service
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  @Autowired private AppConfig appConfig;
  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private MapperFacade mapperFacade;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private ProductReference productReference;
  @Autowired private RateLimiterClient rateLimiterClient;
  @Autowired private CircuitBreaker circuitBreaker;

  @Override
  public CaseDTO getLatestValidNonHICaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {

    Optional<CollectionCase> caseFound =
        dataRepo.readNonHILatestValidCollectionCaseByUprn(Long.toString(uprn.getValue()));
    if (caseFound.isPresent()) {
      log.with("case", caseFound.get().getId())
          .with("uprn", uprn)
          .debug("non HI latest valid case retrieved for UPRN");
      return mapperFacade.map(caseFound.get(), CaseDTO.class);
    } else {
      log.with("uprn", uprn).warn("No cases returned for uprn");
      throw new CTPException(Fault.RESOURCE_NOT_FOUND, "Failed to retrieve Case");
    }
  }

  @Override
  public CaseDTO createNewCase(CaseRequestDTO request) throws CTPException {

    Optional<CaseDTO> existingCase = getLatestCaseByUPRN(request.getUprn());

    CaseDTO caseToReturn;
    if (existingCase.isPresent()) {
      // Don't need to create a new case, as we found one with the same UPRN
      caseToReturn = existingCase.get();
    } else {
      // Create a new case as not found for the UPRN in Firestore
      CaseType caseType = ServiceUtil.determineCaseType(request);
      CollectionCase newCase =
          ServiceUtil.createCase(request, caseType, appConfig.getCollectionExerciseId());
      log.with("caseId", newCase.getId())
          .with("primaryCaseType", caseType)
          .debug("Created new case");

      // Store new case in Firestore
      dataRepo.writeCollectionCase(newCase);

      // tell RM we have created a case for the selected (HH|CE|SPG) address
      ServiceUtil.sendNewAddressEvent(eventPublisher, newCase);

      caseToReturn = mapperFacade.map(newCase, CaseDTO.class);
    }

    return caseToReturn;
  }

  @Override
  public CaseDTO modifyAddress(final AddressChangeDTO addressChanges) throws CTPException {

    String caseId = addressChanges.getCaseId().toString();

    Optional<CollectionCase> caseMatch = dataRepo.readCollectionCase(caseId);

    if (caseMatch.isEmpty()) {
      log.with("caseId", caseId).warn("Failed to retrieve Case from storage");
      throw new CTPException(
          CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve Case for caseId: {}", caseId);
    }

    CollectionCase rmCase = caseMatch.get();

    CaseDTO caseData = createModifiedAddressCaseDetails(caseId, rmCase, addressChanges);

    AddressCompact originalAddress = mapperFacade.map(rmCase.getAddress(), AddressCompact.class);
    AddressCompact updatedAddress = mapperFacade.map(rmCase.getAddress(), AddressCompact.class);

    mapperFacade.map(addressChanges.getAddress(), updatedAddress);

    sendAddressModifiedEvent(caseId, originalAddress, updatedAddress);

    return caseData;
  }

  /**
   * Create case details with updated Address
   *
   * @param caseId requested caseId for which to update address
   * @param rmCase original Case from repository
   * @param addressChanges Changed address details from request
   * @return CaseDTO updated case details
   * @throws CTPException UPRN of stored address and request change do not match
   */
  private CaseDTO createModifiedAddressCaseDetails(
      String caseId, CollectionCase rmCase, AddressChangeDTO addressChanges) throws CTPException {

    CaseDTO caseData = mapperFacade.map(rmCase, CaseDTO.class);
    if (!caseData.getAddress().getUprn().equals(addressChanges.getAddress().getUprn())) {
      log.with("caseId", caseId)
          .with("uprn", addressChanges.getAddress().getUprn().toString())
          .warn("The UPRN of the referenced Case and the provided Address UPRN must be matching");
      throw new CTPException(
          CTPException.Fault.BAD_REQUEST,
          "The UPRN of the referenced Case and the provided Address UPRN must be matching");
    }
    caseData.setAddress(addressChanges.getAddress());
    return caseData;
  }

  /**
   * Send AddressModified event
   *
   * @param caseId of updated case
   * @param originalAddress details of case
   * @param newAddress details of case
   */
  private void sendAddressModifiedEvent(
      String caseId, AddressCompact originalAddress, AddressCompact newAddress) {

    log.with("caseId", caseId)
        .with("uprn", originalAddress.getUprn())
        .debug("Generating AddressModified event");

    AddressModification addressModification =
        AddressModification.builder()
            .collectionCase(new CollectionCaseCompact(UUID.fromString(caseId)))
            .originalAddress(originalAddress)
            .newAddress(newAddress)
            .build();

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_MODIFIED, Source.RESPONDENT_HOME, Channel.RH, addressModification);

    log.with("caseId", caseId)
        .with("transactionId", transactionId)
        .debug("AddressModified event published");
  }

  /**
   * This method contains the business logic for submitting a fulfilment by SMS request.
   *
   * @param requestBodyDTO contains the parameters from the originating http POST request.
   * @throws CTPException if the specified case cannot be found, or if no matching product is found.
   */
  @Override
  public void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException {
    Contact contact = new Contact();
    contact.setTelNo(requestBodyDTO.getTelNo());
    CollectionCase caseDetails = findCaseDetails(requestBodyDTO.getCaseId());
    var products = createProductList(DeliveryChannel.SMS, requestBodyDTO, caseDetails);
    recordRateLimiting(contact, requestBodyDTO.getClientIP(), products, caseDetails);
    createAndSendFulfilments(DeliveryChannel.SMS, contact, requestBodyDTO, products, caseDetails);
  }

  @Override
  public void fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());
    CollectionCase caseDetails = findCaseDetails(requestBodyDTO.getCaseId());
    var products = createProductList(DeliveryChannel.POST, requestBodyDTO, caseDetails);
    preValidatePostalContactDetails(products, contact);
    recordRateLimiting(contact, requestBodyDTO.getClientIP(), products, caseDetails);
    createAndSendFulfilments(DeliveryChannel.POST, contact, requestBodyDTO, products, caseDetails);
  }

  /*
   * create a cached list of product information to use for both
   * rate-limiting and event generation.
   * this prevents multiple calls to repeat getting products details.
   * NOTE: must return list in order of fulfilmentCodes
   */
  private List<Product> createProductList(
      DeliveryChannel deliveryChannel, FulfilmentRequestDTO request, CollectionCase caseDetails)
      throws CTPException {
    Map<String, Product> map = new HashMap<>();
    Region region = Region.valueOf(caseDetails.getAddress().getRegion());
    for (String fulfilmentCode : new HashSet<>(request.getFulfilmentCodes())) {
      map.put(fulfilmentCode, findProduct(fulfilmentCode, deliveryChannel, region));
    }
    return request.getFulfilmentCodes().stream().map(fc -> map.get(fc)).collect(toList());
  }

  private void preValidatePostalContactDetails(List<Product> products, Contact contact)
      throws CTPException {
    for (Product product : products) {
      if (isIndividual(product)) {
        validateContactName(contact);
      }
    }
  }

  private void recordRateLimiting(
      Contact contact, String ipAddress, List<Product> products, CollectionCase caseDetails) {
    if (appConfig.getRateLimiter().isEnabled()) {
      for (Product product : products) {
        log.with("fulfilmentCode", product.getFulfilmentCode()).debug("Recording rate-limiting");
        CaseType caseType = CaseType.valueOf(caseDetails.getCaseType());
        UniquePropertyReferenceNumber uprn =
            UniquePropertyReferenceNumber.create(caseDetails.getAddress().getUprn());
        recordRateLimiting(contact, product, caseType, ipAddress, uprn);
      }
    } else {
      log.info("Rate limiter client is disabled");
    }
  }

  /*
   * Call the rate limiter within a circuit-breaker, thus protecting the RHSvc
   * functionality from the unlikely event that that the rate limiter service is failing.
   *
   * If the limit is breached, a ResponseStatusException with HTTP 429 will be thrown.
   */
  private void recordRateLimiting(
      Contact contact,
      Product product,
      CaseType caseType,
      String ipAddress,
      UniquePropertyReferenceNumber uprn) {
    ResponseStatusException limitException =
        circuitBreaker.run(
            () -> {
              try {
                rateLimiterClient.checkFulfilmentRateLimit(
                    Domain.RH, product, caseType, ipAddress, uprn, contact.getTelNo());
                return null;
              } catch (CTPException e) {
                // we should get here if the rate-limiter is failing or not communicating
                // ... wrap and rethrow to be handled by the circuit-breaker
                throw new RuntimeException(e);
              } catch (ResponseStatusException e) {
                // we have got a 429 but don't rethrow it otherwise this will count against
                // the circuit-breaker accounting, so instead we return it to later throw
                // outside the circuit-breaker mechanism.
                return e;
              }
            },
            throwable -> {
              // This is the Function for the circuitBreaker.run second parameter, which is called
              // when an exception is thrown from the first Supplier parameter (above), including
              // as part of the processing of being in the circuit-breaker OPEN state.
              //
              // It is OK to carry on, since it is better to tolerate limiter error than fail
              // operation, however by getting here, the circuit-breaker has counted the failure,
              // or we are in circuit-breaker OPEN state.
              if (throwable instanceof CallNotPermittedException) {
                log.info("Circuit breaker is OPEN calling rate limiter for fulfilments");
              } else {
                log.with("error", throwable.getMessage())
                    .error(throwable, "Rate limiter failure for fulfilments");
              }
              return null;
            });
    if (limitException != null) {
      throw limitException;
    }
  }

  private void createAndSendFulfilments(
      DeliveryChannel deliveryChannel,
      Contact contact,
      FulfilmentRequestDTO request,
      List<Product> products,
      CollectionCase caseDetails)
      throws CTPException {
    log.with("fulfilmentCodes", request.getFulfilmentCodes())
        .with("deliveryChannel", deliveryChannel)
        .debug("Entering createAndSendFulfilment");

    for (Product product : products) {
      FulfilmentRequest payload =
          createFulfilmentRequestPayload(request.getCaseId(), contact, product, caseDetails);

      eventPublisher.sendEvent(
          EventType.FULFILMENT_REQUESTED, Source.RESPONDENT_HOME, Channel.RH, payload);
    }
  }

  private boolean isIndividual(Product product) {
    return product.getIndividual() == null ? false : product.getIndividual();
  }

  private FulfilmentRequest createFulfilmentRequestPayload(
      UUID caseId, Contact contact, Product product, CollectionCase caseDetails)
      throws CTPException {

    String individualCaseId = null;
    if (isIndividual(product) && CaseType.HH.name().equals(caseDetails.getCaseType())) {
      individualCaseId = UUID.randomUUID().toString();
    }

    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    fulfilmentRequest.setIndividualCaseId(individualCaseId);
    fulfilmentRequest.setFulfilmentCode(product.getFulfilmentCode());
    fulfilmentRequest.setCaseId(caseId.toString());
    fulfilmentRequest.setContact(contact);
    return fulfilmentRequest;
  }

  private Product findProduct(String fulfilmentCode, DeliveryChannel deliveryChannel, Region region)
      throws CTPException {
    log.with("region", region)
        .with("deliveryChannel", deliveryChannel)
        .with("fulfilmentCode", fulfilmentCode)
        .debug("Attempting to find product.");

    // Build search criteria base on the cases details and the requested fulfilmentCode
    Product searchCriteria = new Product();
    searchCriteria.setRequestChannels(Collections.singletonList(RequestChannel.RH));
    searchCriteria.setRegions(Collections.singletonList(region));
    searchCriteria.setDeliveryChannel(deliveryChannel);
    searchCriteria.setFulfilmentCode(fulfilmentCode);

    // Attempt to find matching product
    return productReference.searchProducts(searchCriteria).stream()
        .findFirst()
        .orElseThrow(
            () -> {
              log.with("searchCriteria", searchCriteria).warn("Compatible product cannot be found");
              return new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
            });
  }

  // Read case from firestore
  private CollectionCase findCaseDetails(UUID caseId) throws CTPException {
    return dataRepo
        .readCollectionCase(caseId.toString())
        .orElseThrow(
            () -> {
              log.with("caseId", caseId).info("Case not found");
              return new CTPException(Fault.RESOURCE_NOT_FOUND, "Case not found: " + caseId);
            });
  }

  private void validateContactName(Contact contact) throws CTPException {
    if (StringUtils.isBlank(contact.getForename()) || StringUtils.isBlank(contact.getSurname())) {

      log.warn("Individual fields are required for the requested fulfilment");
      throw new CTPException(
          Fault.BAD_REQUEST,
          "The fulfilment is for an individual so none of the following fields can be empty: "
              + "'forename' and 'surname'");
    }
  }

  private Optional<CaseDTO> getLatestCaseByUPRN(UniquePropertyReferenceNumber uprn)
      throws CTPException {
    Optional<CaseDTO> result = Optional.empty();

    Optional<CollectionCase> caseFound =
        dataRepo.readLatestCollectionCaseByUprn(Long.toString(uprn.getValue()));
    if (caseFound.isPresent()) {
      log.with("case", caseFound.get().getId())
          .with("uprn", uprn)
          .debug("Existing case found by UPRN");
      result = Optional.of(mapperFacade.map(caseFound.get(), CaseDTO.class));
    }

    return result;
  }
}
