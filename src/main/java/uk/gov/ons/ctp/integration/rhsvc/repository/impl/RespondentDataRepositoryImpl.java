package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudDataStore;
import uk.gov.ons.ctp.integration.rhsvc.cloud.FirestoreDataStore;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/** A RespondentDataRepository implementation for CRUD operations on Respondent data entities */
@Service
public class RespondentDataRepositoryImpl implements RespondentDataRepository {

  @Value("${GOOGLE_CLOUD_PROJECT}")
  String gcpProject;

  @Value("${googleStorage.caseSchemaName}")
  String caseSchemaName;

  @Value("${googleStorage.uacSchemaName}")
  String uacSchemaName;

  String caseSchema;
  String uacSchema;

  @Autowired private CloudDataStore cloudDataStore;

  public RespondentDataRepositoryImpl() {
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
   */
  @Override
  public void writeUAC(final UAC uac) throws CTPException {
    cloudDataStore.storeObject(uacSchema, uac.getUacHash(), uac);
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
    return cloudDataStore.retrieveObject(UAC.class, uacSchema, universalAccessCode);
  }

  /**
   * Write a CollectionCase object into the cloud data store.
   *
   * @param collectionCase - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeCollectionCase(final CollectionCase collectionCase) throws CTPException {
    cloudDataStore.storeObject(caseSchema, collectionCase.getId(), collectionCase);
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
  @Override
  public List<CollectionCase> readCollectionCasesByUprn(final String uprn) throws CTPException {
    // Run search
    String[] searchByUprnPath = new String[] {"address", "uprn"};
    List<CollectionCase> searchResults =
        cloudDataStore.search(CollectionCase.class, caseSchema, searchByUprnPath, uprn);

    return searchResults;
  }

  public void deleteJsonFromCloud(String schema, String key) throws CTPException {
    cloudDataStore.deleteObject(schema, key);
  }
}
