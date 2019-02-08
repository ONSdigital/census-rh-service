package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Address;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CaseContext;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UACContext;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

public class RespondentDataServiceIT_Test {

  private RespondentDataService cloud;
  private String caseId;
  private String uacCode;
  private UACContext uac;
  private CaseContext caseContext;

  @Before
  public void setup() {
    cloud = new RespondentDataServiceImpl();
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
    uac.setUniversalAccessCode(uacCode);
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

  @Ignore
  @Test
  public void writeAndReadUACContext() throws Exception {
    cloud.writeUACContext(uac);
    Optional<UACContext> uac2Opt = cloud.readUACContext(uacCode);
    UACContext uac2 = uac2Opt.get();
    assertEquals(uac.getUniversalAccessCode(), uac2.getUniversalAccessCode());
    assertEquals(uac.getCaseId(), uac2.getCaseId());
    assertEquals(uac.getCaseType(), uac2.getCaseType());
    assertEquals(uac.getQuestionnaireId(), uac2.getQuestionnaireId());
    assertEquals(uac.getRegion(), uac2.getRegion());
    assertEquals(uac.getTimestamp(), uac2.getTimestamp());
  }

  @Ignore
  @Test
  public void writeAndReadCaseContext() throws Exception {
    cloud.writeCaseContext(caseContext);
    Optional<CaseContext> caseContext2Opt = cloud.readCaseContext(caseId);
    CaseContext caseContext2 = caseContext2Opt.get();
    assertEquals(caseContext.getCaseType(), caseContext2.getCaseType());
    assertEquals(caseContext.getCaseId(), caseContext2.getCaseId());
    assertEquals(caseContext.getUprn(), caseContext2.getUprn());
    assertEquals(caseContext.getCollectionExercise(), caseContext2.getCollectionExercise());
    assertEquals(caseContext.getRegion(), caseContext2.getRegion());
    assertEquals(caseContext.getPostcode(), caseContext2.getPostcode());
    assertEquals(caseContext.getTimestamp(), caseContext2.getTimestamp());
  }
}
