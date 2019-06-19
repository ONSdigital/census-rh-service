package uk.gov.ons.ctp.integration.rhsvc.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionCase;

/**
 * This class tests the FirestoreDataStore class by connecting to a real firestore project. 
 * 
 * To run this code: This class tests Firestore using the firestore API.
 * 1) Uncomment the @Ignore annotations.
 * 2) Make sure the Firestore environment variables are set. eg, I use:
 *   GOOGLE_APPLICATION_CREDENTIALS = /Users/peterbochel/.config/gcloud/application_default_credentials.json
 *   GOOGLE_CLOUD_PROJECT = census-rh-peterb
 */
public class FirestoreDataStoreTest {

  private static final String TEST_SCHEMA = "IT_TEST_SCHEMA";

  private static FirestoreDataStore firestoreDataStore= new FirestoreDataStore();
  
  private Firestore firestore = Mockito.mock(Firestore.class);

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(firestoreDataStore, "firestore", firestore);
  }

  @Test
  public void testStoreObject() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);

    mockFirestoreForExpectedStore(TEST_SCHEMA, case1.getId(), case1);
    
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);
  }

  @Test
  public void testRetrieveObject_found() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);

    mockFirestoreRetrieveObject(case1);
    
    // Verify that retrieveObject returns the expected result
    Optional<CollectionCase> retrievedCase1 =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, case1.getId());
    assertTrue(retrievedCase1.isPresent());
    assertEquals(case1, retrievedCase1.get());
  }

  @Test
  public void testRetrieveObject_notFound() throws Exception {
    mockFirestoreRetrieveObject();
    
    // Submit a read for unknown object
    String unknownId = UUID.randomUUID().toString();
    Optional<CollectionCase> retrievedCase1 =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, unknownId);
    
    // Verify that reading a non existent object returns an empty result set
    assertTrue(retrievedCase1.isEmpty());
  }

  @Test
  public void testSearch_noResults() throws Exception {
    mockFirestoreSearch();
    
    // Verify that there are no results when searching for unknown forename
    String[] searchCriteria = new String[] {"contact", "forename"};
    List<CollectionCase> retrievedCase1 =
        firestoreDataStore.search(CollectionCase.class, TEST_SCHEMA, searchCriteria, "Bob");
    assertTrue(retrievedCase1.isEmpty());
  }

  @Test
  public void testSearch_singleResult() throws Exception {
    // Read test data
    CollectionCase case1 = loadCaseFromFile(0);

    mockFirestoreSearch(case1);

    // Verify that search can find the  first case
    String[] searchCriteria = new String[] {"contact", "forename"};
    List<CollectionCase> retrievedCase1 =
        firestoreDataStore.search(
            CollectionCase.class, TEST_SCHEMA, searchCriteria, case1.getContact().getForename());
    assertEquals(1, retrievedCase1.size());
    assertEquals(case1.getId(), retrievedCase1.get(0).getId());
    assertEquals(case1, retrievedCase1.get(0));
  }

  @Test
  public void testSearch_multipleResults() throws Exception {
    // Read test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    mockFirestoreSearch(case1, case2);
    
    // Verify that search can find the  first case
    String[] searchCriteria = new String[] {"contact", "surname"};
    List<CollectionCase> retrievedCase1 =
        firestoreDataStore.search(
            CollectionCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    assertEquals(2, retrievedCase1.size());
    assertEquals(case1.getId(), retrievedCase1.get(0).getId());
    assertEquals(case1, retrievedCase1.get(0));
    assertEquals(case2.getId(), retrievedCase1.get(1).getId());
    assertEquals(case2, retrievedCase1.get(1));
  }

  @Test
  public void testDelete_success() throws Exception {
    CollectionCase case1 = loadCaseFromFile(0);

    mockFirestoreForExpectedDelete(TEST_SCHEMA, case1.getId());

    // Delete it
    firestoreDataStore.deleteObject(TEST_SCHEMA, case1.getId());
  }

  @Test
  public void testDelete_onNonExistentObject() throws Exception {
    mockFirestoreForAttemptedDelete(TEST_SCHEMA);

    // Attempt to delete a non existent object
    UUID nonExistantUUID = UUID.randomUUID();
    firestoreDataStore.deleteObject(TEST_SCHEMA, nonExistantUUID.toString());
  }

  private CollectionCase loadCaseFromFile(int caseOffset) throws Exception {
    List<CollectionCase> cases = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    CollectionCase caseData = cases.get(caseOffset);

    return caseData;
  }

  private void mockFirestoreForExpectedStore(String expectedSchema, String expectedKey, Object expectedValue) throws InterruptedException, ExecutionException {
    ApiFuture apiFuture = Mockito.mock(ApiFuture.class);
    Mockito.when(apiFuture.get()).thenReturn(null);

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    Mockito.when(documentReference.set((Object) any())).thenReturn(apiFuture);
    
    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.document(any())).thenReturn(documentReference);

    Mockito.when(firestore.collection(any())).thenReturn(collectionReference);
  }

  private void mockFirestoreRetrieveObject(CollectionCase... case1)
      throws InterruptedException, ExecutionException {
    mockFirestoreSearch(case1);
  }

  private void mockFirestoreSearch(CollectionCase... case1)
      throws InterruptedException, ExecutionException {
    List<QueryDocumentSnapshot> results = new ArrayList<>();
    
    for (CollectionCase caseObj : case1) {
      QueryDocumentSnapshot doc1 = Mockito.mock(QueryDocumentSnapshot.class);
      Mockito.when(doc1.toObject(any())).thenReturn(caseObj);
      results.add(doc1);    
    }
    
    QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
    Mockito.when(querySnapshot.getDocuments()).thenReturn(results);
    
    ApiFuture future = Mockito.mock(ApiFuture.class);
    Mockito.when(future.get()).thenReturn(querySnapshot);

    Query query = Mockito.mock(Query.class);
    Mockito.when(query.get()).thenReturn(future);
    
    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.whereEqualTo((FieldPath) any(), any())).thenReturn(query);
  
    Mockito.when(firestore.collection(any())).thenReturn(collectionReference);
  }

  private void mockFirestoreForExpectedDelete(String expectedSchema, String expectedKey) throws InterruptedException, ExecutionException {
    ApiFuture apiFuture = Mockito.mock(ApiFuture.class);
    Mockito.when(apiFuture.get()).thenReturn(null);

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    Mockito.when(documentReference.delete()).thenReturn(apiFuture);
    
    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.document(any())).thenReturn(documentReference);

    Mockito.when(firestore.collection(any())).thenReturn(collectionReference);
  }

  private void mockFirestoreForAttemptedDelete(String expectedSchema) throws InterruptedException, ExecutionException {
    ApiFuture apiFuture = Mockito.mock(ApiFuture.class);
    Mockito.when(apiFuture.get()).thenReturn(null);

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    Mockito.when(documentReference.delete()).thenReturn(apiFuture);
    
    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.document(any())).thenReturn(documentReference);

    Mockito.when(firestore.collection(any())).thenReturn(collectionReference);
  }
}
