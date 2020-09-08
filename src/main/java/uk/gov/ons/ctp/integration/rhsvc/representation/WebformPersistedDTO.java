package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

/** This object holds webform data with extra details for the datastore. */
@Data
@NoArgsConstructor
public class WebformPersistedDTO {

  private String id;

  private Date createdDateTime;

  private WebformDTO webformData;
}
