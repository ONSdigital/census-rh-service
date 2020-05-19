package uk.gov.ons.ctp.integration.rhsvc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
}
