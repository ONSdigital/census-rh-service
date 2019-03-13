package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.rhsvc.message.Payload;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdatedPayload implements Payload {

  private String cuPayloadBody;
}
