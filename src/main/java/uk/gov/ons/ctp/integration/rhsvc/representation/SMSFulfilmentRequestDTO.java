package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** The request object when a SMS fulfilment is requested for a given case */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SMSFulfilmentRequestDTO extends FulfilmentRequestDTO {

  @NotNull
  @Size(max = 20)
  @Pattern(regexp = Constants.PHONENUMBER_RE)
  @LoggingScope(scope = Scope.SKIP)
  private String telNo;
}
