package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirestoreStartupCheck {
  private String timestamp;
}
