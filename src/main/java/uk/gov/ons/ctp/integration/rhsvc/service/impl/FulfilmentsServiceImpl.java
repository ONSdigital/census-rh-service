package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.rhsvc.service.FulfilmentsService;

@Service
public class FulfilmentsServiceImpl implements FulfilmentsService {

  @Autowired ProductReference productReference;

  @Override
  public List<Product> getFulfilments(
      List<CaseType> caseTypes,
      Region region,
      DeliveryChannel deliveryChannel,
      Product.ProductGroup productGroup,
      Boolean individual)
      throws CTPException {

    Product example = new Product();
    example.setRequestChannels(Arrays.asList(RequestChannel.RH));
    example.setCaseTypes(caseTypes);
    example.setRegions(region == null ? null : Arrays.asList(region));
    example.setDeliveryChannel(deliveryChannel);
    example.setIndividual(individual);
    example.setProductGroup(productGroup);

    List<Product> products = productReference.searchProducts(example);

    return products;
  }
}
