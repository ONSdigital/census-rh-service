package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class MessagingConfig {
  private boolean mismatchedQueuesFatal;

  /* processingBackoff - retrying when an event fails to be processed with rabbit working. */
  private BackoffConfig processingBackoff;
  /* recoveryBackoff - frequency of attempted connection to rabbit after rabbit failure */
  private BackoffConfig recoveryBackoff;

  private int consumingThreads;
  private int conMaxAttempts;
  private int prefetchCount;
}
