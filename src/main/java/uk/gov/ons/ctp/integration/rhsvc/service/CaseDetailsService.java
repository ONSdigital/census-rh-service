package uk.gov.ons.ctp.integration.rhsvc.service;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A CaseDetailsService implementation which encapsulates all business logic operating on the Case
 * entity model.
 */
@Service
public class CaseDetailsService {
  private static final Logger log = LoggerFactory.getLogger(CaseDetailsService.class);

  public static final String WRONG_OLD_SAMPLE_UNIT_TYPE_MSG =
      "Old Case has sampleUnitType %s. It is expected to have sampleUnitType %s.";

  private static final String CASE_CREATED_EVENT_DESCRIPTION = "Case created when %s";

  private static final int TRANSACTION_TIMEOUT = 30;

  /** Constructor for CaseDetailsService */
  public CaseDetailsService() {}
}
