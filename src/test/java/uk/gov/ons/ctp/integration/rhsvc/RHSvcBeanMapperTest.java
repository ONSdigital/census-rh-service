package uk.gov.ons.ctp.integration.rhsvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.domain.AddressType;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.EstabType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;

public class RHSvcBeanMapperTest {

  private MapperFacade mapper = new RHSvcBeanMapper();

  private CollectionCase collectionCase;

  @Before
  public void setup() {
    collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class).get(0);
  }

  private AddressDTO expectedAddress() {
    AddressDTO addr = new AddressDTO();
    addr.setAddressLine1("1 main street");
    addr.setAddressLine2("upper upperingham");
    addr.setAddressLine3("");
    addr.setTownName("upton");
    addr.setPostcode("UP103UP");
    addr.setUprn(UniquePropertyReferenceNumber.create("123456"));
    return addr;
  }

  @Test
  public void shouldMapCollectionCaseToCaseDTO() {
    CaseDTO dto = mapper.map(collectionCase, CaseDTO.class);
    assertEquals(UUID.fromString("aa4477d1-dd3f-4c69-b181-7ff725dc9fa4"), dto.getCaseId());
    assertEquals("10000000010", dto.getCaseRef());
    assertEquals(CaseType.CE.name(), dto.getCaseType());
    assertEquals(AddressType.CE.name(), dto.getAddressType());
    assertEquals(expectedAddress(), dto.getAddress());
    assertEquals("E", dto.getRegion());
    assertEquals("E", dto.getAddressLevel());
    assertEquals(EstabType.HOUSEHOLD, dto.getEstabType());
  }

  @Test
  public void shouldMapCollectionCaseWithCamelCaseEstabTypeToCaseDTO() {
    collectionCase.getAddress().setEstabType("Prison");
    CaseDTO dto = mapper.map(collectionCase, CaseDTO.class);
    assertEquals(EstabType.PRISON, dto.getEstabType());
  }

  @Test
  public void shouldMapCollectionCaseWithUnrecognisedEstabTypeToCaseDTO() {
    collectionCase.getAddress().setEstabType("XXX");
    CaseDTO dto = mapper.map(collectionCase, CaseDTO.class);
    assertEquals(EstabType.OTHER, dto.getEstabType());
  }

  // some Cases from RH have had null EstabType in production. Make sure we handle them.
  @Test
  public void shouldMapCollectionCaseWithNullEstabTypeToCaseDTO() {
    collectionCase.getAddress().setEstabType(null);
    CaseDTO dto = mapper.map(collectionCase, CaseDTO.class);
    assertEquals(EstabType.OTHER, dto.getEstabType());
  }

  // Household caseType should default to Household estabType if incoming estabType is null
  @Test
  public void shouldMapHouseholdCollectionCaseWithNullEstabTypeToCaseDTO() {
    collectionCase.getAddress().setEstabType(null);
    collectionCase.setCaseType(CaseType.HH.name());
    CaseDTO dto = mapper.map(collectionCase, CaseDTO.class);
    assertEquals(EstabType.HOUSEHOLD, dto.getEstabType());
  }
}
