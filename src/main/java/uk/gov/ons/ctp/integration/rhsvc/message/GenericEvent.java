package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.Header;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenericEvent {

  private Header event;
}
