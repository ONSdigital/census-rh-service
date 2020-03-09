package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.List;
import lombok.Data;
import uk.gov.ons.ctp.integration.common.product.model.Product;

@Data
/** Representation of a Product omitting fields not needed by the RH UI */
public class ProductDTO {

  private String fulfilmentCode;
  private Product.ProductGroup productGroup;
  private String description;
  private String language;
  private List<Product.CaseType> caseTypes;
  private Boolean individual;
  private List<Product.Region> regions;
  private Product.DeliveryChannel deliveryChannel;
  private List<Product.RequestChannel> requestChannels;
  private Product.Handler handler;
}
