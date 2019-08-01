package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import ma.glasnost.orika.BoundMapperFacade;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
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
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.integration.rhsvc.representation.util.StringToUPRNConverter;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;

/** Implementation to deal with Case data */
@Service
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  @Autowired private RespondentDataRepository dataRepo;
  @Autowired private MapperFacade mapperFacade;
  @Autowired private EventPublisher eventPublisher;
  @Autowired private ProductReference productReference;

  private BoundMapperFacade<AddressDTO, AddressModified> addressDTOMapperFacade;
  private BoundMapperFacade<Address, AddressModified> addressMapperFacade;

  /** Constructor */
  public CaseServiceImpl() {

    MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();
    ConverterFactory converterFactory = mapperFactory.getConverterFactory();
    converterFactory.registerConverter(new StringToUPRNConverter());
    this.addressDTOMapperFacade =
        mapperFactory.getMapperFacade(AddressDTO.class, AddressModified.class);
    this.addressMapperFacade = mapperFactory.getMapperFacade(Address.class, AddressModified.class);
  }

  @Override
  public List<CaseDTO> getHHCaseByUPRN(final UniquePropertyReferenceNumber uprn)
      throws CTPException {

    String uprnValue = Long.toString(uprn.getValue());
    log.debug("Fetching case details by UPRN: {}", uprnValue);

    List<CollectionCase> rmCase = dataRepo.readCollectionCasesByUprn(uprnValue);
    List<CollectionCase> results =
        rmCase
            .stream()
            .filter(c -> c.getAddress().getAddressType().equals(CaseType.HH.name()))
            .collect(Collectors.toList());
    List<CaseDTO> caseData = mapperFacade.mapAsList(results, CaseDTO.class);

    log.debug("{} HH case(s) retrieved for UPRN {}", caseData.size(), uprnValue);

    return caseData;
  }

  @Override
  public CaseDTO modifyAddress(final UUID caseId, final AddressChangeDTO addressChanges)
      throws CTPException {

    Optional<CollectionCase> caseMatch = dataRepo.readCollectionCase(caseId.toString());

    if (!caseMatch.isPresent()) {
      log.with("caseId", caseId).error("Failed to retrieve Case from storage");
      throw new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND, "Failed to retrieve Case");
    }

    CollectionCase rmCase = caseMatch.get();

    CaseDTO caseData = createModifiedAddressCaseDetails(caseId, rmCase, addressChanges);

    AddressModified originalAddress = addressMapperFacade.map(rmCase.getAddress());

    AddressModified updatedAddress =
        addressDTOMapperFacade.map(
            addressChanges.getAddress(), addressMapperFacade.map(rmCase.getAddress()));

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
      UUID caseId, CollectionCase rmCase, AddressChangeDTO addressChanges) throws CTPException {

    CaseDTO caseData = mapperFacade.map(rmCase, CaseDTO.class);
    if (!caseData.getAddress().getUprn().equals(addressChanges.getAddress().getUprn())) {
      log.with("caseId", caseId)
          .with("UPRN", addressChanges.getAddress().getUprn().toString())
          .error("Address CaseId and UPRN do not match");
      throw new CTPException(
          CTPException.Fault.BAD_REQUEST, "Address CaseId and UPRN do not match");
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
      UUID caseId, AddressModified originalAddress, AddressModified newAddress)
      throws CTPException {

    log.debug(
        "Generating AddressModified event for caseId: "
            + caseId.toString()
            + ", UPRN: "
            + originalAddress.getUprn());

    AddressModification addressModification =
        AddressModification.builder()
            .collectionCase(new CollectionCaseCompact(caseId))
            .originalAddress(originalAddress)
            .newAddress(newAddress)
            .build();

    String transactionId =
        eventPublisher.sendEvent(
            EventType.ADDRESS_MODIFIED, Source.RESPONDENT_HOME, Channel.RH, addressModification);

    log.debug(
        "AddressModified event published for caseId: "
            + addressModification.getCollectionCase().getId().toString()
            + ", transactionId: "
            + transactionId);
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
      String errorMessage = "Case not found: " + caseId;
      log.info(errorMessage);
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
