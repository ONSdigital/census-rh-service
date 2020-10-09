package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class MessagingConfig {
  private boolean mismatchedQueuesFatal;
  private int backoffInitial;
  private int backoffMultiplier;
  private int backoffMax;
  private int consumingThreads;
  private int conMaxAttempts;
  private int prefetchCount;
}
