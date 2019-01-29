package uk.gov.ons.ctp.integration.rhsvc.cloud;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CloudStorageGCSServiceTest {


    public static final String CASE_ID_1 = "caseID_1";
    public static final String CASE_ID_2 = "caseID_2";
    public static final String UAC_1 = "uac1";
    public static final String UAC_2 = "uac2";
    public static final String DATA_ABOUT_THE_CASE = "data_about_the_case";
    CloudStorage cloudStorage = new CloudStorageGCSService();

    @Test()
    public void test_0_createNewUacDetails() {
        cloudStorage.createUacDetails(UAC_1, UAC_1 + ":" + CASE_ID_1);
        cloudStorage.createUacDetails(UAC_2, UAC_2 + ":" + CASE_ID_2);
    }

    @Test()
    public void test_0_createUacDetailsWithNewDetails() {
        cloudStorage.createUacDetails(UAC_1, UAC_1 + ":" + CASE_ID_2);
    }

    @Test()
    public void test_1_createUacDetails_Object_Exists() {
        cloudStorage.createUacDetails(UAC_1, UAC_1 + ":" + CASE_ID_1);
    }

    @Test
    public void test_2_createCaseDetails() {
        cloudStorage.createCaseDetails(CASE_ID_1, CASE_ID_1 + ":" + DATA_ABOUT_THE_CASE);
    }

    @Test
    public void test_21_createCaseDetails() {
        cloudStorage.createCaseDetails(CASE_ID_2, CASE_ID_2 + ":" + DATA_ABOUT_THE_CASE);
    }

    @Test()
    public void test_3_createCaseDetails_Object_Exists() {
        cloudStorage.createCaseDetails(CASE_ID_1, CASE_ID_1 + ":" + DATA_ABOUT_THE_CASE);
    }

    @Test
    public void test_3_deleteUacDetails() {
        cloudStorage.deleteUacDetails(UAC_1);
    }


    @Test()
    public void test_4_deleteNonExistingUacDetails() {
        cloudStorage.deleteUacDetails(UAC_1);
    }

    @Test
    public void test_5_deleteCaseDetails() {
        cloudStorage.deleteUacDetails(CASE_ID_1);
    }

    @Test()
    public void test_6_deleteNonExistendCaseDetails() {
        cloudStorage.deleteCaseDetails(CASE_ID_1);
    }
}