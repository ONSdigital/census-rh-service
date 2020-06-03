package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.RespondentHomeFixture;
import uk.gov.ons.ctp.integration.rhsvc.event.impl.CaseEventReceiverImpl;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

@RunWith(MockitoJUnitRunner.class)
public class CaseEventReceiverImplUnit_Test {

  @Mock private RespondentDataRepository mockRespondentDataRepo;

  @InjectMocks private CaseEventReceiverImpl target;

  @Test
  public void test_acceptCaseEvent_success() throws CTPException {
    CaseEvent caseEvent = RespondentHomeFixture.createCaseUpdatedEvent();
    target.acceptCaseEvent(caseEvent);
    verify(mockRespondentDataRepo).writeCollectionCase(caseEvent.getPayload().getCollectionCase());
  }
}
