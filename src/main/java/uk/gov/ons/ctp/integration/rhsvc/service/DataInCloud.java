package uk.gov.ons.ctp.integration.rhsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

public interface DataInCloud {

  void writeUACContext(UACContext uac) throws JsonProcessingException;

  void writeCaseContext(CaseContext uac) throws JsonProcessingException;

  UACContext readUacContext(String key) throws IOException;

  CaseContext readCaseContext(String key) throws IOException;
}
