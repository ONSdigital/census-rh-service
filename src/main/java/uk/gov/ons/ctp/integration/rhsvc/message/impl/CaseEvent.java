package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseEvent extends GenericEvent {

  private CasePayload payload = new CasePayload();

  @Override
  public String toString() {
    return super.toString() + " " + payload.toString();
  }
}
