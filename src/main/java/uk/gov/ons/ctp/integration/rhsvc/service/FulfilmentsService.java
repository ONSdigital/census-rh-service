package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.List;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.rhsvc.representation.ProductDTO;

public interface FulfilmentsService {

  List<ProductDTO> getFulfilments(
      List<CaseType> caseType,
      Region region,
      DeliveryChannel deliveryChannel,
      Product.ProductGroup productGroup,
      Boolean individual)
      throws CTPException;
}
