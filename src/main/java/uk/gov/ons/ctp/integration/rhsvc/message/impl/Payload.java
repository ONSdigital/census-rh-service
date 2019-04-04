package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseEvent;

@AllArgsConstructor
@Getter
public class Payload {
  private CaseEvent response;
}
