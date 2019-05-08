package uk.gov.ons.ctp.integration.rhsvc.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

  private String title;
  private String forename;
  private String surname;
  private String email;
  private String telNo;
}
