package uk.gov.ons.ctp.integration.rhsvc.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String region; // E, W or N
  private String latitude;
  private String longitude;
  private String uprn;
  private String arid;
  private String addressType;
  private String estabType;
}
