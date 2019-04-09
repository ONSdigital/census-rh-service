package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.endpoint.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.GenericCaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Header;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Payload;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentHomeService;

/**
 * A RespondentDataService implementation which encapsulates all business logic operating on the
 * Core Respondent Details entity model.
 */
@Service
public class RespondentHomeServiceImpl implements RespondentHomeService {
  private static final Logger log = LoggerFactory.getLogger(RespondentHomeServiceImpl.class);

  @Autowired private RespondentEventPublisher publisher;

  
  @Override
  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) {

    Header eventData =
       Header.builder()
           .type("SurveryLaunched")
           .source("ContactCentreAPI")
           .channel("cc")
           .dateTime(new Timestamp(System.currentTimeMillis()))
           .transactionId("c45de4dc-3c3b-11e9-b210-d663bd873pmb")
           .build();

    CaseEvent response = new CaseEvent();
   response.add("questionnaireId", "eoueoueoueuouueeopmb");
   response.add("caseId", "bbd55984-0dbf-4499-bfa7-0aa4228700e9");
   response.add("agentId", "cc_000351pmb");

    GenericCaseEvent caseEvent = new GenericCaseEvent(eventData, new Payload(response));

    publisher.sendEvent(caseEvent);
  }
}
