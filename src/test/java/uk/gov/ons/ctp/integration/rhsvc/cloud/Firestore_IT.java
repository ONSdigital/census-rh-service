package uk.gov.ons.ctp.integration.rhsvc.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.ctp.common.FixtureHelper;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.time.DateTimeUtil;

/**
 * This class tests the FirestoreDataStore class by connecting to a real firestore project.
 *
 * <p>To run this code: This class tests Firestore using the firestore API. 1) Uncomment the @Ignore
 * annotations. 2) Make sure the Firestore environment variables are set. eg, I use:
 * GOOGLE_APPLICATION_CREDENTIALS =
 * /Users/peterbochel/.config/gcloud/application_default_credentials.json GOOGLE_CLOUD_PROJECT =
 * census-rh-peterb
 */
@Ignore
public class Firestore_IT {

  private static final String FIRESTORE_CREDENTIALS_ENV_NAME = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String FIRESTORE_PROJECT_ENV_NAME = "GOOGLE_CLOUD_PROJECT";
  private static final String TEST_SCHEMA = "IT_TEST_SCHEMA";

  private static FirestoreDataStore firestoreDataStore;

  @BeforeClass
  public static void setUp() {
    firestoreDataStore = new FirestoreDataStore();
    firestoreDataStore.connect();

    String googleCredentials = System.getenv(FIRESTORE_CREDENTIALS_ENV_NAME);
    String googleProjectName = System.getenv(FIRESTORE_PROJECT_ENV_NAME);
    System.out.printf(
        "Connecting to Firestore project '%s' using credentials at '%s'\n",
        googleProjectName, googleCredentials);
    Firestore firestore = FirestoreOptions.getDefaultInstance().getService();

    ReflectionTestUtils.setField(firestoreDataStore, "firestore", firestore);
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

    verifyStoredCase(case1, false, "2020-06-01T20:17:46.384Z");
    verifyStoredCase(case2, true, "2020-05-01T20:17:46.384Z");
  }

  private void verifyStoredCase(
      CollectionCase caze, boolean isAddrValid, String expectedCreatedDateTime) throws Exception {
    Optional<CollectionCase> retrievedCase =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, caze.getId());
    assertTrue(retrievedCase.isPresent());
    CollectionCase rcase = retrievedCase.get();
    assertEquals(caze, rcase);
    assertEquals(isAddrValid, rcase.isAddressInvalid());
    assertEquals(expectedCreatedDateTime, zuluDateTimeFormatter(rcase.getCreatedDateTime()));
  }

  @Test
  public void testReplaceObject() throws Exception {
    // Load test data
    CollectionCase case1 = loadCaseFromFile(0);
    CollectionCase case2 = loadCaseFromFile(1);

    // Add data to collection
    String id = case1.getId();
    firestoreDataStore.storeObject(TEST_SCHEMA, id, case1);

    // Verify that 1st case can be read back
    Optional<CollectionCase> retrievedCase =
        firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, id);
    assertEquals(case1, retrievedCase.get());

    // Replace contents with case2
    firestoreDataStore.storeObject(TEST_SCHEMA, id, case2);

    // Confirm contents of 'id', which was case1, is now case2
    retrievedCase = firestoreDataStore.retrieveObject(CollectionCase.class, TEST_SCHEMA, id);
    assertEquals(case2, retrievedCase.get());
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

    // Verify that search can find the first case
    String[] searchByForename = new String[] {"contact", "forename"};
    List<CollectionCase> retrievedCase1 =
        firestoreDataStore.search(
            CollectionCase.class, TEST_SCHEMA, searchByForename, case1.getContact().getForename());
    assertEquals(1, retrievedCase1.size());
    assertEquals(case1.getId(), retrievedCase1.get(0).getId());
    assertEquals(case1, retrievedCase1.get(0));

    // Verify that search can find the second case
    String[] searchByUprn = new String[] {"address", "region"};
    List<CollectionCase> retrievedCase2 =
        firestoreDataStore.search(CollectionCase.class, TEST_SCHEMA, searchByUprn, "E");
    assertEquals(2, retrievedCase2.size());
    assertEquals(case1, retrievedCase2.get(0));
    assertEquals(case2, retrievedCase2.get(1));
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
  public void testSearch_serialisationFailure() throws Exception {
    // Add test data to Firestore
    CollectionCase case1 = loadCaseFromFile(0);
    firestoreDataStore.storeObject(TEST_SCHEMA, case1.getId(), case1);

    // Verify that search can find the first case
    boolean exceptionCaught = false;
    try {
      String[] searchByForename = new String[] {"contact", "forename"};
      firestoreDataStore.search(
          String.class, TEST_SCHEMA, searchByForename, case1.getContact().getForename());
    } catch (Exception e) {
      assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("e"));
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
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

    // Attempt to delete a non existent object. Should not fail
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

  private String zuluDateTimeFormatter(Date date) {
    DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern(DateTimeUtil.DATE_FORMAT_IN_JSON).withZone(ZoneId.of("Z"));
    return formatter.format(date.toInstant());
  }
}
