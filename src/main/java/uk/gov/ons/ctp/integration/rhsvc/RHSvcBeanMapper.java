package uk.gov.ons.ctp.integration.rhsvc;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.metadata.Type;
import org.springframework.stereotype.Component;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.AddressCompact;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.util.StringToUPRNConverter;
import uk.gov.ons.ctp.common.util.StringToUUIDConverter;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;

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
    converterFactory.registerConverter(new EstabTypeConverter());

    factory
        .classMap(CollectionCase.class, CaseDTO.class)
        .field("id", "caseId")
        .field("address.addressType", "addressType")
        .field("address.region", "region")
        .field("address.addressLevel", "addressLevel")
        .field("address.estabType", "estabType")
        .byDefault()
        .customize(
            new CustomMapper<CollectionCase, CaseDTO>() {
              @Override
              public void mapAtoB(
                  CollectionCase collectionCase, CaseDTO dto, MappingContext context) {
                if (dto.getEstabType() == null) {
                  dto.setEstabType(
                      CaseType.HH.name().equals(dto.getCaseType())
                          ? EstabType.HOUSEHOLD
                          : EstabType.OTHER);
                }
              }
            })
        .register();

    factory
        .classMap(CollectionCase.class, UniqueAccessCodeDTO.class)
        .field("id", "caseId")
        .field("address.region", "region")
        .field("address.estabType", "estabType")
        .byDefault()
        .register();

    factory.classMap(AddressDTO.class, AddressCompact.class).byDefault().register();
    factory.classMap(Address.class, AddressCompact.class).byDefault().register();
  }

  static class EstabTypeConverter extends BidirectionalConverter<String, EstabType> {
    @Override
    public String convertFrom(
        EstabType source, Type<String> destinationType, MappingContext mappingContext) {
      return source.name();
    }

    @Override
    public EstabType convertTo(
        String source, Type<EstabType> destinationType, MappingContext mappingContext) {
      return EstabType.forCode(source);
    }
  }
}
