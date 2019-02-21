package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

public interface RespondentDataService {

  void writeUACContext(UACContext uacContext) throws CTPException;

  void writeCaseContext(CaseContext caseContext) throws CTPException;

  Optional<UACContext> readUACContext(String universalAccessCode) throws CTPException;

  Optional<CaseContext> readCaseContext(String caseId) throws CTPException;
}
