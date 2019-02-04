package uk.gov.ons.ctp.integration.rhsvc.cloud;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GCSDataStoreIT_Test {

  private static final String CASE_BUCKET = "case_bucket";
  private static final String CASE_ID_1 = "caseId_1";
  private static final String CASE_ID_2 = "caseId_2";
  private static final String UAC_BUCKET = "uac_bucket";
  private static final String UAC_1 = "abcd-defg-1234";
  private static final String CASE_CONTENT =
      "\"caseId\" :  { \"caseId\" : \"123456789\",  \"uprn\": \"123456789012\",  \"address\":\n"
          + "\n"
          + "{ \"line1\": \"2A Priors Way\",     \"line2: \"Olivers Battery\",     \"city\": \"Winchester\"   }\n"
          + ",\n"
          + "\n"
          + "  \"postcode\": \"SO22 4HJ\",\n"
          + "\n"
          + "  \"caseType\": \"(H|HI|C|CI)\", //TBC\n"
          + "\n"
          + "  \"region\": \"EN\",\n"
          + "\n"
          + "  \"collectionExercise\": \"census\", // TBC\n"
          + "\n"
          + "  \"timestamp\": \"2007-12-03T10:15:30+00:00\"\n"
          + "\n"
          + "}";
  private static final String UAC_CONTENT =
      "\"UAC\": \n"
          + "\n"
          + "{   \"UAC\": \"abcd-defg-1234\",   \"questionnaireId\": \"123456789\",   \"transactionId\": \"123456789\",   \"caseType\": \"(H|HI|C|CI)\".   \"region\": \"EN\",   \"caseId\": \"123456789\",   \"timestamp\": \"2007-12-03T10:15:30+00:00\" }\n";

  CloudDataStore cloudDataStore = new GCSDataStore();

  @Test()
  public void storeAndReceive_Uac() {
    cloudDataStore.storeObject(UAC_BUCKET, UAC_1, CASE_ID_1);
    String value = cloudDataStore.retrieveObject(UAC_BUCKET, UAC_1);
    assertEquals(CASE_ID_1, value);

    cloudDataStore.storeObject(UAC_BUCKET, UAC_1, UAC_CONTENT);
    String value2 = cloudDataStore.retrieveObject(UAC_BUCKET, UAC_1);
    assertEquals(UAC_CONTENT, value2);
  }

  @Test()
  public void storeAndReceive_Case() {
    cloudDataStore.storeObject(CASE_BUCKET, CASE_ID_1, CASE_ID_2);
    String value2 = cloudDataStore.retrieveObject(CASE_BUCKET, CASE_ID_1);
    assertEquals(CASE_ID_2, value2);

    cloudDataStore.storeObject(CASE_BUCKET, CASE_ID_1, CASE_CONTENT);
    String value = cloudDataStore.retrieveObject(CASE_BUCKET, CASE_ID_1);
    assertEquals(CASE_CONTENT, value);
  }
}
