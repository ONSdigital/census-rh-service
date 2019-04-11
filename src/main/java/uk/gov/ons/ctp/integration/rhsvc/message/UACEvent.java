package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UACEvent extends GenericEvent {

  private UACPayload payload = new UACPayload();

  @Override
  public String toString() {
    return super.toString() + " " + payload.toString();
  }
}
