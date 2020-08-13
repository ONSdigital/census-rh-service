package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CollectionCaseNewAddress;
import uk.gov.ons.ctp.common.event.model.NewAddress;

public class TestUtil {
  // the actual census id as per the application.yml and also RM
  private static final String COLLECTION_EXERCISE_ID = "34d7f3bb-91c9-45d0-bb2d-90afce4fc790";

  static void verifyNewAddressEventSent(
      EventPublisher eventPublisher,
      String caseId,
      CaseType expectedCaseType,
      Address expectedAddress) {

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
