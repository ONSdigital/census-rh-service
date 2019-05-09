package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.representation.UniqueAccessCodeDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

/** Unit tests of the Unique Access Code Service */
public class UniqueAccessCodeServiceImplTest {

  private static final String UAC = "w4nwwpphjjpt";
  private static final String UAC_HASH =
      "97a2e785f8cffc8f40b5e8fc626933bb8aedf3df82672299bf7aeafe68c99c93";
  private static final String CASE_ID = "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4";

  @InjectMocks private UniqueAccessCodeServiceImpl uacSvc;

  @Mock private RespondentDataService dataRepo;

  private List<UAC> uac;
  private List<CollectionCase> collectionCase;

  /** Setup tests */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    this.uac = FixtureHelper.loadClassFixtures(UAC[].class);
    this.collectionCase = FixtureHelper.loadClassFixtures(CollectionCase[].class);
  }

  /** Test request for claim object where UAC and Case found */
  @Test
  public void getUniqueAccessCodeDataUACAndCaseFound() throws Exception {
    UAC uacTest = uac.get(0);
    CollectionCase caseTest = collectionCase.get(0);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCollectionCase(CASE_ID)).thenReturn(Optional.of(caseTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getUniqueAccessCodeData(UAC);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCollectionCase(CASE_ID);

    assertEquals(UAC, uacDTO.getUac());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(uacTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(Integer.valueOf(uacTest.getQuestionnaireId()), uacDTO.getQuestionnaireId());
    assertEquals(uacTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(uacTest.getRegion(), uacDTO.getRegion());

    assertEquals(caseTest.getAddress().getAddressLine1(), uacDTO.getAddress().getAddressLine1());
    assertEquals(caseTest.getAddress().getAddressLine2(), uacDTO.getAddress().getAddressLine2());
    assertEquals(caseTest.getAddress().getAddressLine3(), uacDTO.getAddress().getAddressLine3());
    assertEquals(caseTest.getAddress().getTownName(), uacDTO.getAddress().getTownName());
    assertEquals(caseTest.getAddress().getPostcode(), uacDTO.getAddress().getPostcode());
    assertEquals(caseTest.getAddress().getUprn(), uacDTO.getAddress().getUprn());
  }

  /** Test request for claim object where UAC found with caseID, CollectionCase not found */
  @Test
  public void getUniqueAccessCodeDataUACFoundWithCaseID() throws Exception {
    UAC uacTest = uac.get(0);
    CollectionCase caseTest = collectionCase.get(0);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));
    when(dataRepo.readCollectionCase(CASE_ID)).thenReturn(Optional.empty());

    UniqueAccessCodeDTO uacDTO = uacSvc.getUniqueAccessCodeData(UAC);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(1)).readCollectionCase(CASE_ID);

    assertEquals(UAC, uacDTO.getUac());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(UUID.fromString(uacTest.getCaseId()), uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(uacTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(Integer.valueOf(uacTest.getQuestionnaireId()), uacDTO.getQuestionnaireId());
    assertEquals(uacTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(uacTest.getRegion(), uacDTO.getRegion());

    assertEquals(null, uacDTO.getAddress());
  }

  /** Test request for claim object where UAC found without caseID */
  @Test
  public void getUniqueAccessCodeDataUACFoundWithoutCaseId() throws Exception {
    UAC uacTest = uac.get(0);
    uacTest.setCaseId(null);
    when(dataRepo.readUAC(UAC_HASH)).thenReturn(Optional.of(uacTest));

    UniqueAccessCodeDTO uacDTO = uacSvc.getUniqueAccessCodeData(UAC);

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCollectionCase(CASE_ID);

    assertEquals(UAC, uacDTO.getUac());
    assertEquals(Boolean.valueOf(uacTest.getActive()), uacDTO.isActive());
    assertEquals(null, uacDTO.getCaseId());
    assertEquals(
        UUID.fromString(uacTest.getCollectionExerciseId()), uacDTO.getCollectionExerciseId());
    assertEquals(Integer.valueOf(uacTest.getQuestionnaireId()), uacDTO.getQuestionnaireId());
    assertEquals(uacTest.getCaseType(), uacDTO.getCaseType());
    assertEquals(uacTest.getRegion(), uacDTO.getRegion());

    assertEquals(null, uacDTO.getAddress());
  }

  /** Test request for claim object where UAC not found */
  @Test
  public void getUniqueAccessCodeDataUACNotFound() throws Exception {

    when(dataRepo.readUAC(UAC_HASH))
        .thenThrow(new CTPException(CTPException.Fault.RESOURCE_NOT_FOUND));

    boolean exceptionThrown = false;
    try {
      uacSvc.getUniqueAccessCodeData(UAC);
    } catch (CTPException e) {
      exceptionThrown = true;
    }

    verify(dataRepo, times(1)).readUAC(UAC_HASH);
    verify(dataRepo, times(0)).readCollectionCase(CASE_ID);

    assertTrue(exceptionThrown);
  }
}
