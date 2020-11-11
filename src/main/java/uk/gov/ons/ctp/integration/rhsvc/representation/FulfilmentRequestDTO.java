package uk.gov.ons.ctp.integration.rhsvc.representation;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class FulfilmentRequestDTO {

  @NotNull private UUID caseId;

  @NotEmpty private List<@NotEmpty String> fulfilmentCodes;

  @NotNull private Date dateTime;

  private String clientIP;
}
