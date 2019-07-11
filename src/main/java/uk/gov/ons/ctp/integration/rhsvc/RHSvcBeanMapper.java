package uk.gov.ons.ctp.integration.rhsvc;

import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;

/** The bean mapper that maps to/from DTOs and repository entity types. */
@Component
public class RHSvcBeanMapper extends ConfigurableMapper {

  /**
   * Setup the mapper for all of our beans.
   *
   * @param factory the factory to which we add our mappings
   */
  protected final void configure(final MapperFactory factory) {

    ConverterFactory converterFactory = factory.getConverterFactory();
    converterFactory.registerConverter(new StringToUUIDConverter());

    factory
        .classMap(CollectionCase.class, CaseDTO.class)
        .field("address.addressType", "addressType")
        .field("address.addressLine1", "addressLine1")
        .field("address.addressLine2", "addressLine2")
        .field("address.addressLine3", "addressLine3")
        .field("address.townName", "townName")
        .field("address.region", "region")
        .field("address.postcode", "postcode")
        .field("address.uprn", "uprn")
        .byDefault()
        .register();
  }
}
