package uk.gov.ons.ctp.integration.rhsvc.cloud;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

@Service
public class FirestoreDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(FirestoreDataStore.class);

  private static final String FIRESTORE_CREDENTIALS_ENV_NAME = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String FIRESTORE_PROJECT_ENV_NAME = "GOOGLE_CLOUD_PROJECT";

  private Firestore firestore;

  public FirestoreDataStore() {
    String googleCredentials = System.getenv(FIRESTORE_CREDENTIALS_ENV_NAME);
    String googleProjectName = System.getenv(FIRESTORE_PROJECT_ENV_NAME);
    log.debug(
        "Connecting to Firestore project '{}' using credentials at '{}'",
        googleProjectName,
        googleCredentials);

    firestore = FirestoreOptions.getDefaultInstance().getService();
  }

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
    } catch (Exception e) {
      String failureMessage =
          "Failed to create object in Firestore. Schema: " + schema + " with key " + key;
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
    ApiFuture<QuerySnapshot> query =
        firestore.collection(schema).whereEqualTo(fieldPathForId, key).get();

    // Wait for Firestore to fetch object
    QuerySnapshot querySnapshot;
    try {
      querySnapshot = query.get();
    } catch (Exception e) {
      String failureMessage =
          "Failed to read object from Firestore. Schema: " + schema + " with key " + key;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

    // Validate the query results
    Optional<T> result = null;
    if (documents.isEmpty()) {
      result = Optional.empty();
    } else if (documents.size() == 1) {
      // Convert to result type
      T foundObject = (T) documents.get(0).toObject(target);
      result = Optional.of(foundObject);
    } else if (documents.size() != 1) {
      throw new CTPException(
          Fault.SYSTEM_ERROR,
          "Firestore returned incorrect number of objects. Returned "
              + documents.size()
              + " objects for Schema: "
              + schema
              + " with key "
              + key);
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
    // Run a query
    FieldPath searchPath = FieldPath.of(fieldPath);
    ApiFuture<QuerySnapshot> query =
        firestore.collection(schema).whereEqualTo(searchPath, searchValue).get();

    // Get hold of query results
    QuerySnapshot querySnapshot;
    try {
      querySnapshot = query.get();
    } catch (Exception e) {
      throw new CTPException(
          Fault.SYSTEM_ERROR,
          e,
          "Search of schema '" + schema + "' failed for field '" + "'" + fieldPath);
    }
    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

    // Convert the results to Java objects
    List<T> results = documents.stream().map(d -> d.toObject(target)).collect(Collectors.toList());

    return results;
  }

  /**
   * Delete an object from Firestore. No error is thrown if the object doesn't exist.
   *
   * @param scheama - is the name of the collection which holds the object.
   * @param key - identifies the object within the collection.
   */
  @Override
  public void deleteObject(final String schema, final String key) throws CTPException {
    log.debug("Deleting object from Firestore. Schema '{}' with key '{}'", schema, key);

    // Tell firestore to delete object
    DocumentReference docRef = firestore.collection(schema).document(key);
    ApiFuture<WriteResult> result = docRef.delete();

    // Wait for delete to complete
    try {
      result.get();
    } catch (Exception e) {
      String failureMessage =
          "Failed to delete object from Firestore. Schema: " + schema + " with key " + key;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }
}
