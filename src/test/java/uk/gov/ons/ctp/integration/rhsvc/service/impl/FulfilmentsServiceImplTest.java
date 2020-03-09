package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.integration.common.product.ProductReference;
import uk.gov.ons.ctp.integration.common.product.model.Product;
import uk.gov.ons.ctp.integration.common.product.model.Product.CaseType;
import uk.gov.ons.ctp.integration.common.product.model.Product.DeliveryChannel;
import uk.gov.ons.ctp.integration.common.product.model.Product.Region;
import uk.gov.ons.ctp.integration.common.product.model.Product.RequestChannel;
import uk.gov.ons.ctp.integration.rhsvc.representation.ProductDTO;

public class FulfilmentsServiceImplTest {

  @Mock ProductReference productReference;

  @InjectMocks FulfilmentsServiceImpl fulfilmentsService;

  @Captor ArgumentCaptor<Product> exampleProductCaptor;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testFulfilmentsQuery() throws Exception {
    // Mock the behaviour of the Product Reference
    List<Product> mockedResults = new ArrayList<>();
    Mockito.when(productReference.searchProducts(any())).thenReturn(mockedResults);

    // Invoke the method under test
    List<ProductDTO> products =
        fulfilmentsService.getFulfilments(
            Arrays.asList(CaseType.HH),
            Region.E,
            DeliveryChannel.SMS,
            Product.ProductGroup.UAC,
            true);

    // Get hold of the example product used in the search
    Mockito.verify(productReference).searchProducts(exampleProductCaptor.capture());
    Product capturedExampleProduct = exampleProductCaptor.getValue();

    // Verify that the request to product reference was made as RH
    assertEquals(1, capturedExampleProduct.getRequestChannels().size());
    assertEquals(RequestChannel.RH, capturedExampleProduct.getRequestChannels().get(0));

    // Verify the parameters are used in the product search
    assertTrue(capturedExampleProduct.getCaseTypes().contains(CaseType.HH));
    assertEquals(1, capturedExampleProduct.getRegions().size());
    assertEquals(Region.E, capturedExampleProduct.getRegions().get(0));
    assertEquals(DeliveryChannel.SMS, capturedExampleProduct.getDeliveryChannel());
    assertTrue(capturedExampleProduct.getIndividual());
    assertEquals(Product.ProductGroup.UAC, capturedExampleProduct.getProductGroup());

    // Verify that nothing else was specified in the product search
    assertNull(capturedExampleProduct.getFulfilmentCode());
    assertNull(capturedExampleProduct.getInitialContactCode());
    assertNull(capturedExampleProduct.getReminderContactCode());
    assertNull(capturedExampleProduct.getFieldDistributionCode());
    assertNull(capturedExampleProduct.getDescription());
    assertNull(capturedExampleProduct.getLanguage());
    assertNull(capturedExampleProduct.getHandler());

    // Verify that getFulfilments() returns the value it got from the ProductReference search
    assertEquals(mockedResults, products);
  }
}
