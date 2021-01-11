package uk.gov.ons.ctp.integration.rhsvc.config;

import javax.validation.constraints.Min;
import lombok.Data;

@Data
public class LoadsheddingConfig {

  @Min(1)
  private int modulus;
}
