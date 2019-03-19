package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCase {

  private String id = "bbd55984-0dbf-4499-bfa7-0aa4228700e9";
  private String caseRef = "10000000010";
  private String survey = "Census";
  private String collectionExerciseId = "n66de4dc-3c3b-11e9-b210-d663bd873d93";
  private String sampleUnitRef = "";
  private CaseAddress address;
  private String state = "actionable";
  private String actionableFrom = "2011-08-12T20:17:46.384Z";

  private Map<String, String> properties = new HashMap<>();

  /**
   * Getter for Map of message name value pairs
   *
   * @return Map of message elements
   */
  @JsonAnyGetter
  public Map<String, String> getProperties() {
    return properties;
  }

  /**
   * Setter for Map of message name value pairs
   *
   * @param key message attribute name
   * @param value message attribute value
   */
  @JsonAnySetter
  public void add(String key, String value) {
    properties.put(key, value);
  }
}
