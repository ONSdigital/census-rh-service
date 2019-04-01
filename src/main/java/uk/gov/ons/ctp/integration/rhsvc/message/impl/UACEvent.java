package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UACEvent extends GenericEvent {

  private UACPayload uacPayload;

  @Override
  public String toString() {
    return super.toString() + " " + uacPayload.toString();
  }
}
