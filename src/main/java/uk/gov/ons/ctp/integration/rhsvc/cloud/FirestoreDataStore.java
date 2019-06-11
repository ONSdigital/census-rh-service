package uk.gov.ons.ctp.integration.rhsvc.cloud;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
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
import uk.gov.ons.ctp.common.event.model.CollectionCase;

@Service
public class FirestoreDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(FirestoreDataStore.class);
  
  private static final String FIRESTORE_CREDENTIALS_ENV_NAME = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String FIRESTORE_PROJECT_ENV_NAME = "GOOGLE_CLOUD_PROJECT";
  
  private Firestore firestore;

  
  public FirestoreDataStore() {
    String googleCredentials = System.getenv(FIRESTORE_CREDENTIALS_ENV_NAME);
    String googleProjectName = System.getenv(FIRESTORE_PROJECT_ENV_NAME);

    log.info("Connecting to Firestore project '{}' using credentials at '{}'", googleProjectName, googleCredentials);
    firestore = FirestoreOptions.getDefaultInstance().getService();
  }

  
  /**
   * Write object to Firestore collection.
   * If the object already exists then it will be overwritten.
   *
   * @param schema - holds the name of the collection that the object will be added to.
   * @param key - identifies the object within the collection.
   * @param value - is the object to be written to Firestore. It must provide get methods that allow access to its contents.
   * @throws CTPException if any failure was detected interacting with Firestore.
   */
  @Override
  public void storeObject(final String schema, final String key, final Object value) throws CTPException
  {
    log.debug("Saving object to Firestore. Schema '{}' with key '{}'" + schema, key);
    
    ApiFuture<WriteResult> result = null;
      result = firestore.collection(schema)
          .document(key)
          .set(value);

    // Wait for Firestore to complete
    try {
      result.get();
    } catch (CancellationException | ExecutionException | InterruptedException e) {
      String failureMessage = "Failed to create object in Firestore. Schema: " + schema + " with key " + key;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }


  /**
   * Read an object from Firestore.
   * @param schema - is the name of the collection which holds the object.
   * @param key - identifies the object within the collection.
   *
   * @return - Optional containing the object if it was found, otherwise the optional will contain null.
   */
  @Override
  public <T> Optional<T> retrieveObject(Class<T> target, final String schema, final String key) throws CTPException {
    log.debug("Fetching object from Firestore. Schema '{}' with key '{}'", schema, key);
    
    // Submit request to firestore
    FieldPath fieldPathForId = FieldPath.documentId();
    ApiFuture<QuerySnapshot> query = firestore
        .collection(schema)
        .whereEqualTo(fieldPathForId, key)
        .get();

    // Wait for Firestore to fetch object
    QuerySnapshot querySnapshot;
    try {
      querySnapshot = query.get();
    } catch (CancellationException | ExecutionException | InterruptedException e) {
      // PMB maybe catch exception 
      String failureMessage = "Failed to read object from Firestore. Schema: " + schema + " with key " + key;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
    
    // Validate the query results
    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
    if (documents.isEmpty()) {
      return Optional.empty();
    } else if (documents.size() != 1) {
      throw new CTPException(Fault.SYSTEM_ERROR, "Firestore returned incorrect number of objects. Returned " + documents.size() + " objects for Schema: " + schema + " with key " + key);
    }
    
    // Convert to result type
    T targetObject = (T) documents.get(0).toObject(target);

    return Optional.of(targetObject);
  }

  public <T> Optional<T> search(Class<T> target, final String schema, String[] fieldPath, String value) throws CTPException {
    FieldPath searchPath = FieldPath.of(fieldPath);
    ApiFuture<QuerySnapshot> query = firestore.collection(schema).whereEqualTo(searchPath, value).get();

    QuerySnapshot querySnapshot = query.get();
    
    List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();

    T[] results = documents.toArray(target[]);
    
    return retrievedCase1;
  }


  /**
   * Delete an object from Firestore. 
   * No error is thrown if the object doesn't exist.
   * 
   * @param scheama - is the name of the collection which holds the object.
   * @param key - identifies the object within the collection.
   */
  @Override
  public void deleteObject(final String schema, final String key) throws CTPException {
    log.debug("Deleting object from Firestore. Schema '{}' with key '{}'", schema, key);
    
    DocumentReference docRef = firestore.collection(schema).document(key);
    ApiFuture<WriteResult> result = docRef.delete();

    try {
      result.get();
    } catch (CancellationException | ExecutionException | InterruptedException e) {
      String failureMessage = "Failed to delete object from Firestore. Schema: " + schema + " with key " + key;
      throw new CTPException(Fault.SYSTEM_ERROR, e, failureMessage);
    }
  }
}
