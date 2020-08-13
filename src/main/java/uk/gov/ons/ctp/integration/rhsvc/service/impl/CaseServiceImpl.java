package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.AddressLevel;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseRequestDTO;
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
      CaseType caseType = determinePrimaryCaseType(request);
      CollectionCase newCase = createCase(caseType, null /*PMB*/, request);
      log.with("caseId", newCase.getId())
          .with("primaryCaseType", caseType)
          .debug("Created new case");

      // Store new case in Firestore
      dataRepo.writeCollectionCase(newCase);

      // tell RM we have created a case for the selected (HH|CE|SPG) address
      sendNewAddressEvent(newCase);

      caseToReturn = mapperFacade.map(newCase, CaseDTO.class);
    }

    // Set address level for case
    if (caseToReturn.getCaseType().equals(CaseType.CE.name())) {
      caseToReturn.setAddressLevel(AddressLevel.E.name());
    } else {
      caseToReturn.setAddressLevel(AddressLevel.U.name());
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
    createAndSendFulfilment(
        DeliveryChannel.SMS,
        contact,
        requestBodyDTO.getCaseId(),
        requestBodyDTO.getFulfilmentCode());
  }

  @Override
  public void fulfilmentRequestByPost(PostalFulfilmentRequestDTO requestBodyDTO)
      throws CTPException {
    Contact contact = new Contact();
    contact.setTitle(requestBodyDTO.getTitle());
    contact.setForename(requestBodyDTO.getForename());
    contact.setSurname(requestBodyDTO.getSurname());
    createAndSendFulfilment(
        DeliveryChannel.POST,
        contact,
        requestBodyDTO.getCaseId(),
        requestBodyDTO.getFulfilmentCode());
  }

  private void createAndSendFulfilment(
      DeliveryChannel deliveryChannel, Contact contact, UUID caseId, String fulfilmentCode)
      throws CTPException {
    log.with("fulfilmentCode", fulfilmentCode)
        .with("deliveryChannel", deliveryChannel)
        .debug("Entering createAndSendFulfilment");
    FulfilmentRequest payload =
        createFulfilmentRequestPayload(fulfilmentCode, deliveryChannel, caseId, contact);
    eventPublisher.sendEvent(
        EventType.FULFILMENT_REQUESTED, Source.RESPONDENT_HOME, Channel.RH, payload);
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
    return productReference
        .searchProducts(searchCriteria)
        .stream()
        .findFirst()
        .orElseThrow(
            () -> {
              log.with("searchCriteria", searchCriteria).warn("Compatible product cannot be found");
              return new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
            });
  }

  private FulfilmentRequest createFulfilmentRequestPayload(
      String fulfilmentCode, Product.DeliveryChannel deliveryChannel, UUID caseId, Contact contact)
      throws CTPException {
    // Read case from firestore
    CollectionCase caseDetails =
        dataRepo
            .readCollectionCase(caseId.toString())
            .orElseThrow(
                () -> {
                  log.with("caseId", caseId).info("Case not found");
                  return new CTPException(Fault.RESOURCE_NOT_FOUND, "Case not found: " + caseId);
                });

    // Attempt to find the requested product
    Region region = Region.valueOf(caseDetails.getAddress().getRegion());
    Product product = findProduct(fulfilmentCode, deliveryChannel, region);
    boolean individual = product.getIndividual() == null ? false : product.getIndividual();

    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    if (individual) {
      if (deliveryChannel == DeliveryChannel.POST) {
        validateContactName(contact);
      }
      if (CaseType.HH.name().equals(caseDetails.getCaseType())) {
        fulfilmentRequest.setIndividualCaseId(UUID.randomUUID().toString());
      }
    }

    fulfilmentRequest.setFulfilmentCode(fulfilmentCode);
    fulfilmentRequest.setCaseId(caseId.toString());
    fulfilmentRequest.setContact(contact);
    fulfilmentRequest.setAddress(caseDetails.getAddress());
    return fulfilmentRequest;
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

  private CaseType determinePrimaryCaseType(CaseRequestDTO request) {
    String caseTypeStr = null;

    EstabType estabType = EstabType.forCode(request.getEstabType());
    Optional<AddressType> addressTypeForEstab = estabType.getAddressType();
    if (addressTypeForEstab.isPresent()) {
      // 1st choice. Set based on the establishment description
      caseTypeStr = addressTypeForEstab.get().name(); // ie the equivalent
    } else {
      caseTypeStr = request.getAddressType().name(); // trust AIMS
    }

    CaseType caseType = CaseType.valueOf(caseTypeStr);

    return caseType;
  }

  // Build a new case to store and send to RM via the NewAddressReported event.
  private CollectionCase createCase(CaseType caseType, UAC uac, CaseRequestDTO request) {
    CollectionCase newCase = new CollectionCase();

    newCase.setId(UUID.randomUUID().toString());
    newCase.setCollectionExerciseId(appConfig.getCollectionExerciseId());
    newCase.setHandDelivery(false);
    newCase.setSurvey("CENSUS");
    newCase.setCaseType(caseType.name());
    newCase.setAddressInvalid(false);
    newCase.setCreatedDateTime(DateTimeUtil.nowUTC());

    Address address = new Address();
    address.setAddressLine1(request.getAddressLine1());
    address.setAddressLine2(request.getAddressLine2());
    address.setAddressLine3(request.getAddressLine3());
    address.setTownName(request.getTownName());
    address.setRegion(request.getRegion().name());
    address.setPostcode(request.getPostcode());
    address.setUprn(Long.toString(request.getUprn().getValue()));
    address.setAddressType(caseType.name());
    address.setEstabType(request.getEstabType());
    newCase.setAddress(address);

    log.with("caseId", newCase.getId())
        .with("caseType", caseType)
        .debug("Have populated CollectionCase object");

    return newCase;
  }

  private void sendNewAddressEvent(CollectionCase collectionCase) {
    String caseId = collectionCase.getId();
    log.with("caseId", caseId).info("Generating NewAddressReported event");

    CollectionCaseNewAddress caseNewAddress = new CollectionCaseNewAddress();
    caseNewAddress.setId(caseId);
    caseNewAddress.setCaseType(collectionCase.getCaseType());
    caseNewAddress.setCollectionExerciseId(collectionCase.getCollectionExerciseId());
    caseNewAddress.setSurvey("CENSUS");
    caseNewAddress.setAddress(collectionCase.getAddress());

    NewAddress newAddress = new NewAddress();
    newAddress.setCollectionCase(caseNewAddress);

    String transactionId =
        eventPublisher.sendEvent(
            EventType.NEW_ADDRESS_REPORTED, Source.RESPONDENT_HOME, Channel.RH, newAddress);

    log.with("caseId", caseId)
        .with("transactionId", transactionId)
        .debug("NewAddressReported event published");
  }
}
