package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PostalFulfilmentRequestDTO extends FulfilmentRequestDTO {

  @Size(max = 12)
  @LoggingScope(scope = Scope.SKIP)
  private String title;

  @Size(max = 60)
  @LoggingScope(scope = Scope.SKIP)
  private String forename;

  @Size(max = 60)
  @LoggingScope(scope = Scope.SKIP)
  private String surname;
}
