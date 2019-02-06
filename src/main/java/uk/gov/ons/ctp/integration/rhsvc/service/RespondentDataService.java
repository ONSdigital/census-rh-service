package uk.gov.ons.ctp.integration.rhsvc.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

import java.util.Optional;

public interface RespondentDataService {

  void writeUACContext(UACContext uacContext) throws CTPException;

  void writeCaseContext(CaseContext caseContext) throws CTPException;

  Optional<UACContext> readUACContext(String universalAccessCode) throws CTPException;

  Optional<CaseContext> readCaseContext(String caseId) throws CTPException;
}
