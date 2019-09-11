package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.representation.FeedbackDTO;

/** Service responsible for recording end-user feedback */
public interface FeedbackService {

  /**
   * Forwards end-user feedback to a queue for subsequent processing.
   *
   * @param feedbackDTO contains the end-users feedback.
   * @return a String containing the transaction id used when the feedback was sent to Rabbit.
   * @throws CTPException something went wrong.
   */
  String processFeedback(FeedbackDTO feedbackDTO) throws CTPException;
}
