package uk.gov.ons.ctp.integration.rhsvc.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Address;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.Contact;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.UAC;
import uk.gov.ons.ctp.integration.rhsvc.service.RespondentDataService;

// import uk.gov.ons.ctp.integration.rhsvc.domain.model.Address;

public class RespondentDataServiceIT_Test {

  private RespondentDataService cloud;
  private String caseId;
  private String uacHash;
  private UAC uac;
  private CollectionCase collectionCase;

  @Before
  public void setup() {
    cloud = new RespondentDataServiceImpl();
    caseId = "c45de4dc-3c3b-11e9-b210-d663bd873d93";
    uacHash = "72C84BA99D77EE766E9468A0DE36433A44888E5DEC4AFB84F8019777800B7364"; // SHA-256 hash of
    // UAC
    String caseRef = "10000000010";
    String survey = "Census";
    String collectionExerciseId = "n66de4dc-3c3b-11e9-b210-d663bd873d93";
    String addressLines = "Address lines";
    String state = "actionable";
    String actionableFrom = "2011-08-12T20:17:46.384Z";
    String active = "true";
    String questionnaireId = "1110000009";
    String caseType = "H";
    String region = "E";
    Address address = new Address();
    address.setAddressLine1("1 main street");
    address.setAddressLine2("upper upperingham");
    address.setAddressLine3("");
    address.setTownName("upton");
    address.setPostcode("UP103UP");
    address.setRegion("E");
    address.setLatitude("50.863849");
    address.setLongitude("-1.229710");
    address.setUprn("XXXXXXXXXXXXX");
    address.setArid("XXXXX");
    address.setAddressType("CE");
    address.setEstabType("XXX");
    Contact contact = new Contact();
    contact.setTitle("Ms");
    contact.setForename("jo");
    contact.setSurname("smith");
    contact.setEmail("me@example.com");
    contact.setTelNo("+447890000000");

    collectionCase = new CollectionCase();
    collectionCase.setId(caseId);
    collectionCase.setCaseRef(caseRef);
    collectionCase.setSurvey(survey);
    collectionCase.setCollectionExerciseId(collectionExerciseId);
    collectionCase.setAddress(address);
    collectionCase.setContact(contact);
    collectionCase.setState(state);
    collectionCase.setActionableFrom(actionableFrom);

    uac = new UAC();
    uac.setUacHash(uacHash);
    uac.setCaseId(caseId);
    uac.setActive(active);
    uac.setQuestionnaireId(questionnaireId);
    uac.setCaseType(caseType);
    uac.setRegion(region);
    uac.setCollectionExerciseId(collectionExerciseId);
  }

  @Ignore
  @Test
  public void writeAndReadUAC() throws Exception {
    cloud.writeUAC(uac);
    Optional<UAC> uac2Opt = cloud.readUAC(uacHash);
    UAC uac2 = uac2Opt.get();
    assertEquals(uac.getUacHash(), uac2.getUacHash());
    assertEquals(uac.getActive(), uac2.getActive());
    assertEquals(uac.getCaseId(), uac2.getCaseId());
    assertEquals(uac.getCaseType(), uac2.getCaseType());
    assertEquals(uac.getQuestionnaireId(), uac2.getQuestionnaireId());
    assertEquals(uac.getRegion(), uac2.getRegion());
    assertEquals(uac.getCollectionExerciseId(), uac2.getCollectionExerciseId());
  }

  @Ignore
  @Test
  public void writeAndReadCollectionCase() throws Exception {
    cloud.writeCollectionCase(collectionCase);
    Optional<CollectionCase> collectionCase2Opt = cloud.readCollectionCase(caseId);
    CollectionCase collectionCase2 = collectionCase2Opt.get();
    assertEquals(collectionCase.getId(), collectionCase2.getId());
    assertEquals(collectionCase.getCaseRef(), collectionCase2.getCaseRef());
    assertEquals(collectionCase.getSurvey(), collectionCase2.getSurvey());
    assertEquals(
        collectionCase.getCollectionExerciseId(), collectionCase2.getCollectionExerciseId());
    assertEquals(collectionCase.getAddress(), collectionCase2.getAddress());
    assertEquals(collectionCase.getContact(), collectionCase2.getContact());
    assertEquals(collectionCase.getState(), collectionCase2.getState());
    assertEquals(collectionCase.getActionableFrom(), collectionCase2.getActionableFrom());
  }
}
