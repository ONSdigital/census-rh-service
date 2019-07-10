package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
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
   * @param deliveryChannel is an optional parameter to specify the delivery channel, eg, 'POST'.
   * @return A list of matching products. The list will be empty if there are no matching products.
   * @throws CTPException if something went wrong.
   */
  @RequestMapping(value = "/fulfilments", method = RequestMethod.GET)
  public ResponseEntity<List<Product>> getFulfilments(
      @RequestParam(required = false) CaseType caseType,
      @RequestParam(required = false) Region region,
      @RequestParam(required = false) DeliveryChannel deliveryChannel)
      throws CTPException {

    log.with("caseType", caseType)
        .with("region", region)
        .with("deliveryChannel", deliveryChannel)
        .info("Entering GET getFulfilments");

    List<Product> fulfilments =
        fulfilmentsService.getFulfilments(caseType, region, deliveryChannel);

    log.info("Found {} fulfilment(s)", fulfilments.size());
    
    return ResponseEntity.ok(fulfilments);
  }
}
