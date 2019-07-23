package uk.gov.ons.ctp.integration.rhsvc.representation;

public interface Constants {

  public static final String PHONENUMBER_RE =
      "^(?:(?:\\(?(?:0(?:0|11)\\)?[\\s-]?\\(?|\\+)44\\)?[\\s-]?(?:\\(?0\\)?[\\s-]?)?)|"
          + "(?:\\(?0))(?:(?:\\d{5}\\)?[\\s-]?\\d{4,5})|(?:\\d{4}\\)?[\\s-]?(?:\\d{5}|\\d{3}[\\s-]"
          + "?\\d{3}))|(?:\\d{3}\\)?[\\s-]?\\d{3}[\\s-]?\\d{3,4})|(?:\\d{2}\\)?[\\s-]?\\d{4}[\\s-]"
          + "?\\d{4}))(?:[\\s-]?(?:x|ext\\.?|\\#)\\d{3,4})?$";
  public static final String OPTIONAL_PHONENUMBER_RE = "^$|" + PHONENUMBER_RE;
}
