package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

@Service
public class FirestoreDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(FirestoreDataStore.class);

  // Names of environment variables which firestore uses for connection information
  public static final String FIRESTORE_CREDENTIALS_ENV_NAME = "GOOGLE_APPLICATION_CREDENTIALS";
  public static final String FIRESTORE_PROJECT_ENV_NAME = "GOOGLE_CLOUD_PROJECT";

  @Autowired private Firestore firestore;

  /**
   * Write object to Firestore collection. If the object already exists then it will be overwritten.
   *
   * @param schema - holds the name of the collection that the object will be added to.
   * @param key - identifies the object within the collection.
   * @param value - is the object to be written to Firestore. It must provide get methods that allow
   *     access to its contents.
   * @throws CTPException if any failure was detected interacting with Firestore.
   */
  @Override
  public void storeObject(final String schema, final String key, final Object value)
      throws CTPException {
    log.debug("Saving object to Firestore. Schema '{}' with key '{}'" + schema, key);

    // Store the object
    ApiFuture<WriteResult> result = firestore.collection(schema).document(key).set(value);

    // Wait for Firestore to complete
    try {
      result.get();
      log.debug("Firstore save completed. Schema '{}' with key '{}'" + schema, key);
    } catch (Exception e) {
      String failureMessage =
          "Failed to create object in Firestore. Schema: " + schema + " with key " + key;
      log.error(e, failureMessage);
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }

  /**
   * Read an object from Firestore.
   *
   * @param schema - is the name of the collection which holds the object.
   * @param key - identifies the object within the collection.
   * @return - Optional containing the object if it was found, otherwise the optional will contain
   *     null.
   */
  @Override
  public <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key)
      throws CTPException {
    log.debug("Fetching object from Firestore. Schema '{}' with key '{}'", schema, key);

    // Submit read request to firestore
    FieldPath fieldPathForId = FieldPath.documentId();
    List<T> documents = runSearch(target, schema, fieldPathForId, key);

    // Squash results down to single document
    Optional<T> result = null;
    if (documents.isEmpty()) {
      result = Optional.empty();
      log.debug("Search didn't find any objects");
    } else if (documents.size() == 1) {
      result = Optional.of(documents.get(0));
      log.debug("Search found single result");
    } else {
      String failureMessage =
          "Firestore returned more than 1 result object. Returned "
              + documents.size()
              + " objects for Schema '"
              + schema
              + "' with key '"
              + key
              + "'";
      log.error(failureMessage);
      throw new CTPException(Fault.SYSTEM_ERROR, failureMessage);
    }

    return result;
  }

  /**
   * Runs a firestore object search. This returns objects whose field is equal to the search value.
   *
   * @param target is the object type that results should be returned in.
   * @param schema is the schema to search.
   * @param fieldPath is an array of strings that describe the path to the search field. eg, [
   *     "case", "addresss", "postcode" ]
   * @param searchValue is the value that the field must equal for it to be returned as a result.
   * @return An optional which contains a List of results.
   * @throws CTPException if anything goes wrong.
   */
  public <T> List<T> search(
      Class<T> target, final String schema, String[] fieldPath, String searchValue)
      throws CTPException {
    log.debug(
        "Searching Firestore. Schema '{}' with field path '{}' and search value of '{}'. "
            + "Expected results of type '{}'",
        schema,
        fieldPath,
        searchValue,
        target);

    // Run a query for a custom search path
    FieldPath searchPath = FieldPath.of(fieldPath);
    List<T> r = runSearch(target, schema, searchPath, searchValue);
    log.debug("Firestore search return {} results", r.size());

    return r;
  }

  private <T> List<T> runSearch(
      Class<T> target, final String schema, FieldPath fieldPath, String searchValue)
      throws CTPException {
    // Run a query
    ApiFuture<QuerySnapshot> query =
        firestore.collection(schema).whereEqualTo(fieldPath, searchValue).get();

    // Wait for query to complete and get results
    QuerySnapshot querySnapshot;
    try {
      querySnapshot = query.get();
    } catch (Exception e) {
      String failureMessage =
          "Failed to search schema '" + schema + "' by field '" + "'" + fieldPath;
      log.error(e, failureMessage);
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

    // Convert the results to Java objects
    List<T> results;
    try {
      results = documents.stream().map(d -> d.toObject(target)).collect(Collectors.toList());
    } catch (Exception e) {
      String failureMessage =
          "Failed to convert Firestore result to Java object. Target class '" + target + "'";
      log.error(e, failureMessage);
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }

    return results;
  }

  /**
   * Delete an object from Firestore. No error is thrown if the object doesn't exist.
   *
   * @param schema - is the name of the collection which holds the object.
   * @param key - identifies the object within the collection.
   */
  @Override
  public void deleteObject(final String schema, final String key) throws CTPException {
    log.debug("Deleting object from Firestore. Schema '{}' for key '{}'", schema, key);

    // Tell firestore to delete object
    DocumentReference docRef = firestore.collection(schema).document(key);
    ApiFuture<WriteResult> result = docRef.delete();

    // Wait for delete to complete
    try {
      result.get();
      log.debug("Firestore delete completed");
    } catch (Exception e) {
      String failureMessage =
          "Failed to delete object from Firestore. Schema: " + schema + " with key " + key;
      log.error(e, failureMessage);
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }
}
