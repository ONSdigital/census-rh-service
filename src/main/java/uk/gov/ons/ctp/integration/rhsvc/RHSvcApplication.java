package uk.gov.ons.ctp.integration.rhsvc;

// import com.godaddy.logging.LoggingConfigs;
// import java.time.Clock;
// import javax.annotation.PostConstruct;
// import net.sourceforge.cobertura.CoverageIgnore;
// import org.redisson.Redisson;
// import org.redisson.api.RedissonClient;
// import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import uk.gov.ons.ctp.integration.rhsvc.config.AppConfig;

/** The 'main' entry point for the RHSvc SpringBoot Application. */
// @CoverageIgnore
// @EnableTransactionManagement
// @IntegrationComponentScan
// @ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
// @EnableJpaRepositories(basePackages = {"uk.gov.ons.ctp.integration"})
// @EntityScan("uk.gov.ons.ctp.integration")
// @EnableAsync
// @ImportResource("springintegration/main.xml")
@SpringBootApplication
public class RHSvcApplication {

  private AppConfig appConfig;

  /** Constructor for RHSvcApplication */
  @Autowired
  public RHSvcApplication(final AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  /**
   * The main entry point for this applicaion.
   *
   * @param args runtime command line args
   */
  public static void main(final String[] args) {

    SpringApplication.run(RHSvcApplication.class, args);
  }

  /**
   * The restTemplate bean injected in REST client classes
   *
   * @return the restTemplate used in REST calls
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
