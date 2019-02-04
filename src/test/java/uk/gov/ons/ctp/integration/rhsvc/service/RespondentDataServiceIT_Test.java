package uk.gov.ons.ctp.integration.rhsvc.service;

import org.junit.Before;
import org.junit.Test;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Address;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RespondentDataServiceIT_Test {

  private DataInCloud cloud;
  private String caseId;
  private String uacCode;
  private UACContext uac;
  private CaseContext caseContext;

  @Before
  public void setup() {
    cloud = new RespondentDataService();
    caseId = "123456789";
    uacCode = "abcd-defg-1234";
    String case_type = "H";
    String questionaireId = "123456789";
    String transactionId = "123456789";
    String region = "EN";
    String timestamp = "2019-02-01T08:15:30+00:00";
    String addressLine1 = "Address line 1";
    String addressLine2 = "Address line 2";
    String city = "city";
    String collection_exercise = "collection exercise";
    String postCode = "PO155XX";
    String uprn = "123456789012";

    uac = new UACContext();
    uac.setUac(uacCode);
    uac.setCaseId(caseId);
    uac.setCaseType(case_type);
    uac.setQuestionnaireId(questionaireId);
    uac.setTransactionId(transactionId);
    uac.setRegion(region);
    uac.setTimestamp(timestamp);

    Address address = new Address();
    address.setAddressLine1(addressLine1);
    address.setAddressLine2(addressLine2);
    address.setCity(city);
    caseContext = new CaseContext();
    caseContext.setCaseId(caseId);
    caseContext.setAddress(address);
    caseContext.setCaseType(case_type);
    caseContext.setAddress(address);
    caseContext.setTimestamp(timestamp);
    caseContext.setCollectionExercise(collection_exercise);
    caseContext.setPostcode(postCode);
    caseContext.setUprn(uprn);
    caseContext.setRegion(region);
  }

  @Test
  public void writeAndReadUACContext() throws IOException {
    cloud.writeUACContext(uac);
    UACContext uac2 = cloud.readUacContext(uacCode);
    assertEquals(uac.getUac(), uac2.getUac());
    assertEquals(uac.getCaseId(), uac2.getCaseId());
    assertEquals(uac.getCaseType(), uac2.getCaseType());
    assertEquals(uac.getQuestionnaireId(), uac2.getQuestionnaireId());
    assertEquals(uac.getRegion(), uac2.getRegion());
    assertEquals(uac.getTimestamp(), uac2.getTimestamp());
  }

  @Test
  public void writeAndReadCaseContext() throws IOException {
    cloud.writeCaseContext(caseContext);
    CaseContext caseContext2 = cloud.readCaseContext(caseId);
    assertEquals(caseContext.getCaseType(), caseContext2.getCaseType());
    assertEquals(caseContext.getCaseId(), caseContext2.getCaseId());
    assertEquals(caseContext.getUprn(), caseContext2.getUprn());
    assertEquals(caseContext.getCollectionExercise(), caseContext2.getCollectionExercise());
    assertEquals(caseContext.getRegion(), caseContext2.getRegion());
    assertEquals(caseContext.getPostcode(), caseContext2.getPostcode());
    assertEquals(caseContext.getTimestamp(), caseContext2.getTimestamp());
  }
}
