package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class MessagingConfig {
  private boolean mismatchedQueuesFatal;

  /* processingBackoff - retrying when an event fails to be processed with rabbit working. */
  private BackoffConfig processingBackoff;
  /* recoveryBackoff - retrying the listener when rabbit stops working. */
  private BackoffConfig recoveryBackoff;

  private int consumingThreads;
  private int conMaxAttempts;
  private int prefetchCount;
}
