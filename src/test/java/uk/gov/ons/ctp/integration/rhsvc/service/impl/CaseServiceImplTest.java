package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.AddressModification;
import uk.gov.ons.ctp.common.event.model.AddressModified;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.RHSvcBeanMapper;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressChangeDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseType;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniquePropertyReferenceNumber;

public class CaseServiceImplTest {

  private static final UniquePropertyReferenceNumber UPRN =
      new UniquePropertyReferenceNumber("123456");

  @InjectMocks private CaseServiceImpl caseSvc;

  @Mock private RespondentDataRepository dataRepo;

  @Mock private EventPublisher eventPublisher;

  @Spy private MapperFacade mapperFacade = new RHSvcBeanMapper();

  private List<CollectionCase> collectionCase;
  private List<AddressChangeDTO> addressChangeDTO;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    this.addressChangeDTO = FixtureHelper.loadClassFixtures(AddressChangeDTO[].class);
  }

  /** Test returns valid CaseDTO for valid UPRN */
  @Test
  public void getHHCaseByUPRNFound() throws Exception {

    when(dataRepo.readCollectionCasesByUprn(Long.toString(UPRN.getValue())))
        .thenReturn(collectionCase);

    CollectionCase hhCase = this.collectionCase.get(0);

    List<CaseDTO> caseDTO = caseSvc.getHHCaseByUPRN(UPRN);
    CaseDTO rmCase = caseDTO.get(0);

    assertThat(caseDTO, hasSize(1));
    assertEquals(hhCase.getId(), rmCase.getCaseId().toString());
    assertEquals(hhCase.getCaseRef(), rmCase.getCaseRef());
    assertEquals(hhCase.getState(), rmCase.getState());
    assertEquals(hhCase.getAddress().getAddressType(), rmCase.getAddressType());
    assertEquals(hhCase.getAddress().getAddressLine1(), rmCase.getAddress().getAddressLine1());
    assertEquals(hhCase.getAddress().getAddressLine2(), rmCase.getAddress().getAddressLine2());
    assertEquals(hhCase.getAddress().getAddressLine3(), rmCase.getAddress().getAddressLine3());
    assertEquals(hhCase.getAddress().getTownName(), rmCase.getAddress().getTownName());
    assertEquals(hhCase.getAddress().getRegion(), rmCase.getRegion());
    assertEquals(hhCase.getAddress().getPostcode(), rmCase.getAddress().getPostcode());
    assertEquals(
        hhCase.getAddress().getUprn(), Long.toString(rmCase.getAddress().getUprn().getValue()));
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

  /** Test returns valid CaseDTO and sends address modified event message for valid CaseID */
  @Test
  public void modifyAddressByCaseIdFound() throws Exception {

    CollectionCase rmCase = collectionCase.get(0);
    UUID caseId = UUID.fromString(rmCase.getId());
    AddressChangeDTO addressChange = addressChangeDTO.get(0);
    ArgumentCaptor<AddressModification> payloadCapture =
        ArgumentCaptor.forClass(AddressModification.class);

    when(dataRepo.readCollectionCase(rmCase.getId())).thenReturn(Optional.of(rmCase));

    CaseDTO caseDTO = caseSvc.modifyAddress(caseId, addressChange);

    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.ADDRESS_MODIFIED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            payloadCapture.capture());

    AddressModification payload = payloadCapture.getValue();
    AddressModified originalAddress = payload.getOriginalAddress();
    AddressModified newAddress = payload.getNewAddress();
    AddressDTO addressUpdate = addressChange.getAddress();

    assertEquals(rmCase.getId(), caseDTO.getCaseId().toString());
    assertEquals(rmCase.getCaseRef(), caseDTO.getCaseRef());
    assertEquals(rmCase.getAddress().getAddressType(), caseDTO.getAddressType());
    assertEquals(rmCase.getState(), caseDTO.getState());
    assertSame(addressChange.getAddress(), caseDTO.getAddress());
    assertEquals(rmCase.getAddress().getRegion(), caseDTO.getRegion());

    assertSame(payload.getCollectionCase().getId(), caseId);

    assertEquals(rmCase.getAddress().getAddressLine1(), originalAddress.getAddressLine1());
    assertEquals(rmCase.getAddress().getAddressLine2(), originalAddress.getAddressLine2());
    assertEquals(rmCase.getAddress().getAddressLine3(), originalAddress.getAddressLine3());
    assertEquals(rmCase.getAddress().getTownName(), originalAddress.getTownName());
    assertEquals(rmCase.getAddress().getPostcode(), originalAddress.getPostcode());
    assertEquals(rmCase.getAddress().getRegion(), originalAddress.getRegion());
    assertEquals(rmCase.getAddress().getUprn(), originalAddress.getUprn());
    assertEquals(rmCase.getAddress().getArid(), originalAddress.getArid());

    assertEquals(addressUpdate.getAddressLine1(), newAddress.getAddressLine1());
    assertEquals(addressUpdate.getAddressLine2(), newAddress.getAddressLine2());
    assertEquals(addressUpdate.getAddressLine3(), newAddress.getAddressLine3());
    assertEquals(addressUpdate.getTownName(), newAddress.getTownName());
    assertEquals(addressUpdate.getPostcode(), newAddress.getPostcode());
    assertEquals(rmCase.getAddress().getRegion(), newAddress.getRegion());
    assertEquals(rmCase.getAddress().getUprn(), newAddress.getUprn());
    assertEquals(rmCase.getAddress().getArid(), newAddress.getArid());
  }

  /** Test request to modify address where caseId not found */
  @Test
  public void modifyAddressByCaseIdNotFound() throws Exception {

    CollectionCase rmCase = collectionCase.get(0);
    UUID caseId = UUID.fromString(rmCase.getId());
    AddressChangeDTO addressChange = addressChangeDTO.get(0);

    when(dataRepo.readCollectionCase(caseId.toString())).thenReturn(Optional.empty());

    boolean exceptionThrown = false;
    try {
      caseSvc.modifyAddress(caseId, addressChange);
    } catch (CTPException e) {
      assertEquals(CTPException.Fault.RESOURCE_NOT_FOUND, e.getFault());
      exceptionThrown = true;
    }

    verify(dataRepo, times(1)).readCollectionCase(caseId.toString());
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());

    assertTrue(exceptionThrown);
  }

  /** Test request to modify address where caseId does not return matching UPRN */
  @Test
  public void modifyAddressByCaseIdDifferentUPRN() throws Exception {

    CollectionCase rmCase = collectionCase.get(0);
    UUID caseId = UUID.fromString(rmCase.getId());
    AddressChangeDTO addressChange = addressChangeDTO.get(0);
    addressChange.getAddress().getUprn().setValue(0L);

    when(dataRepo.readCollectionCase(caseId.toString())).thenReturn(Optional.of(rmCase));

    boolean exceptionThrown = false;
    try {
      caseSvc.modifyAddress(caseId, addressChange);
    } catch (CTPException e) {
      assertEquals(CTPException.Fault.BAD_REQUEST, e.getFault());
      exceptionThrown = true;
    }

    verify(dataRepo, times(1)).readCollectionCase(caseId.toString());
    verify(eventPublisher, times(0)).sendEvent(any(), any(), any(), any());

    assertTrue(exceptionThrown);
  }
}
