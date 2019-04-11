package uk.gov.ons.ctp.integration.rhsvc.message;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Header {

  private String type;
  private String source;
  private String channel;
  private Timestamp dateTime;
  private String transactionId;
}
