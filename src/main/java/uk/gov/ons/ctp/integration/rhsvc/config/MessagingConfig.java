package uk.gov.ons.ctp.integration.rhsvc.config;

import lombok.Data;

@Data
public class MessagingConfig {

  private boolean mismatchedQueuesFatal;
  // recoveryBackoff - frequency of attempted connection to rabbit after rabbit failure
  private BackoffConfig recoveryBackoff;
  // default settings for container
  private ContainerConfig uacCaseListener;
  private ContainerConfig webformListener;

  @Data
  public static class ContainerConfig {

    // processingBackoff - retrying when an event fails to be processed with rabbit working.
    private BackoffConfig processingBackoff;
    private int consumingThreads;
    private int conMaxAttempts;
    private int prefetchCount;
  }
}
