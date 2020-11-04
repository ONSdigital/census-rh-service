package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class WebformConfig {
  private String templateId;
  private String emailEn;
  private String emailCy;
}
