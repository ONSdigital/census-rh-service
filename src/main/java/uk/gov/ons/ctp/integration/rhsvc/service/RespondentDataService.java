package uk.gov.ons.ctp.integration.rhsvc.service;

import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.message.impl.UAC;

public interface RespondentDataService {

  void writeUAC(UAC uac) throws CTPException;

  void writeCollectionCase(CollectionCase collectionCase) throws CTPException;

  Optional<UAC> readUAC(String universalAccessCode) throws CTPException;

  Optional<CollectionCase> readCollectionCase(String caseId) throws CTPException;
}
