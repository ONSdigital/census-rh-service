package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.cloud.RetryableCloudDataStore;
import uk.gov.ons.ctp.common.domain.CaseType;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.repository.RespondentDataRepository;

/** A RespondentDataRepository implementation for CRUD operations on Respondent data entities */
@Service
public class RespondentDataRepositoryImpl implements RespondentDataRepository {
  private RetryableCloudDataStore cloudDataStore;

  @Value("${GOOGLE_CLOUD_PROJECT}")
  private String gcpProject;

  @Value("${cloud-storage.case-schema-name}")
  private String caseSchemaName;

  @Value("${cloud-storage.uac-schema-name}")
  private String uacSchemaName;

  String caseSchema;
  private String uacSchema;

  private static final String[] SEARCH_BY_UPRN_PATH = new String[] {"address", "uprn"};

  @PostConstruct
  public void init() {
    caseSchema = gcpProject + "-" + caseSchemaName.toLowerCase();
    uacSchema = gcpProject + "-" + uacSchemaName.toLowerCase();
  }

  @Autowired
  public RespondentDataRepositoryImpl(RetryableCloudDataStore retryableCloudDataStore) {
    this.cloudDataStore = retryableCloudDataStore;
  }

  /**
   * Stores a UAC object into the cloud data store.
   *
   * @param uac - object to be stored in the cloud
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeUAC(final UAC uac) throws CTPException {
    cloudDataStore.storeObject(uacSchema, uac.getUacHash(), uac, uac.getCaseId());
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
    return cloudDataStore.retrieveObject(UAC.class, uacSchema, universalAccessCodeHash);
  }

  /**
   * Write a CollectionCase object into the cloud data store.
   *
   * @param collectionCase - is the case to be stored in the cloud.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public void writeCollectionCase(final CollectionCase collectionCase) throws CTPException {
    String id = collectionCase.getId();
    cloudDataStore.storeObject(caseSchema, id, collectionCase, id);
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
   * Read case objects from cloud based on its uprn. Filter by non HI, latest case, and optionally
   * whether the case is valid.
   *
   * @param uprn - is the uprn that the target case(s) must contain.
   * @param onlyValid - true if only valid cases to be returned; false otherwise
   * @return - Optional containing 1 de-serialised version of the stored object. If no matching
   *     cases are found then an empty Optional is returned.
   * @throws CTPException - if a cloud exception was detected.
   */
  @Override
  public Optional<CollectionCase> readNonHILatestCollectionCaseByUprn(
      final String uprn, boolean onlyValid) throws CTPException {
    List<CollectionCase> searchResults =
        cloudDataStore.search(CollectionCase.class, caseSchema, SEARCH_BY_UPRN_PATH, uprn);
    return filterLatestValidNonHiCollectionCaseSearchResults(searchResults, onlyValid);
  }

  /**
   * Filter search results returning Latest !addressInvalid non HI case
   *
   * @param searchResults - Search results found in dataStore by searching by uprn
   * @return Optional of the resulting collection case or Empty
   */
  private Optional<CollectionCase> filterLatestValidNonHiCollectionCaseSearchResults(
      final List<CollectionCase> searchResults, boolean onlyValid) {
    return searchResults.stream()
        .filter(c -> !c.getCaseType().equals(CaseType.HI.name()))
        .filter(c -> onlyValid ? !c.isAddressInvalid() : true)
        .max(Comparator.comparing(CollectionCase::getCreatedDateTime));
  }
}
