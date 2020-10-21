package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;
import uk.gov.ons.ctp.common.rest.RestClientConfig;

@Data
public class RateLimiterConfig {
  private RestClientConfig restClientConfig;
}
