package uk.gov.ons.ctp.integration.rhsvc.domain.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Serializable {
  private String addressLine1;
  private String addressLine2;
  private String city;
}
