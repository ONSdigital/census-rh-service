package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.rhsvc.representation.ProductDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.FulfilmentsService;

/** The REST controller for the RH Fulfilment end points */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public final class FulfilmentsEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(FulfilmentsEndpoint.class);

  private FulfilmentsService fulfilmentsService;

  @Autowired
  public FulfilmentsEndpoint(final FulfilmentsService fulfilmentsService) {
    this.fulfilmentsService = fulfilmentsService;
  }

  /**
   * The GET end point to retrieve fulfilment details, ie product codes for case type, region and
   * delivery channel.
   *
   * <p>All request parameters are optional. If no parameters are specified then all products which
   * are applicable to RH are returned.
   *
   * @param caseType is an optional parameter to specify the case type, eg, 'HI' or 'HH'
   * @param region is an optional parameter to specify the region, eg, 'E' for England.
   * @param deliveryChannel is an optional parameter to specify the delivery channel, eg, 'POST'
   * @param individual is an optional parameter to specify whether this is a query about an
   *     individual or a household
   * @param productGroup is an optional parameter to specify the product group, eg 'UAC'
   * @return A list of matching products. The list will be empty if there are no matching products.
   * @throws CTPException if something went wrong.
   */
  @RequestMapping(value = "/fulfilments", method = RequestMethod.GET)
  public ResponseEntity<List<ProductDTO>> getFulfilments(
      @RequestParam(required = false) CaseType caseType,
      @RequestParam(required = false) Region region,
      @RequestParam(required = false) DeliveryChannel deliveryChannel,
      @RequestParam(required = false) Boolean individual,
      @RequestParam(required = false) Product.ProductGroup productGroup)
      throws CTPException {

    log.with("requestParam.caseType", caseType)
        .with("requestParam.region", region)
        .with("requestParam.deliveryChannel", deliveryChannel)
        .with("requestParam.individual", individual)
        .with("requestParam.productGroup", productGroup)
        .info("Entering GET getFulfilments");
    List<CaseType> caseTypes = caseType == null ? Collections.emptyList() : Arrays.asList(caseType);
    List<ProductDTO> fulfilments =
        fulfilmentsService.getFulfilments(
            caseTypes, region, deliveryChannel, productGroup, individual);

    log.with("size", fulfilments.size()).info("Found fulfilment(s)");

    return ResponseEntity.ok(fulfilments);
  }
}
