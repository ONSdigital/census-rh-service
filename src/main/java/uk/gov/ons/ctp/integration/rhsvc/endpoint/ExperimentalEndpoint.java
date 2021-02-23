package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
  private static final Logger log = LoggerFactory.getLogger(ExperimentalEndpoint.class);
  private static final AtomicInteger logCount = new AtomicInteger();
  private static final AtomicInteger threadedlogCount = new AtomicInteger();

  @RequestMapping(value = "/log/{num}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void doSomeLogging(@PathVariable(value = "num") final int num) {
    log.with("num", num).info("Entering doSomeLogging");

    for (int i = 0; i < num; i++) {
      log.with("running_log_count", logCount.incrementAndGet()).info("Experimental logging");
    }

    log.with("num", num).info("Exiting doSomeLogging; count so far {}", logCount.get());
  }

  @RequestMapping(value = "/log/{threads}/{num}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void doSomeLoggingInThreads(@PathVariable final int threads, @PathVariable final int num) {
    log.with("threads", threads).with("num", num).info("Entering doSomeLoggingInThreads");
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    List<Callable<Boolean>> logTasks = new ArrayList<>();

    for (int i = 0; i < num; i++) {
      logTasks.add(
          () -> {
            log.with("running_log_count", threadedlogCount.incrementAndGet())
                .info("Experimental logging in local threads");
            return true;
          });
    }

    try {
      executor.invokeAll(logTasks);
    } catch (InterruptedException e) {
      log.error("Threads interrupted");
    }

    log.with("num", num)
        .info("Exiting doSomeLoggingInThreads; count so far {}", threadedlogCount.get());
  }

  @Builder
  @Data
  public static class LogStats {
    private int numLogCount;
    private int numThreadedLogCount;
  }

  @RequestMapping(value = "/stats", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<LogStats> stats() {
    LogStats stats =
        LogStats.builder()
            .numLogCount(logCount.get())
            .numThreadedLogCount(threadedlogCount.get())
            .build();
    return ResponseEntity.ok(stats);
  }
}
