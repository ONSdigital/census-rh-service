package uk.gov.ons.ctp.integration.rhsvc.message.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.common.time.DateTimeUtil;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Header {

  private String type;
  private String source;
  private String channel;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateTimeUtil.DATE_FORMAT_IN_JSON)
  private Date dateTime;

  private String transactionId;
}
