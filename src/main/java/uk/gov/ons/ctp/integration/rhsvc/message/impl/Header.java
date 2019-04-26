package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header {

  private String type;
  private String source;
  private String channel;
  private Date dateTime;
  private String transactionId;
}
