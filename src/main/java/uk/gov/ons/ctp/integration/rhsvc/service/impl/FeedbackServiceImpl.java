package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.integration.rhsvc.representation.FeedbackDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.FeedbackService;

@Service
public class FeedbackServiceImpl implements FeedbackService {
  @Autowired private EventPublisher eventPublisher;

  @Override
  public String processFeedback(FeedbackDTO feedbackDTO) throws CTPException {
    Feedback feedbackResponse = new Feedback();
    feedbackResponse.setPageUrl(feedbackDTO.getPageUrl());
    feedbackResponse.setPageTitle(feedbackDTO.getPageTitle());
    feedbackResponse.setFeedbackText(feedbackDTO.getFeedbackText());

    String transactionId =
        eventPublisher.sendEvent(
            EventType.FEEDBACK, Source.RESPONDENT_HOME, Channel.RM, feedbackResponse);

    return transactionId;
  }
}
