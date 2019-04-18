package uk.gov.ons.ctp.integration.rhsvc.domain.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

/** Message from RM to the RH Service */
public class CaseEvent {

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

  /** Convert message to String */
  public String toString() {
    if (properties == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      String key = entry.getKey();
      String value = entry.getValue();
      sb.append(key + ": " + value);
    }
    return sb.toString();
  }
}