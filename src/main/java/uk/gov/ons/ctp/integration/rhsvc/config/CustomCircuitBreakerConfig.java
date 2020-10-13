package uk.gov.ons.ctp.integration.rhsvc.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import lombok.Data;

@Data
public class CustomCircuitBreakerConfig {
  private int timeout = 4;
  private int minNumberOfCalls = CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;
  private int slidingWindowSize = CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SIZE;
  private float failureRateThreshold = CircuitBreakerConfig.DEFAULT_FAILURE_RATE_THRESHOLD;
  private float slowCallRateThreshold = CircuitBreakerConfig.DEFAULT_SLOW_CALL_RATE_THRESHOLD;
  private boolean writableStackTraceEnabled =
      CircuitBreakerConfig.DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
  private int waitDurationSecondsInOpenState =
      CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE;
  private int slowCallDurationSecondsThreshold =
      CircuitBreakerConfig.DEFAULT_SLOW_CALL_DURATION_THRESHOLD;
  private int permittedNumberOfCallsInHalfOpenState =
      CircuitBreakerConfig.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
  private SlidingWindowType slidingWindowType = SlidingWindowType.COUNT_BASED;
  private boolean automaticTransitionFromOpenToHalfOpenEnabled;
}
