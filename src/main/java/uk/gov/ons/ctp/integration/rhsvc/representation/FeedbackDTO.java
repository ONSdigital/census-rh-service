package uk.gov.ons.ctp.integration.rhsvc.representation;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/** Representation of end-user feedback data */
@Data
public class FeedbackDTO {

  @NotNull
  @Size(max = 1000)
  private String pageUrl;

  @NotNull
  @Size(max = 1000)
  private String pageTitle;

  @NotNull
  @Size(max = 10000)
  private String feedbackText;
}
