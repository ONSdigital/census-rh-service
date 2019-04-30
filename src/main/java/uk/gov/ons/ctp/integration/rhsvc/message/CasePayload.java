package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CasePayload {

  private CollectionCase collectionCase = new CollectionCase();
}
