package uk.gov.ons.ctp.integration.rhsvc.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.AbortedException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.model.CollectionCase;

/** This class unit tests the FirestoreDataStore class. It uses Mockito to simulate Firestore. */
public class FirestoreDataStoreTest {

  private static final String TEST_SCHEMA = "IT_TEST_SCHEMA";

  private static FirestoreDataStore firestoreDataStore = new FirestoreDataStore();

  private Firestore firestore = Mockito.mock(Firestore.class);

  @Before
  public void setUp() {
    ReflectionTestUtils.setField(firestoreDataStore, "firestore", firestore);
  }

  @Test
  public void testStoreObject() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);

    ApiFuture<WriteResult> apiFuture =
        mockFirestoreForExpectedStore(TEST_SCHEMA, case1.getId(), case1, null);

    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);
    Mockito.verify(apiFuture).get();
  }

  @Test
  public void testStoreObject_fails() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);

    ExecutionException firestoreException =
        new ExecutionException("fake Firestore exception", null);
    mockFirestoreForExpectedStore(TEST_SCHEMA, case1.getId(), case1, firestoreException);

    boolean exceptionCaught = false;
    try {
      firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to create object"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("fake Firestore exception"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testStoreObject_detectsContention() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);

    String exceptionMessage = "ABORTED: Too much contention on these documents. Please try again.";
    Exception firestoreException =
        new AbortedException(exceptionMessage, null, Mockito.mock(StatusCode.class), true);

    mockFirestoreForExpectedStore(TEST_SCHEMA, case1.getId(), case1, firestoreException);

    boolean exceptionCaught = false;
    try {
      firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);
    } catch (DataStoreContentionException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("contention on schema 'IT_TEST_SCHEMA'"));
      assertTrue(
          e.getCause().getMessage(), e.getCause().getMessage().contains("Too much contention"));
      exceptionCaught = true;
    }
    assertTrue("Failed to detect datastore contention", exceptionCaught);
  }

  @Test
  public void testRetrieveObject_found() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);

    mockFirestoreRetrieveObject(TEST_SCHEMA, case1.getId(), null, case1);

    // Verify that retrieveObject returns the expected result
    Optional<CollectionCase> retrievedCase1 =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, case1.getId());
    assertTrue(retrievedCase1.isPresent());
    assertEquals(case1, retrievedCase1.get());
  }

  @Test
  public void testRetrieveObject_notFound() throws Exception {
    String unknownId = UUID.randomUUID().toString();

    mockFirestoreRetrieveObject(TEST_SCHEMA, unknownId, null);

    // Submit a read for unknown object
    Optional<CollectionCase> retrievedCase1 =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, unknownId);

    // Verify that reading a non existent object returns an empty result set
    assertTrue(retrievedCase1.isEmpty());
  }

  @Test
  public void testRetrieveObject_failsWithMultipleResultsFound() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);

    // Force failure by making the retrieve return more than 1 result object
    mockFirestoreRetrieveObject(TEST_SCHEMA, case1.getId(), null, case1, case1);

    // Verify that retrieveObject fails because of more than 1 result
    boolean exceptionCaught = false;
    try {
      firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, case1.getId());
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Firestore returned more than 1"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testSearch_noResults() throws Exception {
    mockFirestoreSearch(TEST_SCHEMA, "Bob", null, null);

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

    mockFirestoreSearch(TEST_SCHEMA, case1.getContact().getForename(), null, null, case1);

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

    mockFirestoreSearch(TEST_SCHEMA, "Smith", null, null, case1, case2);

    // Verify that search can find the  first case
    String[] searchCriteria = new String[] {"contact", "surname"};
    List<CollectionCase> retrievedCase1 =
        firestoreDataStore.search(CollectionCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    assertEquals(2, retrievedCase1.size());
    assertEquals(case1.getId(), retrievedCase1.get(0).getId());
    assertEquals(case1, retrievedCase1.get(0));
    assertEquals(case2.getId(), retrievedCase1.get(1).getId());
    assertEquals(case2, retrievedCase1.get(1));
  }

  @Test
  public void testSearch_failsWithFirestoreException() throws Exception {
    // Read test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    ExecutionException firestoreException =
        new ExecutionException("fake Firestore exception", null);
    mockFirestoreSearch(TEST_SCHEMA, "Smith", firestoreException, null, case1, case2);

    boolean exceptionCaught = false;
    try {
      String[] searchCriteria = new String[] {"contact", "surname"};
      firestoreDataStore.search(CollectionCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to search"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("fake Firestore exception"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testSearch_failsSerialisationException() throws Exception {
    // Read test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    RuntimeException serialisationException = new RuntimeException("Could not deserialize object");
    mockFirestoreSearch(TEST_SCHEMA, "Smith", null, serialisationException, case1, case2);

    boolean exceptionCaught = false;
    try {
      String[] searchCriteria = new String[] {"contact", "surname"};
      firestoreDataStore.search(CollectionCase.class, TEST_SCHEMA, searchCriteria, "Smith");
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to convert"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("Could not deserialize object"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testDelete_success() throws Exception {
    CollectionCase case1 = loadCaseFromFile(0);

    ApiFuture<WriteResult> apiFuture =
        mockFirestoreForExpectedDelete(TEST_SCHEMA, case1.getId(), null);

    // Delete it
    firestoreDataStore.deleteObject(TEST_SCHEMA, case1.getId());
    Mockito.verify(apiFuture).get();
  }

  @Test
  public void testDelete_failsWithFirestoreException() throws Exception {
    CollectionCase case1 = loadCaseFromFile(0);

    ExecutionException firestoreException =
        new ExecutionException("fake Firestore exception", null);
    mockFirestoreForExpectedDelete(TEST_SCHEMA, case1.getId(), firestoreException);

    // Attempt a deletion
    boolean exceptionCaught = false;
    try {
      firestoreDataStore.deleteObject(TEST_SCHEMA, case1.getId());
    } catch (CTPException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Failed to delete"));
      assertTrue(
          e.getCause().getMessage(),
          e.getCause().getMessage().contains("fake Firestore exception"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  @Test
  public void testDelete_onNonExistentObject() throws Exception {
    UUID nonExistantUUID = UUID.randomUUID();

    ApiFuture<WriteResult> apiFuture =
        mockFirestoreForAttemptedDelete(TEST_SCHEMA, nonExistantUUID.toString());

    // Attempt to delete a non existent object
    firestoreDataStore.deleteObject(TEST_SCHEMA, nonExistantUUID.toString());
    Mockito.verify(apiFuture).get();
  }

  private CollectionCase loadCaseFromFile(int caseOffset) throws Exception {
    List<CollectionCase> cases = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    CollectionCase caseData = cases.get(caseOffset);

    return caseData;
  }

  private ApiFuture<WriteResult> mockFirestoreForExpectedStore(
      String expectedSchema, String expectedKey, Object expectedValue, Exception exception)
      throws InterruptedException, ExecutionException {
    ApiFuture<WriteResult> apiFuture = genericMock(ApiFuture.class);
    if (exception == null) {
      Mockito.when(apiFuture.get()).thenReturn(null);
    } else {
      Mockito.when(apiFuture.get()).thenThrow(exception);
    }

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    Mockito.when(documentReference.set(eq(expectedValue))).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.document(eq(expectedKey))).thenReturn(documentReference);

    Mockito.when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);

    return apiFuture;
  }

  private void mockFirestoreRetrieveObject(
      String expectedSchema,
      String expectedSearchValue,
      Exception exception,
      CollectionCase... case1)
      throws InterruptedException, ExecutionException {
    mockFirestoreSearch(expectedSchema, expectedSearchValue, exception, null, case1);
  }

  private void mockFirestoreSearch(
      String expectedSchema,
      String expectedSearchValue,
      Exception searchException,
      Exception serialisationException,
      CollectionCase... resultData)
      throws InterruptedException, ExecutionException {

    ApiFuture<QuerySnapshot> apiFuture = genericMock(ApiFuture.class);

    if (searchException == null && serialisationException == null) {
      // Build list of results which are to be returned
      List<QueryDocumentSnapshot> results = new ArrayList<>();
      for (CollectionCase caseObj : resultData) {
        QueryDocumentSnapshot doc1 = Mockito.mock(QueryDocumentSnapshot.class);
        Mockito.when(doc1.toObject(eq(CollectionCase.class))).thenReturn(caseObj);
        results.add(doc1);
      }

      QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
      Mockito.when(querySnapshot.getDocuments()).thenReturn(results);

      Mockito.when(apiFuture.get()).thenReturn(querySnapshot);
    } else if (searchException != null) {
      Mockito.when(apiFuture.get()).thenThrow(searchException);
    } else {
      // SerialisationException
      List<QueryDocumentSnapshot> results = new ArrayList<>();
      QueryDocumentSnapshot doc1 = Mockito.mock(QueryDocumentSnapshot.class);
      Mockito.when(doc1.toObject(eq(CollectionCase.class))).thenThrow(serialisationException);
      results.add(doc1);

      QuerySnapshot querySnapshot = Mockito.mock(QuerySnapshot.class);
      Mockito.when(querySnapshot.getDocuments()).thenReturn(results);

      Mockito.when(apiFuture.get()).thenReturn(querySnapshot);
    }

    Query query = Mockito.mock(Query.class);
    Mockito.when(query.get()).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.whereEqualTo((FieldPath) any(), eq(expectedSearchValue)))
        .thenReturn(query);

    Mockito.when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);
  }

  private ApiFuture<WriteResult> mockFirestoreForExpectedDelete(
      String expectedSchema, String expectedKey, Exception exception)
      throws InterruptedException, ExecutionException {
    ApiFuture<WriteResult> apiFuture = genericMock(ApiFuture.class);
    if (exception == null) {
      Mockito.when(apiFuture.get()).thenReturn(null);
    } else {
      Mockito.when(apiFuture.get()).thenThrow(exception);
    }

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    Mockito.when(documentReference.delete()).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.document(eq(expectedKey))).thenReturn(documentReference);

    Mockito.when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);

    return apiFuture;
  }

  private ApiFuture<WriteResult> mockFirestoreForAttemptedDelete(
      String expectedSchema, String expectedKey) throws InterruptedException, ExecutionException {
    ApiFuture<WriteResult> apiFuture = genericMock(ApiFuture.class);
    Mockito.when(apiFuture.get()).thenReturn(null);

    DocumentReference documentReference = Mockito.mock(DocumentReference.class);
    Mockito.when(documentReference.delete()).thenReturn(apiFuture);

    CollectionReference collectionReference = Mockito.mock(CollectionReference.class);
    Mockito.when(collectionReference.document(eq(expectedKey))).thenReturn(documentReference);

    Mockito.when(firestore.collection(eq(expectedSchema))).thenReturn(collectionReference);

    return apiFuture;
  }

  @SuppressWarnings("unchecked")
  static <T> T genericMock(Class<? super T> classToMock) {
    return (T) Mockito.mock(classToMock);
  }
}
