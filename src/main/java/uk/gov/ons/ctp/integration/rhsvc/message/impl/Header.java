package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header {

  private String type = "myType";
  private String source = "mySource";
  private String channel = "theEnglishChannel";
  private Timestamp dateTime;
  private String transactionId = "myTransId";
}
