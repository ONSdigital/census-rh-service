package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.cloud.DataStoreContentionException;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/** A RespondentDataRepository implementation for CRUD operations on Respondent data entities */
@Service
public class RespondentDataRepositoryImpl implements RespondentDataRepository {
  private static final Logger log = LoggerFactory.getLogger(RespondentDataRepositoryImpl.class);

  @Autowired private RetryableRespondentDataRepository retryableRespondentDataRepository;

  /**
   * Stores a UAC object into the cloud data store.
   *
   * @param uac - object to be stored in the cloud
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeUAC(final UAC uac) throws CTPException {
    try {
      retryableRespondentDataRepository.writeUAC(uac);
    } catch (DataStoreContentionException e) {
      log.error("Retries exhausted for storage of UAC: " + uac.getCaseId());
      throw new CTPException(
          Fault.SYSTEM_ERROR, e, "Retries exhausted for storage of UAC: " + uac.getCaseId());
    }
  }

  /**
   * Read a UAC object from cloud.
   *
   * @param universalAccessCodeHash - the hash of the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<UAC> readUAC(final String universalAccessCodeHash) throws CTPException {
    return retryableRespondentDataRepository.readUAC(universalAccessCodeHash);
  }

  /**
   * Write a CollectionCase object into the cloud data store.
   *
   * @param collectionCase - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeCollectionCase(final CollectionCase collectionCase) throws CTPException {
    try {
      retryableRespondentDataRepository.writeCollectionCase(collectionCase);
    } catch (DataStoreContentionException e) {
      log.error("Retries exhausted for storage of CollectionCase: " + collectionCase.getId());
      throw new CTPException(
          Fault.SYSTEM_ERROR, e, "Retries exhausted for storage of UAC: " + collectionCase.getId());
    }
  }

  /**
   * Read a Case object from cloud.
   *
   * @param caseId - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<CollectionCase> readCollectionCase(final String caseId) throws CTPException {
    return retryableRespondentDataRepository.readCollectionCase(caseId);
  }

  /**
   * Read case objects from cloud based on its uprn.
   *
   * @param uprn - is the uprn that the target case(s) must contain.
   * @return - List containing 0 or more de-serialised version of the stored object. If no matching
   *     cases are found then an empty List is returned.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  @Deprecated
  public List<CollectionCase> readCollectionCasesByUprn(final String uprn) throws CTPException {
    return retryableRespondentDataRepository.readCollectionCasesByUprn(uprn);
  }

  /**
   * Read case objects from cloud based on its uprn. Filter by non HI, not addressInvalid, latest
   * case
   *
   * @param uprn - is the uprn that the target case(s) must contain.
   * @return - Optional containing 1 de-serialised version of the stored object. If no matching
   *     cases are found then an empty Optional is returned.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<CollectionCase> readNonHILatestValidCollectionCaseByUprn(String uprn)
      throws CTPException {
    return retryableRespondentDataRepository.readNonHILatestCollectionCaseByUprn(uprn);
  }
}
