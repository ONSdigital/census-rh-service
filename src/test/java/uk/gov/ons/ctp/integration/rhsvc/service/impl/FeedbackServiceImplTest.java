package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventPublisher.Channel;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.EventPublisher.Source;
import uk.gov.ons.ctp.common.event.model.Feedback;
import uk.gov.ons.ctp.integration.rhsvc.representation.FeedbackDTO;

public class FeedbackServiceImplTest {

  @Mock EventPublisher publisher;

  @InjectMocks FeedbackServiceImpl feedbackService;

  @Captor ArgumentCaptor<Feedback> sendEventCaptor;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testFeedbackEventGenerated() throws Exception {
    // Give a feedback DTO to the service layer
    FeedbackDTO feedbackDTO = new FeedbackDTO();
    feedbackDTO.setChannel(Channel.EQ);
    feedbackDTO.setPageUrl("http://100.2.20.99/start");
    feedbackDTO.setPageTitle("Random Title");
    feedbackDTO.setFeedbackText("Bla bla");
    feedbackService.processFeedback(feedbackDTO);

    // Intercept call to EventPublisher, and capture supplied payload
    Mockito.verify(publisher)
        .sendEvent(
            eq(EventType.FEEDBACK),
            eq(Source.RESPONDENT_HOME),
            eq(feedbackDTO.getChannel()),
            sendEventCaptor.capture());
    Feedback feedback = sendEventCaptor.getValue();

    // Verify contents of payload object given to EventPublisher
    assertEquals(feedbackDTO.getPageUrl(), feedback.getPageUrl());
    assertEquals(feedbackDTO.getPageTitle(), feedback.getPageTitle());
    assertEquals(feedbackDTO.getFeedbackText(), feedback.getFeedbackText());
  }
}
