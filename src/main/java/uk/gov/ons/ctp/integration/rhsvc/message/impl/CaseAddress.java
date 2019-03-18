package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseAddress implements Serializable {
  private String addressLine1;
  private String addressLine2;
  private String city;
}
