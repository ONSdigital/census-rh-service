package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class NotifyConfig {
  private String apiKey;
  private String baseUrl;
}
