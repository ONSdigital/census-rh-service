package uk.gov.ons.ctp.integration.rhsvc.representation;

import lombok.Data;

/** Representation of address data */
@Data
public class AddressDTO {

  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String uprn;
}
