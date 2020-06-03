package uk.gov.ons.ctp.integration.rhsvc;

import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.util.StringToUPRNConverter;

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
    converterFactory.registerConverter(new StringToUPRNConverter());

    factory
        .classMap(CollectionCase.class, CaseDTO.class)
        .field("id", "caseId")
        .field("address.addressType", "addressType")
        .field("address.region", "region")
        .byDefault()
        .register();

    factory.classMap(AddressDTO.class, AddressCompact.class).byDefault().register();
    factory.classMap(Address.class, AddressCompact.class).byDefault().register();
  }
}
