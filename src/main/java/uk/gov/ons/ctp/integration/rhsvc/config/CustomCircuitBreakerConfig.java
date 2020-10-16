package uk.gov.ons.ctp.integration.rhsvc.config;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_FAILURE_RATE_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SIZE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLOW_CALL_DURATION_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_SLOW_CALL_RATE_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_WRITABLE_STACK_TRACE_ENABLED;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import lombok.Data;

@Data
public class CustomCircuitBreakerConfig {
  private int timeout = 4;
  private int minNumberOfCalls = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
  private int slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;
  private float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
  private float slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD;
  private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
  private int waitDurationSecondsInOpenState = DEFAULT_WAIT_DURATION_IN_OPEN_STATE;
  private int slowCallDurationSecondsThreshold = DEFAULT_SLOW_CALL_DURATION_THRESHOLD;
  private int permittedNumberOfCallsInHalfOpenState = DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
  private SlidingWindowType slidingWindowType = SlidingWindowType.COUNT_BASED;
  private boolean automaticTransitionFromOpenToHalfOpenEnabled;
}
