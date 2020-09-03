package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.domain.Region;

/** This object holds details from a webform. */
@Data
@NoArgsConstructor
@SuppressWarnings("serial")
public class WebformDTO implements Serializable {

  /** enum for category */
  public enum WebformCategory {
    MISSING_INFORMATION,
    TECHNICAL,
    FORM,
    COMPLAINT,
    ADDRESS,
    OTHER
  }

  /** enum for language */
  public enum WebformLanguage {
    EN,
    CY
  }

  @NotNull private WebformCategory category;

  @NotNull private Region region;

  @NotNull private WebformLanguage language;

  @NotNull private String name; // PMB Length ?

  @NotNull private String description; // PMB Length ?

  @NotNull private String email;
}
