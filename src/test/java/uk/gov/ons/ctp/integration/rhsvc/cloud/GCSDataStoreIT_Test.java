package uk.gov.ons.ctp.integration.rhsvc.cloud;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GCSDataStoreIT_Test {

    private static final String CASE_BUCKET = "case_bucket";
    private static final String CASE_ID_1 = "caseId_1";
    private static final String CASE_ID_2 = "caseId_2";
    private static final String UAC_BUCKET = "uac_bucket";
    private static final String UAC_1 = "abcd-defg-1234";
    private static String CASE_CONTENT;
    private static String UAC_CONTENT;

    CloudDataStore cloudDataStore = new GCSDataStore();

    @Before
    public void setup() {
        UAC_CONTENT = readJsonFile("UAC");
        CASE_CONTENT = readJsonFile("CASE");
        System.out.println();
    }

    //    @Ignore
    @Test()
    public void storeAndReceive_Uac() {
        cloudDataStore.storeObject(UAC_BUCKET, UAC_1, CASE_ID_1);
        String value = cloudDataStore.retrieveObject(UAC_BUCKET, UAC_1);
        assertEquals(CASE_ID_1, value);

        cloudDataStore.storeObject(UAC_BUCKET, UAC_1, UAC_CONTENT);
        String value2 = cloudDataStore.retrieveObject(UAC_BUCKET, UAC_1);
        assertEquals(UAC_CONTENT, value2);
    }

//    @Ignore
    @Test()
    public void storeAndReceive_Case() {
        cloudDataStore.storeObject(CASE_BUCKET, CASE_ID_1, CASE_ID_2);
        String value2 = cloudDataStore.retrieveObject(CASE_BUCKET, CASE_ID_1);
        assertEquals(CASE_ID_2, value2);

        cloudDataStore.storeObject(CASE_BUCKET, CASE_ID_1, CASE_CONTENT);
        String value = cloudDataStore.retrieveObject(CASE_BUCKET, CASE_ID_1);
        assertEquals(CASE_CONTENT, value);
    }

    private String readJsonFile(final String filename) {
        Path path = Paths.get("src/test/resources/" + filename + ".json");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                sb.append(currentLine);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return sb.toString();
    }
}