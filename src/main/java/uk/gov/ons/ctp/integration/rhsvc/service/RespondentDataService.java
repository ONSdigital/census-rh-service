package uk.gov.ons.ctp.integration.rhsvc.service;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * A RespondentDataService implementation which encapsulates all business logic operating on the
 * Core Respondent Details entity model.
 */
@Service
public class RespondentDataService {
  private static final Logger log = LoggerFactory.getLogger(RespondentDataService.class);

  private static final int TRANSACTION_TIMEOUT = 30;

  /** Constructor for RespondentDataService */
  public RespondentDataService() {}
}
