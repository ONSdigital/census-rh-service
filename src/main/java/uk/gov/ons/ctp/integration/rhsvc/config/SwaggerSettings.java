package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

// import net.sourceforge.cobertura.CoverageIgnore;

/** Config POJO for Swagger UI Generation */
// @CoverageIgnore
@Data
public class SwaggerSettings {

  private Boolean swaggerUiActive;
  private String groupName;
  private String title;
  private String description;
  private String version;
}
