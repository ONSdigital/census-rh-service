package uk.gov.ons.ctp.integration.rhsvc.repository;

import java.util.List;
import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.cloud.DataStoreContentionException;

/** Repository for Respondent Data */
public interface RespondentDataRepository {

  void writeUAC(UAC uac) throws CTPException, DataStoreContentionException;

  void writeCollectionCase(CollectionCase collectionCase)
      throws CTPException, DataStoreContentionException;

  Optional<UAC> readUAC(String universalAccessCode) throws CTPException;

  Optional<CollectionCase> readCollectionCase(String caseId) throws CTPException;

  List<CollectionCase> readCollectionCasesByUprn(final String uprn) throws CTPException;
}
