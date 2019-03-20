package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePayload {

  private CollectionCase collectionCase;

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
