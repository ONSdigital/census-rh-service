package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.DataStoreContentionException;
import uk.gov.ons.ctp.integration.rhsvc.cloud.FirestoreDataStore;

/**
 * This layer interacts with the data repository. It is responsible for handling exponential
 * backoffs when the datastore is becoming overloaded.
 */
@Service
public class RetryableRespondentDataRepository {
  private static final Logger log =
      LoggerFactory.getLogger(RetryableRespondentDataRepository.class);

  @Value("${GOOGLE_CLOUD_PROJECT}")
  String gcpProject;

  @Value("${cloudStorage.caseSchemaName}")
  String caseSchemaName;

  @Value("${cloudStorage.uacSchemaName}")
  String uacSchemaName;

  String caseSchema;
  String uacSchema;

  private final CloudDataStore cloudDataStore;

  public RetryableRespondentDataRepository() {
    this.cloudDataStore = new FirestoreDataStore();
    this.cloudDataStore.connect();
  }

  @PostConstruct
  public void init() {
    caseSchema = gcpProject + "-" + caseSchemaName.toLowerCase();
    uacSchema = gcpProject + "-" + uacSchemaName.toLowerCase();
  }

  /**
   * Stores a UAC object into the cloud data store.
   *
   * @param uac - object to be stored in the cloud
   * @throws CTPException - if a cloud exception was detected.
   * @throws DataStoreContentionException - on contention
   */
  @Retryable(
      label = "writeUAC",
      include = DataStoreContentionException.class,
      backoff =
          @Backoff(
              delayExpression = "#{${cloudStorage.backoffInitial}}",
              multiplierExpression = "#{${cloudStorage.backoffMultiplier}}",
              maxDelayExpression = "#{${cloudStorage.backoffMax}}"),
      maxAttemptsExpression = "#{${cloudStorage.backoffMaxAttempts}}",
      listeners = "rhRetryListener")
  public void writeUAC(final UAC uac) throws CTPException, DataStoreContentionException {
    cloudDataStore.storeObject(uacSchema, uac.getUacHash(), uac);
  }

  /**
   * Read a UAC object from cloud.
   *
   * @param universalAccessCodeHash - the hash of the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  public Optional<UAC> readUAC(final String universalAccessCodeHash) throws CTPException {
    return cloudDataStore.retrieveObject(UAC.class, uacSchema, universalAccessCodeHash);
  }

  /**
   * Write a CollectionCase object into the cloud data store.
   *
   * @param collectionCase - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   * @throws DataStoreContentionException - on contention
   */
  @Retryable(
      label = "writeCollectionCase",
      include = DataStoreContentionException.class,
      backoff =
          @Backoff(
              delayExpression = "#{${cloudStorage.backoffInitial}}",
              multiplierExpression = "#{${cloudStorage.backoffMultiplier}}",
              maxDelayExpression = "#{${cloudStorage.backoffMax}}"),
      maxAttemptsExpression = "#{${cloudStorage.backoffMaxAttempts}}",
      listeners = "rhRetryListener")
  public void writeCollectionCase(final CollectionCase collectionCase)
      throws CTPException, DataStoreContentionException {
    cloudDataStore.storeObject(caseSchema, collectionCase.getId(), collectionCase);
  }

  /**
   * Read a Case object from cloud.
   *
   * @param caseId - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  public Optional<CollectionCase> readCollectionCase(final String caseId) throws CTPException {
    return cloudDataStore.retrieveObject(CollectionCase.class, caseSchema, caseId);
  }

  /**
   * Read case objects from cloud based on its uprn.
   *
   * @param uprn - is the uprn that the target case(s) must contain.
   * @return - List containing 0 or more de-serialised version of the stored object. If no matching
   *     cases are found then an empty List is returned.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Deprecated
  public List<CollectionCase> readCollectionCasesByUprn(final String uprn) throws CTPException {
    // Run search
    String[] searchByUprnPath = new String[] {"address", "uprn"};
    return cloudDataStore.search(CollectionCase.class, caseSchema, searchByUprnPath, uprn);
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
  public Optional<CollectionCase> readNonHILatestCollectionCaseByUprn(final String uprn)
      throws CTPException {
    // Run search
    String[] searchByUprnPath = new String[] {"address", "uprn"};
    List<CollectionCase> searchResults =
        cloudDataStore.search(CollectionCase.class, caseSchema, searchByUprnPath, uprn);
    return filterLatestValidNonHiCollectionCaseSearchResults(searchResults);
  }

  /**
   * Filter search results returning Latest !addressInvalid non HI case
   *
   * @param searchResults - Search results found in dataStore by searching by uprn
   * @return Optional of the resulting collection case or Empty
   */
  private Optional<CollectionCase> filterLatestValidNonHiCollectionCaseSearchResults(
      final List<CollectionCase> searchResults) {
    return searchResults
        .stream()
        .filter(c -> !c.getCaseType().equals(CaseType.HI.name()))
        .filter(c -> !c.isAddressInvalid())
        .max(Comparator.comparing(CollectionCase::getCreatedDateTime));
  }

  /**
   * When attempts to retry object storage have been exhausted this method is invoked and it can
   * then throw the exception (triggering Rabbit retries). If this is not done then the message
   * won't be eligible for another attempt or writing to the dead letter queue.
   *
   * @param e is the final exception in the storeObject retries.
   * @throws Exception the exception which caused the final attempt to fail.
   */
  @Recover
  public void doRecover(Exception e) throws Exception {
    log.with(e.getMessage()).debug("Datastore recovery throwing exception");
    throw e;
  }
}
