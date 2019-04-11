package uk.gov.ons.ctp.integration.rhsvc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.ons.ctp.integration.rhsvc.domain.model.CollectionCase;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePayload {

  private CollectionCase collectionCase = new CollectionCase();
}
