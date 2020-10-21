package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
      log.debug("No cases returned for uprn: " + uprn);
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
      log.with("caseId", caseId).error("Failed to retrieve Case from storage");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve Case");
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
          .error("The UPRN of the referenced Case and the provided Address UPRN must be matching");
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

  private static class FulfilmentInformation {
    Product product;
    CollectionCase caseDetails;

    boolean isIndividual() {
      return product.getIndividual() == null ? false : product.getIndividual();
    }
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
    var map = createFulfilmentMap(DeliveryChannel.SMS, contact, requestBodyDTO);
    recordRateLimiting(contact, requestBodyDTO, map);
    createAndSendFulfilments(DeliveryChannel.SMS, contact, requestBodyDTO, map);
  }

  @Override
  public void fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());
    var map = createFulfilmentMap(DeliveryChannel.POST, contact, requestBodyDTO);
    recordRateLimiting(contact, requestBodyDTO, map);
    createAndSendFulfilments(DeliveryChannel.POST, contact, requestBodyDTO, map);
  }

  /*
   * create a cached map of case details and product information to use for both
   * rate-limiting and event generation.
   * this prevents multiple calls to repeat getting case details and products.
   */
  private Map<String, FulfilmentInformation> createFulfilmentMap(
      DeliveryChannel deliveryChannel, Contact contact, FulfilmentRequestDTO request)
      throws CTPException {
    Map<String, FulfilmentInformation> map = new HashMap<>();
    for (String fulfilmentCode : request.getFulfilmentCodes()) {
      if (null == map.get(fulfilmentCode)) {
        FulfilmentInformation info = new FulfilmentInformation();
        info.caseDetails = findCaseDetails(request.getCaseId());
        Region region = Region.valueOf(info.caseDetails.getAddress().getRegion());
        info.product = findProduct(fulfilmentCode, deliveryChannel, region);
        map.put(fulfilmentCode, info);
      }
    }
    return map;
  }

  private void recordRateLimiting(
      Contact contact, FulfilmentRequestDTO request, Map<String, FulfilmentInformation> infoMap) {

    for (String fulfilmentCode : request.getFulfilmentCodes()) {
      log.with(fulfilmentCode).debug("Recording rate-limiting");

      FulfilmentInformation info = infoMap.get(fulfilmentCode);

      Product product = info.product;
      CaseType caseType = CaseType.valueOf(info.caseDetails.getCaseType());
      String ipAddress = request.getClientIP();
      UniquePropertyReferenceNumber uprn =
          UniquePropertyReferenceNumber.create(info.caseDetails.getAddress().getUprn());
      Optional<String> telNo = Optional.ofNullable(contact.getTelNo());

      rateLimiterClient.checkRateLimit(Domain.RHSvc, product, caseType, ipAddress, uprn, telNo);
    }
  }

  private void createAndSendFulfilments(
      DeliveryChannel deliveryChannel,
      Contact contact,
      FulfilmentRequestDTO request,
      Map<String, FulfilmentInformation> infoMap)
      throws CTPException {
    log.with("fulfilmentCodes", request.getFulfilmentCodes())
        .with("deliveryChannel", deliveryChannel)
        .debug("Entering createAndSendFulfilment");

    for (String fulfilmentCode : request.getFulfilmentCodes()) {
      FulfilmentInformation info = infoMap.get(fulfilmentCode);
      FulfilmentRequest payload =
          createFulfilmentRequestPayload(
              fulfilmentCode, deliveryChannel, request.getCaseId(), contact, info);

      eventPublisher.sendEvent(
          EventType.FULFILMENT_REQUESTED, Source.RESPONDENT_HOME, Channel.RH, payload);
    }
  }

  private String getIndividualCaseId(
      Product.DeliveryChannel deliveryChannel, Contact contact, FulfilmentInformation info)
      throws CTPException {

    String individualCaseId = null;
    if (info.isIndividual()) {
      if (deliveryChannel == DeliveryChannel.POST) {
        validateContactName(contact);
      }
      if (CaseType.HH.name().equals(info.caseDetails.getCaseType())) {
        individualCaseId = UUID.randomUUID().toString();
      }
    }
    return individualCaseId;
  }

  private FulfilmentRequest createFulfilmentRequestPayload(
      String fulfilmentCode,
      Product.DeliveryChannel deliveryChannel,
      UUID caseId,
      Contact contact,
      FulfilmentInformation info)
      throws CTPException {
    String individualCaseId = getIndividualCaseId(deliveryChannel, contact, info);

    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    fulfilmentRequest.setIndividualCaseId(individualCaseId);
    fulfilmentRequest.setFulfilmentCode(fulfilmentCode);
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
