package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

public interface RespondentDataService {

  void writeUACContext(UACContext uac) throws CTPException;

  void writeCaseContext(CaseContext uac) throws CTPException;

  Optional<UACContext> readUACContext(String key) throws CTPException;

  Optional<CaseContext> readCaseContext(String key) throws CTPException;
}
