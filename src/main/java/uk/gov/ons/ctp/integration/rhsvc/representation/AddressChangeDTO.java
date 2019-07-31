package uk.gov.ons.ctp.integration.rhsvc.representation;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** This is a request object which holds details about a changed address for a case. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressChangeDTO {

  @NotNull private UUID caseId;
  @JsonUnwrapped @Valid private AddressDTO address;
}
