package uk.gov.ons.ctp.integration.rhsvc.config;

// import net.sourceforge.cobertura.CoverageIgnore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

// import uk.gov.ons.tools.rabbit.Rabbitmq;

/** Application Config bean */
// @CoverageIgnore
@EnableRetry
@Configuration
@ConfigurationProperties
@Data
public class AppConfig {
  private SwaggerSettings swaggerSettings;
  private ReportSettings reportSettings;
  // private Rabbitmq rabbitmq;
  private Logging logging;
}
