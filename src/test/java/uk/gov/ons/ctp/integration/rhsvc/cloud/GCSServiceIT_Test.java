package uk.gov.ons.ctp.integration.rhsvc.cloud;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GCSServiceIT_Test {


    public static final String CASE_BUCKET = "case_bucket";
    public static final String CASE_ID_1 = "caseId_1";
    public static final String CASE_ID_2 = "caseId_2";
    public static final String UAC_BUCKET = "uac_bucket";
    public static final String UAC_1 = "abcd-defg-1234";
    public static final String CASE_CONTENT = "\"caseId\" :  { \"caseId\" : \"123456789\",  \"uprn\": \"123456789012\",  \"address\":\n" +
            "\n" +
            "{ \"line1\": \"2A Priors Way\",     \"line2: \"Olivers Battery\",     \"city\": \"Winchester\"   }\n" +
            ",\n" +
            "\n" +
            "  \"postcode\": \"SO22 4HJ\",\n" +
            "\n" +
            "  \"caseType\": \"(H|HI|C|CI)\", //TBC\n" +
            "\n" +
            "  \"region\": \"EN\",\n" +
            "\n" +
            "  \"collectionExercise\": \"census\", // TBC\n" +
            "\n" +
            "  \"timestamp\": \"2007-12-03T10:15:30+00:00\"\n" +
            "\n" +
            "}";
    public static final String UAC_CONTENT = "\"UAC\": \n" +
            "\n" +
            "{   \"UAC\": \"abcd-defg-1234\",   \"questionnaireId\": \"123456789\",   \"transactionId\": \"123456789\",   \"caseType\": \"(H|HI|C|CI)\".   \"region\": \"EN\",   \"caseId\": \"123456789\",   \"timestamp\": \"2007-12-03T10:15:30+00:00\" }\n";

    CloudService cloudService = new GCSService();

    @Test()
    public void test_0_storeAndReceive_Uac() {
        cloudService.storeObject(UAC_BUCKET, UAC_1, CASE_ID_1);
        String value = cloudService.retrieveObject(UAC_BUCKET, UAC_1);
        Assert.assertTrue(CASE_ID_1.equals(value));

        cloudService.storeObject(UAC_BUCKET, UAC_1, UAC_CONTENT);
        String value2 = cloudService.retrieveObject(UAC_BUCKET, UAC_1);
        Assert.assertTrue(UAC_CONTENT.equals(value2));
    }

    @Test()
    public void test_0_storeAndReceive_Case() {
        cloudService.storeObject(CASE_BUCKET, CASE_ID_1, CASE_ID_2);
        String value2 = cloudService.retrieveObject(CASE_BUCKET, CASE_ID_1);
        Assert.assertTrue(CASE_ID_2.equals(value2));

        cloudService.storeObject(CASE_BUCKET, CASE_ID_1, CASE_CONTENT);
        String value = cloudService.retrieveObject(CASE_BUCKET, CASE_ID_1);
        Assert.assertTrue(CASE_CONTENT.equals(value));
    }
}