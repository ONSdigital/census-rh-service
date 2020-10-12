package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class RhsCircuitBreakerConfig {
  private int timeout;
  private int minNumberOfCalls;
  private int slidingWindowSize;
}
