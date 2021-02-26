package uk.gov.ons.ctp.integration.rhsvc.repository;

import java.util.Optional;
import java.util.UUID;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;

/** Repository for Respondent Data */
public interface RespondentDataRepository {

  void writeUAC(UAC uac) throws CTPException;

  void writeCollectionCase(CollectionCase collectionCase) throws CTPException;

  Optional<UAC> readUAC(String universalAccessCode) throws CTPException;

  Optional<CollectionCase> readCollectionCase(String caseId) throws CTPException;

  Optional<CollectionCase> readNonHILatestValidCollectionCaseByUprn(String uprn)
      throws CTPException;

  Optional<CollectionCase> readLatestCollectionCaseByUprn(String uprn) throws CTPException;

  UUID writeCloudStartupCheckObject() throws Exception;
}
