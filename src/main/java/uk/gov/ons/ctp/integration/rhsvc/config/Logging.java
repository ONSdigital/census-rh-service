package uk.gov.ons.ctp.integration.rhsvc.config;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Logging {
  private boolean useJson;
  @NotNull private Encryption encryption;

  @Data
  public static class Encryption {
    @NotBlank private String password;
  }
}
