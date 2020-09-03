package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.jackson.CustomDateSerialiser;

/** This object holds webform data with extra details for the datastore. */
@Data
@NoArgsConstructor
@SuppressWarnings("serial")
public class WebformPersistedDTO implements Serializable {

  private String id;

  @JsonSerialize(using = CustomDateSerialiser.class)
  private Date createdDateTime;

  private WebformDTO webformData;
}
