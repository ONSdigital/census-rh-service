package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.storage.StorageException;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSDataStore;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
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

  RespondentDataServiceImpl() {
    this.cloudDataStore = new GCSDataStore();
  }

  @PostConstruct
  public void init() {
    caseBucket = gcpProject + "-" + caseBucketName.toLowerCase();
    uacBucket = gcpProject + "-" + uacBucketName.toLowerCase();
  }

  /**
   * Serialize an UAC object into JSON string and store it in the cloud
   *
   * @param uac - object to be serialised and stored in the cloud
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public void writeUAC(final UAC uac) throws CTPException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    String universalAccessCode = uac.getUacHash();
    try {
      jsonInString = mapper.writeValueAsString(uac);
    } catch (JsonProcessingException e) {
      log.with(universalAccessCode).error("Could not serialize UAC object to JSON", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    writeJsonToCloud(jsonInString, universalAccessCode, uacBucket, "UAC object stored in cloud");
  }

  /**
   * Read an UAC object from cloud and de-serialize to an UAC object from JSON
   *
   * @param universalAccessCode - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public Optional<UAC> readUAC(final String universalAccessCode) throws CTPException {
    Optional<String> uacStrOpt =
        getJsonFromCloud(
            universalAccessCode, uacBucket, "Could not retrieve the UAC object from cloud");
    if (uacStrOpt.isPresent()) {
      return deserialiseUAC(universalAccessCode, uacStrOpt.get());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Serialize an CaseContext object into JSON string and store it in the cloud
   *
   * @param collectionCase - object to be serialised and stored in the cloud
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public void writeCollectionCase(final CollectionCase collectionCase) throws CTPException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    String caseId = collectionCase.getId();
    try {
      jsonInString = mapper.writeValueAsString(collectionCase);
    } catch (JsonProcessingException e) {
      log.with(caseId).error("Could not serialize CollectionCase object to JSON", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    writeJsonToCloud(jsonInString, caseId, caseBucket, "CollectionCase object stored in cloud");
  }

  /**
   * Read an Case object from cloud and de-serialize to an CaseContext object from JSON
   *
   * @param caseId - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public Optional<CollectionCase> readCollectionCase(final String caseId) throws CTPException {
    Optional<String> collectionCaseStrOpt;
    collectionCaseStrOpt =
        getJsonFromCloud(caseId, caseBucket, "Could not retrieve CollectionCase object from cloud");
    if (collectionCaseStrOpt.isPresent()) {
      return deserialiseCollectionCase(caseId, collectionCaseStrOpt.get());
    } else {
      return Optional.empty();
    }
  }

  private void writeJsonToCloud(String jsonInString, String key, String bucket, String debugMessage)
      throws CTPException {
    try {
      cloudDataStore.storeObject(bucket, key, jsonInString);
    } catch (StorageException e) {
      log.with(key).error("Could not store json object in cloud", e);
      throw new CTPException(Fault.SYSTEM_ERROR);
    }
    log.with(key).debug(debugMessage);
  }

  private Optional<UAC> deserialiseUAC(String universalAccessCode, String uacStrOpt)
      throws CTPException {
    Optional<UAC> uacOpt = Optional.empty();
    if (uacStrOpt != null) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        uacOpt = Optional.of(mapper.readValue(uacStrOpt, UAC.class));
      } catch (IOException e) {
        log.with(universalAccessCode).error("Could not de-serialise UAC object from JSON", e);
        throw new CTPException(Fault.SYSTEM_ERROR, e);
      }
      log.with(universalAccessCode).debug("UAC object has been retrieved from the cloud");
    }
    return uacOpt;
  }

  private Optional<CollectionCase> deserialiseCollectionCase(
      String caseId, String collectionCaseStrOpt) throws CTPException {
    Optional<CollectionCase> collectionCaseOpt = Optional.empty();
    if (collectionCaseStrOpt != null) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        collectionCaseOpt =
            Optional.of(mapper.readValue(collectionCaseStrOpt, CollectionCase.class));
      } catch (IOException e) {
        log.with(caseId).error("Could not de-serialise CollectionCase object from JSON", e);
        throw new CTPException(Fault.SYSTEM_ERROR, e);
      }
      log.with(caseId).debug("CollectionCase object has been retrieved from the cloud");
    }
    return collectionCaseOpt;
  }

  private Optional<String> getJsonFromCloud(String caseId, String caseBucket, String s)
      throws CTPException {
    Optional<String> collectionCaseStrOpt;
    try {
      collectionCaseStrOpt = cloudDataStore.retrieveObject(caseBucket, caseId);
    } catch (StorageException e) {
      log.with(caseId).error(s, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    return collectionCaseStrOpt;
  }

  public void deleteJsonFromCloud(String key, String bucket) throws CTPException {
    try {
      cloudDataStore.deleteObject(key, bucket);
    } catch (StorageException e) {
      log.with(key).error(e.getMessage());
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
  }
}
