package uk.gov.ons.ctp.integration.rhsvc.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionCase;

public class FirestoreTest {

  private static final String FIRESTORE_CREDENTIALS_ENV_NAME = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String FIRESTORE_PROJECT_ENV_NAME = "GOOGLE_CLOUD_PROJECT";

  private static Firestore db;
  
  @BeforeClass
  public static void setUp() {
    String googleCredentials = System.getenv(FIRESTORE_CREDENTIALS_ENV_NAME);
    String googleProjectName = System.getenv(FIRESTORE_PROJECT_ENV_NAME);

    System.out.printf("Connecting to Firestore project '%s' using credentials at '%s'\n", googleProjectName, googleCredentials);

    FirestoreTest.db = FirestoreOptions.getDefaultInstance().getService();
  }
  
  @Before
  public void clearTestData() { 
    ApiFuture<QuerySnapshot> future = db.collection("TEST_CollectionCase").get();
    List<QueryDocumentSnapshot> documents;
    try {
      documents = future.get().getDocuments();
    } catch (CancellationException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    for (QueryDocumentSnapshot document : documents) {
      document.getReference().delete();
    }
  }
  
  @Test
  public void testFirestoreSearchById() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    // Add data to collection
    writeCollectionCase(case1);
    writeCollectionCase(case2);

    // Verify that 1st case can be read back
    CollectionCase retrievedCase1 = readCollectionCaseById(case1.getId(), 1);
    assertEquals(case1.getId(), retrievedCase1.getId());
    assertEquals(case1, retrievedCase1);
  }

  @Test
  public void testFirestoreSearchByUknownId() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    // Add data to collection
    writeCollectionCase(case1);
    writeCollectionCase(case2);

    // Verify that 1st case can be read back
    String unknownUUID = UUID.randomUUID().toString();
    CollectionCase retrievedCase = readCollectionCaseById(unknownUUID, 0);
    assertNull(retrievedCase);
  }

  @Test
  public void testFirestoreSearchByField() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);
    
    // Add data to collection
    writeCollectionCase(case1);
    writeCollectionCase(case2);
    
    // Verify that 1st case can be read back
    CollectionCase retrievedCase1 = readCollectionCaseByUprn(case1.getAddress().getUprn(), 1);
    assertEquals(case1.getId(), retrievedCase1.getId());
    assertEquals(case1, retrievedCase1);
    
    // Verify that 2nd case can be read back
    CollectionCase retrievedCase2 = readCollectionCaseByUprn(case2.getAddress().getUprn(), 1);
    assertEquals(case2.getId(), retrievedCase2.getId());
    assertEquals(case2, retrievedCase2);
  }

  @Test
  public void testFirestoreSearchByFieldNoResults() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);
    
    // Add data to collection
    writeCollectionCase(case1);
    writeCollectionCase(case2);
    
    // Verify null returned when searching for unknown uprn
    CollectionCase retrievedCase = readCollectionCaseByUprn("1231231123", 0);
    assertNull(retrievedCase);
  }

  @Test
  public void testDeleteObject() throws Exception { 
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    
    // Add data to collection
    writeCollectionCase(case1);
    
    // Make sure object has been created
    readCollectionCaseById(case1.getId(), 1);
    
    // Delete
    DocumentReference docRef = db.collection("TEST_CollectionCase").document(case1.getId());
    ApiFuture<WriteResult> result = docRef.delete();
    result.get();
    
    // Make sure object no longer readable
    readCollectionCaseById(case1.getId(), 0);
  }

  @Test
  public void testDeleteNonExistantObject() throws Exception { 
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    
    // Add data to collection
    writeCollectionCase(case1);
    
    // Delete
    String unknownId = "2223423443443";
    DocumentReference docRef = db.collection("TEST_CollectionCase").document(unknownId);
    ApiFuture<WriteResult> result = docRef.delete();
    result.get();
  }

  private CollectionCase loadCaseFromFile(int caseOffset) throws Exception {
    List<CollectionCase> cases = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    CollectionCase caseData = cases.get(caseOffset);

    caseData.setId(UUID.randomUUID().toString());
    
    return caseData;
  }

  private CollectionCase readCollectionCaseById(String id, int expectedSize)
      throws InterruptedException, ExecutionException {
    FieldPath path2 = FieldPath.documentId();
    ApiFuture<QuerySnapshot> query = db.collection("TEST_CollectionCase").whereEqualTo(path2, id).get();
    List<QueryDocumentSnapshot> documents = query.get().getDocuments();
    
    assertEquals(expectedSize, documents.size());
    
    if (expectedSize == 0) {
      return null;
    }
    CollectionCase retrievedCase1 = documents.get(0).toObject(CollectionCase.class);
    
    return retrievedCase1;
  }

  private CollectionCase readCollectionCaseByUprn(String uprn, int expectedSize)
      throws InterruptedException, ExecutionException {
    FieldPath path2 = FieldPath.of("address", "uprn");
    ApiFuture<QuerySnapshot> query = db.collection("TEST_CollectionCase").whereEqualTo(path2, uprn).get();
    List<QueryDocumentSnapshot> documents = query.get().getDocuments();

    assertEquals(expectedSize, documents.size());

    if (expectedSize == 0) {
      return null;
    }
    CollectionCase retrievedCase1 = documents.get(0).toObject(CollectionCase.class);
    
    return retrievedCase1;
  }

  private void writeCollectionCase(CollectionCase caseDetails)
      throws InterruptedException, ExecutionException {
    ApiFuture<WriteResult> result = db.collection("TEST_CollectionCase")
        .document(caseDetails.getId())
        .set(caseDetails);
    result.get();
  }
}
