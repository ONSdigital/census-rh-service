package uk.gov.ons.ctp.integration.rhsvc;

import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.ConfigurableMapper;
import org.springframework.stereotype.Component;

/** The bean mapper that maps to/from DTOs and JPA entity types. */
@Component
public class RHSvcBeanMapper extends ConfigurableMapper {

  /**
   * Setup the mapper for all of our beans. Only fields having non identical names need mapping if
   * we also use byDefault() following.
   *
   * @param factory the factory to which we add our mappings
   */
  protected final void configure(final MapperFactory factory) {

    //    factory
    //        .classMap(CaseGroup.class, CaseGroupDTO.class)
    //        .field("status", "caseGroupStatus")
    //        .byDefault()
    //        .register();
    //
    //    factory.classMap(CaseEvent.class, CaseEventDTO.class).byDefault().register();

  }
}
