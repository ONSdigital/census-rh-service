package uk.gov.ons.ctp.integration.rhsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.storage.StorageException;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSDataStore;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

/**
 * A RespondentDataService implementation which encapsulates all business logic operating on the
 * Core Respondent Details entity model.
 */
@Service
public class RespondentDataService implements DataInCloud {
  private static final Logger log = LoggerFactory.getLogger(RespondentDataService.class);

  private static final String UAC_BUCKET = "uac_bucket";
  private static final String CASE_BUCKET = "case_bucket";

  @Autowired private CloudDataStore cloudDataStore;

  RespondentDataService() {
    this.cloudDataStore = new GCSDataStore();
  }

  /**
   * Serialize an UACContext object into JSON string and store it in the cloud
   *
   * @param uacContext - object to be serialised and stored in the cloud
   * @throws JsonProcessingException
   */
  @Override
  public void writeUACContext(final UACContext uacContext) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    try {
      jsonInString = mapper.writeValueAsString(uacContext);
    } catch (JsonProcessingException e) {
      log.info("Exception while serializing UAC context object to JSON", e);
      throw e;
    }
    try {
      cloudDataStore.storeObject(UAC_BUCKET, uacContext.getUac(), jsonInString);
    } catch (StorageException e) {
      log.info("Exception while storing UAC context object into cloud", e);
      throw e;
    }
    log.info(
        "UAC object  with code = " + uacContext.getUac() + " has just been stored in the cloud");
  }

  /**
   * Read an UAC object from cloud and de-serialize to an UACContext object from JSON
   *
   * @param key - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws IOException
   */
  @Override
  public UACContext readUacContext(final String key) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String objectAsString;
    try {
      objectAsString = cloudDataStore.retrieveObject(UAC_BUCKET, key);
    } catch (Exception e) {
      log.info("Exception while retrieving UAC context object from cloud", e);
      throw e;
    }
    UACContext uac;
    try {
      uac = mapper.readValue(objectAsString, UACContext.class);
    } catch (IOException e) {
      log.info("Exception while de-serializing UAC context object from JSON", e);
      throw e;
    }
    log.info("UAC object  with code = " + uac.getUac() + " has just been retrieved from the cloud");
    return uac;
  }

  /**
   * Serialize an CaseContext object into JSON string and store it in the cloud
   *
   * @param caseContext - object to be serialised and stored in the cloud
   * @throws JsonProcessingException
   */
  @Override
  public void writeCaseContext(final CaseContext caseContext) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    try {
      jsonInString = mapper.writeValueAsString(caseContext);
    } catch (JsonProcessingException e) {
      log.info("Exception while serializing case context object to JSON", e);
      throw e;
    }
    try {
      cloudDataStore.storeObject(CASE_BUCKET, caseContext.getCaseId(), jsonInString);
    } catch (StorageException e) {
      log.info("Exception while storing case context object into cloud", e);
      throw e;
    }
    log.info(
        "Case object  with caseId = "
            + caseContext.getCaseId()
            + " has just been stored in the cloud");
  }

  /**
   * Read an Case object from cloud and de-serialize to an CaseContext object from JSON
   *
   * @param key - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws IOException
   */
  @Override
  public CaseContext readCaseContext(final String key) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    String objectAsString;
    try {
      objectAsString = cloudDataStore.retrieveObject(CASE_BUCKET, key);
    } catch (StorageException e) {
      log.info("Exception while retrieving case context object from cloud", e);
      throw e;
    }
    CaseContext caseObject;
    try {
      caseObject = mapper.readValue(objectAsString, CaseContext.class);
    } catch (JsonProcessingException e) {
      log.info("Exception while de-serializing case context object from JSON", e);
      throw e;
    }
    log.info(
        "CaseContext object  with code = "
            + caseObject.getCaseId()
            + " has just been retrieved from the cloud");
    return caseObject;
  }
}
