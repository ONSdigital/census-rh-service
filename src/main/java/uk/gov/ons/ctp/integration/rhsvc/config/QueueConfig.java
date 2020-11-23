package uk.gov.ons.ctp.integration.rhsvc.config;

import java.util.Set;
import lombok.Data;

@Data
public class QueueConfig {
  private String eventExchange;
  private String deadLetterExchange;
  private String caseQueue;
  private String caseQueueDLQ;
  private String caseRoutingKey;
  private String uacQueue;
  private String uacQueueDLQ;
  private String uacRoutingKey;
  private String webformQueue;
  private String webformQueueDLQ;
  private String webformRoutingKey;
  private String responseAuthenticationRoutingKey;
  private Set<String> qidFilterPrefixes;
}
