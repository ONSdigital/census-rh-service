package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.mockito.ArgumentCaptor;
import uk.gov.ons.ctp.common.domain.AddressLevel;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.domain.UniquePropertyReferenceNumber;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.NewAddress;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;
import uk.gov.ons.ctp.integration.rhsvc.representation.AddressDTO;
import uk.gov.ons.ctp.integration.rhsvc.representation.CaseDTO;

public class TestUtil {
  // the actual census id as per the application.yml and also RM
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  private RespondentDataRepository dataRepo;
  private EventPublisher eventPublisher;

  public TestUtil(RespondentDataRepository dataRepo, EventPublisher eventPublisher) {
    this.dataRepo = dataRepo;
    this.eventPublisher = eventPublisher;
  }

  void validateCaseDTO(
      CaseType expectedCaseType,
      Address expectedAddress,
      CaseDTO actualCase) {
    CollectionCase expectedCollectionCase = new CollectionCase();
    expectedCollectionCase.setId(actualCase.getCaseId().toString());
    expectedCollectionCase.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    expectedCollectionCase.setCaseType(expectedCaseType.name());
    expectedCollectionCase.setAddress(expectedAddress);

    validateCaseDTO(expectedCollectionCase, actualCase);
  }

  void validateCaseDTO(
      CollectionCase expectedCase, CaseDTO actualCase) {
    assertEquals(expectedCase.getId(), actualCase.getCaseId().toString());
    assertEquals(expectedCase.getCaseType(), actualCase.getCaseType());
    assertEquals(expectedCase.getAddress().getAddressType(), actualCase.getAddressType());
    validateAddressDTO(expectedCase.getAddress(), actualCase.getAddress());
    assertEquals(expectedCase.getAddress().getAddressType(), actualCase.getAddressType());
    assertEquals(expectedCase.getAddress().getRegion(), actualCase.getRegion());
    assertEquals(expectedCase.getAddress().getAddressLevel(), actualCase.getAddressLevel());
    assertEquals(expectedCase.getId(), actualCase.getCaseId().toString());
  }

  void validateAddressDTO(Address expected, AddressDTO actual) {
    assertEquals(new UniquePropertyReferenceNumber(expected.getUprn()), actual.getUprn());
    assertEquals(expected.getAddressLine1(), actual.getAddressLine1());
    assertEquals(expected.getAddressLine2(), actual.getAddressLine2());
    assertEquals(expected.getAddressLine3(), actual.getAddressLine3());
    assertEquals(expected.getTownName(), actual.getTownName());
    assertEquals(expected.getPostcode(), actual.getPostcode());
  }

  void validateCase(CaseType expectedCaseType, Address expectedAddress, CollectionCase newCase) {
    Integer expectedCeExpectedCapacity;
    if (expectedCaseType == CaseType.CE) {
      expectedCeExpectedCapacity = 1;
    } else {
      expectedCeExpectedCapacity = 0;
    }

    assertNotNull(UUID.fromString(newCase.getId()));
    assertNull(newCase.getCaseRef());
    assertEquals(expectedCaseType.name(), newCase.getCaseType());
    assertEquals("CENSUS", newCase.getSurvey());
    assertEquals(COLLECTION_EXERCISE_ID, newCase.getCollectionExerciseId());
    assertEquals(new Contact(), newCase.getContact());
    assertNull(newCase.getActionableFrom());
    assertFalse(newCase.isHandDelivery());
    assertFalse(newCase.isAddressInvalid());
    assertEquals(expectedCeExpectedCapacity, newCase.getCeExpectedCapacity());
    assertNotNull(newCase.getCreatedDateTime());

    Address actualAddress = newCase.getAddress();
    assertEquals(expectedAddress.getAddressLine1(), actualAddress.getAddressLine1());
    assertEquals(expectedAddress.getAddressLine2(), actualAddress.getAddressLine2());
    assertEquals(expectedAddress.getAddressLine3(), actualAddress.getAddressLine3());
    assertEquals(expectedAddress.getTownName(), actualAddress.getTownName());
    assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    assertEquals(expectedAddress.getRegion(), actualAddress.getRegion());
    assertEquals(expectedAddress.getUprn(), actualAddress.getUprn());
    assertEquals(expectedAddress.getEstabType(), actualAddress.getEstabType());
    assertNull(expectedAddress.getOrganisationName());
    assertEquals(expectedAddress.getAddressType(), actualAddress.getAddressType());
    assertEquals(expectedAddress.getAddressLevel(), actualAddress.getAddressLevel());
    assertEquals(expectedAddress, actualAddress);
  }

  void verifyCollectionCaseSavedToFirestore(CaseType expectedCaseType, Address expectedAddress)
      throws CTPException {
    ArgumentCaptor<CollectionCase> firestoreCaptor = ArgumentCaptor.forClass(CollectionCase.class);
    verify(dataRepo, times(1)).writeCollectionCase(firestoreCaptor.capture());

    validateCase(expectedCaseType, expectedAddress, firestoreCaptor.getValue());
  }

  void verifyNewAddressEventSent(
      String caseId, CaseType expectedCaseType, Address expectedAddress) {

    ArgumentCaptor<NewAddress> newAddressCapture = ArgumentCaptor.forClass(NewAddress.class);

    verify(eventPublisher, times(1))
        .sendEvent(
            eq(EventType.NEW_ADDRESS_REPORTED),
            eq(Source.RESPONDENT_HOME),
            eq(Channel.RH),
            newAddressCapture.capture());

    NewAddress newAddress = newAddressCapture.getValue();
    CollectionCaseNewAddress caseNewAddress = newAddress.getCollectionCase();
    assertEquals(expectedCaseType.name(), caseNewAddress.getCaseType());
    assertEquals(caseId, caseNewAddress.getId());
    assertEquals("CENSUS", caseNewAddress.getSurvey());
    assertEquals(COLLECTION_EXERCISE_ID, caseNewAddress.getCollectionExerciseId());
    assertNull(caseNewAddress.getFieldCoordinatorId());
    assertNull(caseNewAddress.getFieldOfficerId());
    assertEquals(expectedAddress, caseNewAddress.getAddress());
  }
}
