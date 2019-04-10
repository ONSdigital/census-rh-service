package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.time.DateTimeUtil;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.endpoint.SurveyLaunchedDTO;
import uk.gov.ons.ctp.integration.rhsvc.message.RespondentEventPublisher;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.GenericCaseEvent;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Header;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Payload;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentHomeService;

/**
 * This is a service layer class, which performs RH business level logic for the endpoints.
 */
@Service
public class RespondentHomeServiceImpl implements RespondentHomeService {
  private static final Logger log = LoggerFactory.getLogger(RespondentHomeServiceImpl.class);

  @Autowired private RespondentEventPublisher publisher;

  
  @Override
  public void surveyLaunched(SurveyLaunchedDTO surveyLaunchedDTO) {

    Header eventData =
       Header.builder()
           .type("SurveyLaunched")
           .source("ContactCentreAPI")
           .channel("cc")
           .dateTime(DateTimeUtil.getCurrentDateTimeInJsonFormat())
           .transactionId("c45de4dc-3c3b-11e9-b210-d663bd873pmb")  // PMB what to set this to?
           .build();

    CaseEvent response = new CaseEvent();
   response.add("questionnaireId", surveyLaunchedDTO.getQuestionnaireId());
   response.add("caseId", surveyLaunchedDTO.getCaseId().toString());
   response.add("agentId", null);

    GenericCaseEvent caseEvent = new GenericCaseEvent(eventData, new Payload(response));

    publisher.sendEvent(caseEvent);
  }
}
