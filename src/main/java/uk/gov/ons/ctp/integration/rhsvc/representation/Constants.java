package uk.gov.ons.ctp.integration.rhsvc.representation;

public interface Constants {

  public static final String PHONENUMBER_RE = "^447[0-9]{9}$";
  public static final String OPTIONAL_PHONENUMBER_RE = "^$|" + PHONENUMBER_RE;
}
