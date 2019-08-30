package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressModified;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseCompact;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** Implementation to deal with Case data */
@Service
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private MapperFacade mapperFacade;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private ProductReference productReference;

  @Override
  public List<CaseDTO> getHHCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {

    String uprnValue = Long.toString(uprn.getValue());
    log.with("uprn", uprn).debug("Fetching case details by UPRN");

    List<CollectionCase> rmCase = dataRepo.readCollectionCasesByUprn(uprnValue);
    List<CollectionCase> results =
        rmCase
            .stream()
            .filter(c -> c.getAddress().getAddressType().equals(CaseType.HH.name()))
            .collect(Collectors.toList());
    List<CaseDTO> caseData = mapperFacade.mapAsList(results, CaseDTO.class);

    log.with("cases", caseData.size())
        .with("uprn", uprnValue)
        .debug("HH case(s) retrieved for UPRN");

    return caseData;
  }

  @Override
  public CaseDTO modifyAddress(final AddressChangeDTO addressChanges) throws CTPException {

    String caseId = addressChanges.getCaseId().toString();

    Optional<CollectionCase> caseMatch = dataRepo.readCollectionCase(caseId);

    if (!caseMatch.isPresent()) {
      log.with("caseId", caseId).error("Failed to retrieve Case from storage");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve Case");
    }

    CollectionCase rmCase = caseMatch.get();

    CaseDTO caseData = createModifiedAddressCaseDetails(caseId, rmCase, addressChanges);

    AddressModified originalAddress = mapperFacade.map(rmCase.getAddress(), AddressModified.class);

    AddressModified updatedAddress = mapperFacade.map(rmCase.getAddress(), AddressModified.class);

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
      String caseId, AddressModified originalAddress, AddressModified newAddress)
      throws CTPException {

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
    UUID caseId = requestBodyDTO.getCaseId();

    // Read case from firestore
    Optional<CollectionCase> caseDetails = dataRepo.readCollectionCase(caseId.toString());
    if (caseDetails.isEmpty()) {
      log.with("caseId", caseId).info("Case not found");
      String errorMessage = "Case not found: " + caseId;
      throw new CTPException(Fault.RESOURCE_NOT_FOUND, errorMessage);
    }

    // Attempt to find the requested product
    Product product =
        findProduct(caseDetails.get(), requestBodyDTO.getFulfilmentCode(), DeliveryChannel.SMS);
    if (product == null) {
      log.info("fulfilmentRequestBySMS can't find compatible product");
      throw new CTPException(Fault.BAD_REQUEST, "Compatible product cannot be found");
    }

    // Build and send a fulfilment request event
    FulfilmentRequest fulfilmentRequestedPayload =
        createFulfilmentRequestPayload(product, requestBodyDTO.getTelNo(), caseDetails.get());
    eventPublisher.sendEvent(
        EventType.FULFILMENT_REQUESTED,
        Source.RESPONDENT_HOME,
        Channel.RH,
        fulfilmentRequestedPayload);
  }

  // Search the ProductReference for the specified product
  private Product findProduct(
      CollectionCase caseDetails, String fulfilmentCode, Product.DeliveryChannel deliveryChannel)
      throws CTPException {

    Region region = Region.valueOf(caseDetails.getAddress().getRegion());

    log.with("region", region)
        .with("deliveryChannel", deliveryChannel)
        .with("fulfilmentCode", fulfilmentCode)
        .debug("Attempting to find product.");

    // Build search criteria base on the cases details and the requested fulfilmentCode
    Product searchCriteria = new Product();
    searchCriteria.setRequestChannels(Arrays.asList(Product.RequestChannel.RH));
    searchCriteria.setRegions(Arrays.asList(region));
    searchCriteria.setDeliveryChannel(deliveryChannel);
    searchCriteria.setFulfilmentCode(fulfilmentCode);

    // Attempt to find matching product
    List<Product> products = productReference.searchProducts(searchCriteria);
    if (products.size() == 0) {
      return null;
    }

    return products.get(0);
  }

  private FulfilmentRequest createFulfilmentRequestPayload(
      Product product, String telephoneNumber, CollectionCase caseDetails) {
    // Create the event payload request
    FulfilmentRequest fulfilmentRequest = new FulfilmentRequest();
    fulfilmentRequest.setCaseId(caseDetails.getId());
    if (product.getCaseType().equals(Product.CaseType.HI)) {
      fulfilmentRequest.setIndividualCaseId(UUID.randomUUID().toString());
    }
    fulfilmentRequest.setFulfilmentCode(product.getFulfilmentCode());

    // Use the phone number that was supplied for this fulfilment request
    fulfilmentRequest.setContact(new Contact());
    fulfilmentRequest.getContact().setTelNo(telephoneNumber);

    return fulfilmentRequest;
  }
}
