package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.FirestoreDataStore;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

/**
 * A RespondentDataService implementation which encapsulates all business logic operating on the
 * Core Respondent Details entity model.
 */
@Service
public class RespondentDataServiceImpl implements RespondentDataService {
  private static final Logger log = LoggerFactory.getLogger(RespondentDataServiceImpl.class);

  @Value("${GOOGLE_CLOUD_PROJECT}")
  String gcpProject;

  @Value("${googleStorage.caseBucketName}")
  String caseBucketName;

  @Value("${googleStorage.uacBucketName}")
  String uacBucketName;

  String caseBucket;
  String uacBucket;

  @Autowired private CloudDataStore cloudDataStore;

  public RespondentDataServiceImpl() {
    this.cloudDataStore = new FirestoreDataStore();
  }

  @PostConstruct
  public void init() {
    caseBucket = gcpProject + "-" + caseBucketName.toLowerCase();
    uacBucket = gcpProject + "-" + uacBucketName.toLowerCase();
  }

  /**
   * Stores a UAC object into the cloud data store.
   *
   * @param uac - object to be stored in the cloud
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeUAC(final UAC uac) throws CTPException {
    cloudDataStore.storeObject(uacBucket, uac.getUacHash(), uac);

    Optional<UAC> retrievedUac =
        cloudDataStore.retrieveObject(UAC.class, uacBucket, uac.getUacHash());
    log.info(retrievedUac.toString());
  }

  /**
   * Read a UAC object from cloud.
   *
   * @param universalAccessCode - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<UAC> readUAC(final String universalAccessCode) throws CTPException {
    return cloudDataStore.retrieveObject(UAC.class, caseBucket, universalAccessCode);
  }

  /**
   * Write a CollectionCase object into the cloud data store.
   *
   * @param collectionCase - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeCollectionCase(final CollectionCase collectionCase) throws CTPException {
    cloudDataStore.storeObject(caseBucket, collectionCase.getId(), collectionCase);
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
    return cloudDataStore.retrieveObject(CollectionCase.class, caseBucket, caseId);
  }

  /**
   * Read a Case object from cloud based on its uprn.
   *
   * @param uprn - is the uprn that the target case must contain.
   * @return - de-serialised version of the stored object
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<CollectionCase> readCollectionCaseByUprn(final String uprn) throws CTPException {
    // Run search
    String[] searchByUprnPath = new String[] {"address", "uprn"};
    List<CollectionCase> searchResults = cloudDataStore.search(CollectionCase.class, caseBucket, searchByUprnPath, uprn);
    
    Optional<CollectionCase> collectionCase;
    if (searchResults.isEmpty()) {
      collectionCase = Optional.empty();
    } else if (searchResults.size() == 1) {
      collectionCase = Optional.of(searchResults.get(0));
    } else {
      throw new CTPException(Fault.SYSTEM_ERROR, "Multiple values (" + searchResults.size() + ") returned for uprn '" + uprn + "' in bucket '" + caseBucket + "'");
    }
    
    return collectionCase;
  }

  /**
   * Delete an object from the cloud. No exception is thrown if the object does not exist.
   *
   * @param schema
   * @param key
   * @throws CTPException
   */
  public void deleteJsonFromCloud(String schema, String key) throws CTPException {
    cloudDataStore.deleteObject(schema, key);
  }
}
