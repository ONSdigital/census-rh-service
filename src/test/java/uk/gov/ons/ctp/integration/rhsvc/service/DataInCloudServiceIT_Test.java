package uk.gov.ons.ctp.integration.rhsvc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ons.ctp.integration.rhsvc.cloud.CloudService;
import uk.gov.ons.ctp.integration.rhsvc.cloud.GCSService;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Address;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DataInCloudServiceIT_Test {

    CloudService storage;
    DataInCloud cloud;
    String caseId;
    String uacCode;
    UACContext uac;
    CaseContext caseContext;

    @Before
    public void setup(){
        storage = new GCSService();
        cloud = new DataInCloudService(storage);
        caseId = "123456789";
        uacCode = "abcd-defg-1234";

        String case_type = "(H|HI|C|CI)";
        String questionaireId = "123456789";
        String transactionId = "123456789";
        String region = "EN";
        String timestamp = "2019-02-01T08:15:30+00:00";

        uac = new UACContext();
        uac.setUac(uacCode);
        uac.setCaseId(caseId);
        uac.setCaseType(case_type);
        uac.setQuestionnaireId(questionaireId);
        uac.setTransactionId(transactionId);
        uac.setRegion(region);
        uac.setTimestamp(timestamp);

        caseContext = new CaseContext();
        caseContext.setCaseId(caseId);
        Address address = new Address();
        address.setAddressLine1("Address line 1");
        address.setAddressLine2("Address line 2");
        address.setCity("city");

        caseContext.setAddress(address);
        caseContext.setCaseType(case_type);
        caseContext.setAddress(address);
        caseContext.setTimestamp(timestamp);
        caseContext.setCollectionExercise("collection exercise");
        caseContext.setPostcode("PO155XX");
        caseContext.setUprn("123456789012");
        caseContext.setRegion(region);
    }

    @Test
    public void writeAndReadUAC() {
        try {
            cloud.writeObject(uac);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Failed to write upload uac object to cloud storage");
        }
        UACContext uac2 = null;
        try {
            uac2 = cloud.readUac(uacCode);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to read uac object from cloud storage");
        }
        assertEquals(uac.getUac(), uac2.getUac());
        assertEquals(uac.getCaseId(), uac2.getCaseId());
        assertEquals(uac.getCaseType(), uac2.getCaseType());
        assertEquals(uac.getQuestionnaireId(), uac2.getQuestionnaireId());
        assertEquals(uac.getRegion(), uac2.getRegion());
        assertEquals(uac.getTimestamp(), uac2.getTimestamp());
    }

    @Test
    public void writeAndReadCase() {
        try {
            cloud.writeObject(caseContext);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Failed to write upload case object to cloud storage");
        }
        CaseContext caseContext2 = null;
        try {
            caseContext2 = cloud.readCase(caseId);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to read case object from cloud storage");
        }
        assertEquals(caseContext.getCaseType(), caseContext2.getCaseType());
        assertEquals(caseContext.getCaseId(), caseContext2.getCaseId());
        assertEquals(caseContext.getUprn(), caseContext2.getUprn());
        assertEquals(caseContext.getCollectionExercise(), caseContext2.getCollectionExercise());
        assertEquals(caseContext.getRegion(), caseContext2.getRegion());
        assertEquals(caseContext.getPostcode(), caseContext2.getPostcode());
        assertEquals(caseContext.getTimestamp(), caseContext2.getTimestamp());
    }
}