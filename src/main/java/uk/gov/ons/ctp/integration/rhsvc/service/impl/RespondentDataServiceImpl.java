package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.storage.StorageException;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSDataStore;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

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
   * @throws JsonProcessingException
   */
  @Override
  public void writeUACContext(final UACContext uacContext) throws CTPException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    try {
      jsonInString = mapper.writeValueAsString(uacContext);
    } catch (JsonProcessingException e) {
      log.error("Exception while serializing UAC context object to JSON", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    try {
      cloudDataStore.storeObject(UAC_BUCKET, uacContext.getUac(), jsonInString);
    } catch (StorageException e) {
      log.error("Exception while storing UAC context object into cloud", e);
      throw e;
    }
    log.debug("UAC object  with code = {} stored in cloud", uacContext.getUac());
  }

  /**
   * Read an UAC object from cloud and de-serialize to an UACContext object from JSON
   *
   * @param key - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws IOException
   */
  @Override
  public Optional<UACContext> readUACContext(final String uac) throws CTPException {
    Optional<String> uacContextStrOpt;
    try {
      uacContextStrOpt = cloudDataStore.retrieveObject(UAC_BUCKET, uac);
    } catch (Exception e) {
      log.error("Exception while retrieving UAC context object from cloud", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    Optional<UACContext> uacContextOpt = Optional.empty();
    if (uacContextStrOpt.isPresent()) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        uacContextOpt = Optional.of(mapper.readValue(uacContextStrOpt.get(), UACContext.class));
      } catch (IOException e) {
        log.error("Exception while de-serializing UAC context object from JSON", e);
        throw new CTPException(Fault.SYSTEM_ERROR, e);
      }
      log.debug("UAC object with code {} retrieved from cloud" + uacContextOpt.get().getUac());
    }
    return uacContextOpt;
  }

  /**
   * Serialize an CaseContext object into JSON string and store it in the cloud
   *
   * @param caseContext - object to be serialised and stored in the cloud
   * @throws JsonProcessingException
   */
  @Override
  public void writeCaseContext(final CaseContext caseContext) throws CTPException {
    ObjectMapper mapper = new ObjectMapper();
    String jsonInString;
    try {
      jsonInString = mapper.writeValueAsString(caseContext);
    } catch (JsonProcessingException e) {
      log.error("Exception while serializing case context object to JSON", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    try {
      cloudDataStore.storeObject(CASE_BUCKET, caseContext.getCaseId(), jsonInString);
    } catch (StorageException e) {
      log.error("Exception while storing case context object into cloud", e);
      throw e;
    }
    log.debug("Case object  with caseId {} stored in cloud", caseContext.getCaseId());
  }

  /**
   * Read an Case object from cloud and de-serialize to an CaseContext object from JSON
   *
   * @param caseId - the unique id of the object stored
   * @return - de-serialised version of the stored object
   * @throws IOException
   */
  @Override
  public Optional<CaseContext> readCaseContext(final String caseId) throws CTPException {
    Optional<String> caseContextStrOpt;
    try {
      caseContextStrOpt = cloudDataStore.retrieveObject(CASE_BUCKET, caseId);
    } catch (StorageException e) {
      log.error("Exception while retrieving case context object from cloud", e);
      throw new CTPException(Fault.SYSTEM_ERROR, e);
    }
    Optional<CaseContext> caseContextOpt = Optional.empty();
    if (caseContextStrOpt.isPresent()) {
      ObjectMapper mapper = new ObjectMapper();
      try {
        caseContextOpt = Optional.of(mapper.readValue(caseContextStrOpt.get(), CaseContext.class));
      } catch (IOException e) {
        log.error("Exception while de-serializing case context object from JSON", e);
        throw new CTPException(Fault.SYSTEM_ERROR, e);
      }
      log.debug(
          "CaseContext object with case id {} stored in cloud", caseContextOpt.get().getCaseId());
    }
    return caseContextOpt;
  }
}
