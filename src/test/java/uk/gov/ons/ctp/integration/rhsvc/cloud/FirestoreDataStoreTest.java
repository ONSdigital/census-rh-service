package uk.gov.ons.ctp.integration.rhsvc.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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

  private static final String FIRESTORE_CREDENTIALS_ENV_NAME = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String FIRESTORE_PROJECT_ENV_NAME = "GOOGLE_CLOUD_PROJECT";
  private static final String TEST_SCHEMA = "IT_TEST_SCHEMA";

  private static FirestoreDataStore firestoreDataStore;

  @BeforeClass
  public static void setUp() {
    String googleCredentials = System.getenv(FIRESTORE_CREDENTIALS_ENV_NAME);
    String googleProjectName = System.getenv(FIRESTORE_PROJECT_ENV_NAME);
    System.out.printf(
        "Connecting to Firestore project '%s' using credentials at '%s'\n",
        googleProjectName, googleCredentials);

    firestoreDataStore = new FirestoreDataStore();
  }

  @Before
  public void clearTestData() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    // Add data to collection
    firestoreDataStore.deleteObject(TEST_SCHEMA, case1.getId());
    firestoreDataStore.deleteObject(TEST_SCHEMA, case2.getId());
  }
  
  @Test
  public void testStoreAndRetrieve() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    // Add data to collection
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);
    firestoreDataStore.storeObject(TEST_SCHEMA, case2.getId(), case2);

    // Verify that 1st case can be read back
    Optional<CollectionCase> retrievedCase1 =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, case1.getId());
    assertEquals(case1.getId(), retrievedCase1.get().getId());
    assertEquals(case1, retrievedCase1.get());
  }

  @Test
  public void testRetrieveObject_unknownObject() throws Exception {
    // Chuck an object into firestore
    CollectionCase case1 = loadCaseFromFile(0);
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);

    // Verify that reading a non existent object returns null
    String unknownUUID = UUID.randomUUID().toString();
    Optional<CollectionCase> retrievedCase =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, unknownUUID);
    assertTrue(retrievedCase.isEmpty());
  }

  @Test
  public void testSearch_multipleResults() throws Exception {
    // Read test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    // Add data to collection
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);
    firestoreDataStore.storeObject(TEST_SCHEMA, case2.getId(), case2);

    // Verify that search can find the  first case
    String[] searchCriteria = new String[] {"contact", "forename"};
    List<CollectionCase> retrievedCase1 =
        firestoreDataStore.search(
            CollectionCase.class, TEST_SCHEMA, searchCriteria, case1.getContact().getForename());
    assertEquals(1, retrievedCase1.size());
    assertEquals(case1.getId(), retrievedCase1.get(0).getId());
    assertEquals(case1, retrievedCase1.get(0));

    // Verify that search can find the second case
    List<CollectionCase> retrievedCase2 =
        firestoreDataStore.search(
            CollectionCase.class, TEST_SCHEMA, searchCriteria, case2.getContact().getForename());
    assertEquals(1, retrievedCase2.size());
    assertEquals(case2.getId(), retrievedCase2.get(0).getId());
    assertEquals(case2, retrievedCase2.get(0));
  }

  @Test
  public void testSearch_noResults() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);

    // Verify that there are no results when searching for unknown forename
    String[] searchCriteria = new String[] {"contact", "forename"};
    List<CollectionCase> retrievedCase1 =
        firestoreDataStore.search(CollectionCase.class, TEST_SCHEMA, searchCriteria, "Bob");
    assertTrue(retrievedCase1.isEmpty());
  }

  @Test
  public void testDelete_success() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);

    // Verify that the case can be read back
    Optional<CollectionCase> retrievedCase =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, case1.getId());
    assertEquals(case1, retrievedCase.get());

    // Delete it
    firestoreDataStore.deleteObject(TEST_SCHEMA, case1.getId());

    // Now confirm that we can no longer read the deleted case
    retrievedCase =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, case1.getId());
    assertTrue(retrievedCase.isEmpty());
  }

  @Test
  public void testDelete_onNonExistantObject() throws Exception {
    // Load test data, just so that Firestore has some data loaded
    CollectionCase case1 = loadCaseFromFile(0);
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);

    // Attempt to delete a non existant object
    UUID nonExistantUUID = UUID.randomUUID();
    firestoreDataStore.deleteObject(TEST_SCHEMA, nonExistantUUID.toString());

    // Confirm that firestore can't read the case
    Optional<CollectionCase> retrievedCase =
        firestoreDataStore.retrieveObject(
            CollectionCase.class, TEST_SCHEMA, nonExistantUUID.toString());
    assertTrue(retrievedCase.isEmpty());
  }

  private CollectionCase loadCaseFromFile(int caseOffset) throws Exception {
    List<CollectionCase> cases = FixtureHelper.loadClassFixtures(CollectionCase[].class);
    CollectionCase caseData = cases.get(caseOffset);

    return caseData;
  }
}
