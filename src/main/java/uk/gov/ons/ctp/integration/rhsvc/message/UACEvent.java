package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.message.GenericEvent;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UACEvent extends GenericEvent {

  private UACPayload payload = new UACPayload();
}
