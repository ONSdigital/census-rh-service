package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.FulfilmentRequest;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.rhsvc.representation.SMSFulfilmentRequestDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.CaseService;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

@Service
public class CaseServiceImpl implements CaseService {

  private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

  @Autowired private RespondentDataService respondentDataService;

  @Autowired private ProductReference productReference;

  @Autowired private EventPublisher publisher;

  /**
   * This method contains the business logic for submitting a fulfilment by SMS request.
   *
   * @param requestBodyDTO contains the parameters from the originating http POST request.
   * @throws CTPException if the specified case cannot be found, or if no matching product is found.
   */
  @Override
  public void fulfilmentRequestBySMS(SMSFulfilmentRequestDTO requestBodyDTO) throws CTPException {
    UUID caseId = requestBodyDTO.getCaseId();

    // Find the specified case
    Optional<CollectionCase> caseDetails =
        respondentDataService.readCollectionCase(caseId.toString());
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
    publisher.sendEvent(
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
    CaseType caseType = CaseType.valueOf(caseDetails.getAddress().getAddressType());

    log.with("region", region)
        .with("caseType", caseType)
        .with("deliveryChannel", deliveryChannel)
        .with("fulfilmentCode", fulfilmentCode)
        .debug("Attempting to find product.");

    // Build search criteria base on the cases details and the requested fulfilmentCode
    Product searchCriteria = new Product();
    searchCriteria.setRequestChannels(Arrays.asList(Product.RequestChannel.RH));
    searchCriteria.setRegions(Arrays.asList(region));
    searchCriteria.setCaseType(caseType);
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
    fulfilmentRequest.setAddress(caseDetails.getAddress());
    fulfilmentRequest.setContact(caseDetails.getContact());

    // Use the phone number that was supplied for this fulfilment request
    fulfilmentRequest.getContact().setTelNo(telephoneNumber);

    return fulfilmentRequest;
  }
}
