package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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

    @Test()
    @Ignore
    public void storeAndReceive_Uac() {
        cloudDataStore.storeObject(UAC_BUCKET, UAC_1, CASE_ID_1);
        String value = cloudDataStore.retrieveObject(UAC_BUCKET, UAC_1);
        assertEquals(CASE_ID_1, value);

        cloudDataStore.storeObject(UAC_BUCKET, UAC_1, UAC_CONTENT);
        String value2 = cloudDataStore.retrieveObject(UAC_BUCKET, UAC_1);
        assertEquals(UAC_CONTENT, value2);
    }

    @Test()
    @Ignore
    public void storeAndReceive_Case() {
        cloudDataStore.storeObject(CASE_BUCKET, CASE_ID_1, CASE_ID_2);
        String value2 = cloudDataStore.retrieveObject(CASE_BUCKET, CASE_ID_1);
        assertEquals(CASE_ID_2, value2);

        cloudDataStore.storeObject(CASE_BUCKET, CASE_ID_1, CASE_CONTENT);
        String value = cloudDataStore.retrieveObject(CASE_BUCKET, CASE_ID_1);
        assertEquals(CASE_CONTENT, value);
//        String s = generatePath("GCSDataStoreIT_Test", "GCSDataStoreIT_Test", "store", "UAC");

//        System.out.println(s);
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

    private static <T> List<T> actuallyLoadFixtures(
            final Class<T[]> clazz,
            final String callerClassName,
            final String callerMethodName,
            final String qualifier)
            throws Exception {
        List<T> dummies = null;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        String clazzName = clazz.getSimpleName().replaceAll("[\\[\\]]", "");
        String path = generatePath(callerClassName, clazzName, callerMethodName, qualifier);
        try {
            File file = new File(ClassLoader.getSystemResource(path).getFile());
            dummies = Arrays.asList(mapper.readValue(file, clazz));
        } catch (Throwable t) {
            System.out.println("Problem loading fixture {} reason {}"); //, path, t.getMessage());
            throw t;
        }
        return dummies;
    }

    /**
     * Format the path name to the json file, using optional params ie
     * "uk/gov/ons/ctp/response/action/thing/ThingTest.testThingOK.blueThings.json"
     *
     * @param callerClassName the name of the class that made the initial call
     * @param clazzName the type of object to deserialize and return in a List
     * @param methodName the name of the method in the callerClass that made the initial call
     * @param qualifier further quaification is a single method may need to have two collections of
     *     the same type, qualified
     * @return the constructed path string
     */
    private static String generatePath(
            final String callerClassName,
            final String clazzName,
            final String methodName,
            final String qualifier) {
        return callerClassName.replaceAll("\\.", "/")
                + "."
                + ((methodName != null) ? (methodName + ".") : "")
                + clazzName
                + "."
                + ((qualifier != null) ? (qualifier + ".") : "")
                + "json";
    }
}