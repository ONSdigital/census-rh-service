package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.storage.StorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSDataStore;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

import java.io.IOException;
import java.util.Optional;

/**
 * A RespondentDataService implementation which encapsulates all business logic operating on the
 * Core Respondent Details entity model.
 */
@Service
public class RespondentDataServiceImpl implements RespondentDataService {
  private static final Logger log = LoggerFactory.getLogger(RespondentDataServiceImpl.class);

  private static final String UAC_BUCKET = "uac_bucket";
  private static final String CASE_BUCKET = "case_bucket";

  @Autowired private CloudDataStore cloudDataStore;

  RespondentDataServiceImpl() {
    this.cloudDataStore = new GCSDataStore();
  }

  /**
   * Serialize an UACContext object into JSON string and store it in the cloud
   *
   * @param uacContext - object to be serialised and stored in the cloud
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public void writeUACContext(final UACContext uacContext) throws CTPException {
    if (uacContext == null) {
      throw new CTPException(Fault.BAD_REQUEST);
    }
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    String universalAccessCode = uacContext.getUniversalAccessCode();
    try {
      jsonInString = mapper.writeValueAsString(uacContext);
    } catch (JsonProcessingException e) {
      log.with(universalAccessCode).error("Could not serialize UAC context object to JSON", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    writeJsonToCloud(
        jsonInString, universalAccessCode, UAC_BUCKET, "UAC context object stored in cloud");
  }

  /**
   * Read an UAC object from cloud and de-serialize to an UACContext object from JSON
   *
   * @param universalAccessCode - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public Optional<UACContext> readUACContext(final String universalAccessCode) throws CTPException {
    Optional<String> uacContextStrOpt =
        getJsonFromCloud(
            universalAccessCode,
            UAC_BUCKET,
            "Could not retrieve the UAC Context object from cloud");
    return deserializeUACContext(universalAccessCode, uacContextStrOpt);
  }

  /**
   * Serialize an CaseContext object into JSON string and store it in the cloud
   *
   * @param caseContext - object to be serialised and stored in the cloud
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public void writeCaseContext(final CaseContext caseContext) throws CTPException {
    if (caseContext == null) {
      throw new CTPException(Fault.BAD_REQUEST);
    }
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    String caseId = caseContext.getCaseId();
    try {
      jsonInString = mapper.writeValueAsString(caseContext);
    } catch (JsonProcessingException e) {
      log.with(caseId).error("Could not serialize CASE context object to JSON", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    writeJsonToCloud(jsonInString, caseId, CASE_BUCKET, "Case Context object stored in cloud");
  }

  /**
   * Read an Case object from cloud and de-serialize to an CaseContext object from JSON
   *
   * @param caseId - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws CTPException - this is the exception thrown to the client.
   */
  @Override
  public Optional<CaseContext> readCaseContext(final String caseId) throws CTPException {
    Optional<String> caseContextStrOpt;
    caseContextStrOpt =
        getJsonFromCloud(caseId, CASE_BUCKET, "Could not retrieve case context object from cloud");
    return deserializeCaseContext(caseId, caseContextStrOpt);
  }

  private void writeJsonToCloud(
      String jsonInString, String universalAccessCode, String uacBucket, String s)
      throws CTPException {
    try {
      cloudDataStore.storeObject(uacBucket, universalAccessCode, jsonInString);
    } catch (StorageException e) {
      log.with(universalAccessCode).error("Could not store UAC context object into cloud", e);
      throw new CTPException(Fault.SYSTEM_ERROR);
    }
    log.with(universalAccessCode).debug(s);
  }

  private Optional<UACContext> deserializeUACContext(
      String universalAccessCode, Optional<String> uacContextStrOpt) throws CTPException {
    Optional<UACContext> uacContextOpt = Optional.empty();
    if (uacContextStrOpt.isPresent()) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        uacContextOpt = Optional.of(mapper.readValue(uacContextStrOpt.get(), UACContext.class));
      } catch (IOException e) {
        log.with(universalAccessCode)
            .error("Could not de-serialize UAC context object from JSON", e);
        throw new CTPException(Fault.SYSTEM_ERROR, e);
      }
      log.with(universalAccessCode).debug("UAC Context object has been retrieved from the cloud");
    }
    return uacContextOpt;
  }

  private Optional<CaseContext> deserializeCaseContext(
      String caseId, Optional<String> caseContextStrOpt) throws CTPException {
    Optional<CaseContext> caseContextOpt = Optional.empty();
    if (caseContextStrOpt.isPresent()) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        caseContextOpt = Optional.of(mapper.readValue(caseContextStrOpt.get(), CaseContext.class));
      } catch (IOException e) {
        log.with(caseId).error("Could not de-serialize case context object from JSON", e);
        throw new CTPException(Fault.SYSTEM_ERROR, e);
      }
      log.with(caseId).debug("Case Context object has been retrieved from the cloud");
    }
    return caseContextOpt;
  }

  private Optional<String> getJsonFromCloud(String caseId, String caseBucket, String s)
      throws CTPException {
    Optional<String> caseContextStrOpt;
    try {
      caseContextStrOpt = cloudDataStore.retrieveObject(caseBucket, caseId);
    } catch (StorageException e) {
      log.with(caseId).error(s, e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    return caseContextStrOpt;
  }
}
