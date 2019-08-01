package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.Date;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The request object when a SMS fulfilment is requested for a given case */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SMSFulfilmentRequestDTO {

  @NotNull private UUID caseId;

  @NotNull
  @Size(max = 20)
  @Pattern(regexp = Constants.PHONENUMBER_RE)
  private String telNo;

  @NotNull
  @Size(max = 12)
  private String fulfilmentCode;

  @NotNull private Date dateTime;
}
