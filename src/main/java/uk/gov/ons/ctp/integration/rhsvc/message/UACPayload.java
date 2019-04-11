package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UACPayload {

  private UAC uac = new UAC();
}
