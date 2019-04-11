package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseEvent extends GenericEvent {

  private CasePayload payload = new CasePayload();

  @Override
  public String toString() {
    return super.toString() + " " + payload.toString();
  }
}
