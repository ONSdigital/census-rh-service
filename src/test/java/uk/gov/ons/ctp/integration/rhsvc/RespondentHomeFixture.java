package uk.gov.ons.ctp.integration.rhsvc;

import java.util.Date;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.event.EventPublisher.EventType;
import uk.gov.ons.ctp.common.event.model.Address;
import uk.gov.ons.ctp.common.event.model.CaseEvent;
import uk.gov.ons.ctp.common.event.model.CasePayload;
import uk.gov.ons.ctp.common.event.model.CollectionCase;
import uk.gov.ons.ctp.common.event.model.Contact;
import uk.gov.ons.ctp.common.event.model.Header;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RespondentHomeFixture {

  public static final String QID_01 = "0130000003130730";
  public static final String QID_11 = "1130000001203937";
  public static final String QID_12 = "1230000003125553";
  public static final String QID_13 = "1330000000000124";
  public static final String QID_14 = "1430000001539033";
  public static final String QID_21 = "2130000001529907";
  public static final String QID_31 = "3130000002100533";

  public static final String A_QID = QID_01;

  public static final String EXPECTED_JSON_CONTENT_TYPE = "application/json";

  public static CollectionCase createCollectionCase() {
    CollectionCase collectionCase = new CollectionCase();
    collectionCase.setId("900000000");
    collectionCase.setCaseRef("10000000010");
    collectionCase.setSurvey("Census");
    collectionCase.setCollectionExerciseId("n66de4dc-3c3b-11e9-b210-d663bd873d93");
    collectionCase.setActionableFrom("2011-08-12T20:17:46.384Z");
    collectionCase.setHandDelivery(true);
    collectionCase.setAddressInvalid(false);
    collectionCase.setCreatedDateTime(new Date());

    Address address = collectionCase.getAddress();
    address.setAddressLine1("1 main street");
    address.setAddressLine2("upper upperingham");
    address.setAddressLine3("");
    address.setTownName("upton");
    address.setPostcode("UP103UP");
    address.setRegion("E");
    address.setLatitude("50.863849");
    address.setLongitude("-1.229710");
    address.setUprn("XXXXXXXXXXXXX");
    address.setAddressType("CE");
    address.setEstabType("XXX");

    Contact contact = collectionCase.getContact();
    contact.setTitle("Ms");
    contact.setForename("jo");
    contact.setSurname("smith");
    contact.setTelNo("+447890000000");

    return collectionCase;
  }

  public static CaseEvent createCaseUpdatedEvent() {
    CaseEvent caseEvent = new CaseEvent();
    CasePayload casePayload = caseEvent.getPayload();
    CollectionCase collectionCase = createCollectionCase();
    casePayload.setCollectionCase(collectionCase);

    Header header = new Header();
    header.setType(EventType.CASE_UPDATED);
    header.setDateTime(new Date());
    header.setTransactionId("c45de4dc-3c3b-11e9-b210-d663bd873d93");
    caseEvent.setEvent(header);

    return caseEvent;
  }
}
