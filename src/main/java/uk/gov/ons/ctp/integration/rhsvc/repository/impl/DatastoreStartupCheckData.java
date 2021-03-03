package uk.gov.ons.ctp.integration.rhsvc.repository.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class DatastoreStartupCheckData {
  private String timestamp;
  private String hostname;
}
