package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.FeedbackDTO;
import uk.gov.ons.ctp.integration.rhsvc.service.FeedbackService;

/** The REST endpoint controller for UAC requests. */
@RestController
@RequestMapping(value = "/", produces = "application/json")
public class FeedbackEndpoint {

  private static final Logger log = LoggerFactory.getLogger(FeedbackEndpoint.class);

  @Autowired private FeedbackService feedbackService;

  /**
   * the POST end-point to receive user feedback.
   *
   * @param feedbackDTO contains the feedback and data about the originating page.
   * @return a response containing the id of the generated
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/feedback", method = RequestMethod.POST)
  public ResponseEntity<String> submitFeedback(@Valid @RequestBody final FeedbackDTO feedbackDTO)
      throws CTPException {

    log.with("feedbackDTO", feedbackDTO).info("Entering POST submitFeedback");

    String transactionId = feedbackService.processFeedback(feedbackDTO);

    return ResponseEntity.ok(transactionId);
  }
}
