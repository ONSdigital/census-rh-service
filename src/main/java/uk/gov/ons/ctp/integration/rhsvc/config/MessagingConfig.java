package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class MessagingConfig {
  private boolean mismatchedQueuesFatal;
  private BackoffConfig processingBackoff;
  private BackoffConfig recoveryBackoff;
  private int consumingThreads;
  private int conMaxAttempts;
  private int prefetchCount;
  private RhsCircuitBreakerConfig circuitBreaker;
}
