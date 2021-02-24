package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.micrometer.core.annotation.Timed;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Timed
@RestController
@RequestMapping(value = "/exp", produces = "application/json")
public class ExperimentalEndpoint {
  private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");
  private static final Logger log = LoggerFactory.getLogger(ExperimentalEndpoint.class);
  private final AtomicInteger logCount = new AtomicInteger();

  private int numThreads = 60;

  private ExecutorService executor;

  private LocalDateTime started;
  private Map<LocalDateTime, LogStats> history = new HashMap<>();

  @PostConstruct
  public void setup() {
    executor = Executors.newFixedThreadPool(numThreads);
    log.info("Running with {} threads for executor", numThreads);
  }

  private void initialise() {
    if (started == null) {
      started = LocalDateTime.now(UK_ZONE);
    }
  }

  @RequestMapping(value = "/log/{num}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void doSomeLogging(@PathVariable(value = "num") final int num) {
    log.with("num", num).info("Entering doSomeLogging");
    initialise();

    for (int i = 0; i < num; i++) {
      log.with("running_log_count", logCount.incrementAndGet()).info("Experimental logging");
    }

    log.with("num", num).info("Exiting doSomeLogging; count so far {}", logCount.get());
  }

  @RequestMapping(value = "/log/threaded/{num}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void doSomeLoggingInThreads(@PathVariable final int num) {
    log.with("num", num).info("Entering doSomeLoggingInThreads");

    initialise();
    List<Callable<Boolean>> logTasks = new ArrayList<>();

    for (int i = 0; i < num; i++) {
      logTasks.add(
          () -> {
            log.with("running_log_count", logCount.incrementAndGet())
                .info("Experimental logging in local threads");
            return true;
          });
    }

    try {
      List<Future<Boolean>> futures = executor.invokeAll(logTasks);
      futures.stream()
          .forEach(
              (future) -> {
                try {
                  future.get();
                } catch (InterruptedException e) {
                  log.error("Future interrupted");
                } catch (ExecutionException e) {
                  log.error("Future execution fail");
                }
              });
    } catch (InterruptedException e) {
      log.error("Threads interrupted");
    }

    log.with("num", num).info("Exiting doSomeLoggingInThreads; count so far {}", logCount.get());
  }

  @Builder
  @Data
  public static class LogStats {
    private LocalDateTime started;
    private int numLogCount;
  }

  private LogStats currentStats() {
    return LogStats.builder().started(started).numLogCount(logCount.get()).build();
  }

  @RequestMapping(value = "/stats", method = RequestMethod.GET)
  public ResponseEntity<LogStats> stats() {
    LogStats stats = currentStats();
    return ResponseEntity.ok(stats);
  }

  @RequestMapping(value = "/history", method = RequestMethod.GET)
  public ResponseEntity<Map<LocalDateTime, LogStats>> getHistory() {
    return ResponseEntity.ok(history);
  }

  @RequestMapping(value = "/reset", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void reset() {
    if (started != null) {
      LocalDateTime stopped = LocalDateTime.now(UK_ZONE);
      history.put(stopped, currentStats());
    }
    started = null;
    logCount.getAndSet(0);
  }
}
