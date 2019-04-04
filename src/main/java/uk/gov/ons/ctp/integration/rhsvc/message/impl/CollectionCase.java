package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCase {

  private String id;
  private String caseRef;
  private String survey;
  private String collectionExerciseId;
  private String sampleUnitRef;
  private String address;
  private String state;
  private String actionableFrom;

  //  private String id = "bbd55984-0dbf-4499-bfa7-0aa4228700e9";
  //  private String caseRef = "10000000010";
  //  private String survey = "Census";
  //  private String collectionExerciseId = "n66de4dc-3c3b-11e9-b210-d663bd873d93";
  //  private String sampleUnitRef = "";
  //  private String address;
  //  private String state = "actionable";
  //  private String actionableFrom = "2011-08-12T20:17:46.384Z";

  //  private Map<String, String> properties = new HashMap<>();
  //
  //  /**
  //   * Getter for Map of message name value pairs
  //   *
  //   * @return Map of message elements
  //   */
  //  @JsonAnyGetter
  //  public Map<String, String> getProperties() {
  //    return properties;
  //  }
  //
  //  /**
  //   * Setter for Map of message name value pairs
  //   *
  //   * @param key message attribute name
  //   * @param value message attribute value
  //   */
  //  @JsonAnySetter
  //  public void add(String key, String value) {
  //    properties.put(key, value);
  //  }
}
