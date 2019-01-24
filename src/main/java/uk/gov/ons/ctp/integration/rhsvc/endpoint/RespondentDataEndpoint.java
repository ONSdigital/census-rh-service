package uk.gov.ons.ctp.integration.rhsvc.endpoint;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import ma.glasnost.orika.MapperFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

/** The REST endpoint controller for RHSvc Core Respondent Details */
@RestController
@RequestMapping(value = "/respondent", produces = "application/json")
public final class RespondentDataEndpoint implements CTPEndpoint {
  private static final Logger log = LoggerFactory.getLogger(RespondentDataEndpoint.class);

  public static final String CATEGORY_ACCESS_CODE_AUTHENTICATION_ATTEMPT_NOT_FOUND =
      "Category ACCESS_CODE_AUTHENTICATION_ATTEMPT does not exist";

  private RespondentDataService respondentDataService;
  private MapperFacade mapperFacade;

  /** Contructor for RespondentDataEndpoint */
  @Autowired
  public RespondentDataEndpoint(
      final RespondentDataService respondentDataService,
      final @Qualifier("RHSvcBeanMapper") MapperFacade mapperFacade) {
    this.respondentDataService = respondentDataService;
    this.mapperFacade = mapperFacade;
  }

  /**
   * the GET endpoint to get Core Respondent Details
   *
   * @return the core respondent details found
   * @throws CTPException something went wrong
   */
  @RequestMapping(value = "/data", method = RequestMethod.GET)
  public String getRespondentData() {
    String helloTeam = "Hello Census Integration!";

    return helloTeam;
  }

  //  @RequestMapping(value = "/getRespondentData", method = RequestMethod.GET)
  //  public ResponseEntity getRespondentData() {
  //
  //    // return ResponseEntity.ok(buildDetailedCaseDTO(caseObj, caseevents, iac));
  //
  //    return null;
  //  }
}
