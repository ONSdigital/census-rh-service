package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseType;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

public class CaseServiceImplTest {

  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber("123456");

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  private List<CollectionCase> collectionCase;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
  }

  /** Test returns valid JSON for valid UPRN */
  @Test
  public void getHHCaseByUPRNFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(collectionCase);

    CollectionCase hhCase = this.collectionCase.get(0);

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);
    CaseDTO rmCase = caseDTO.get(0);

    assertThat(caseDTO, hasSize(1));
    assertEquals(hhCase.getId(), rmCase.getId().toString());
    assertEquals(hhCase.getCaseRef(), rmCase.getCaseRef());
    assertEquals(hhCase.getState(), rmCase.getState());
    assertEquals(hhCase.getAddress().getAddressType(), rmCase.getAddressType());
    assertEquals(hhCase.getAddress().getAddressLine1(), rmCase.getAddressLine1());
    assertEquals(hhCase.getAddress().getAddressLine2(), rmCase.getAddressLine2());
    assertEquals(hhCase.getAddress().getAddressLine3(), rmCase.getAddressLine3());
    assertEquals(hhCase.getAddress().getTownName(), rmCase.getTownName());
    assertEquals(hhCase.getAddress().getRegion(), rmCase.getRegion());
    assertEquals(hhCase.getAddress().getPostcode(), rmCase.getPostcode());
    assertEquals(hhCase.getAddress().getUprn(), Long.toString(rmCase.getUprn().getValue()));
  }

  /** Test returns empty list where only non HH cases returned from repository */
  @Test
  public void getHHCaseByUPRNHICasesOnly() throws Exception {

    List<CollectionCase> nonHHCases =
        collectionCase
            .stream()
            .filter(c -> !c.getAddress().getAddressType().equals(CaseType.HH.name()))
            .collect(Collectors.toList());

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue()))).thenReturn(nonHHCases);

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);

    assertThat(nonHHCases, hasSize(2));
    assertThat(caseDTO, hasSize(0));
  }

  /** Test returns empty list where no cases returned from repository */
  @Test
  public void getHHCaseByUPRNNotFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(Collections.emptyList());

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);

    assertThat(caseDTO, hasSize(0));
  }
}
