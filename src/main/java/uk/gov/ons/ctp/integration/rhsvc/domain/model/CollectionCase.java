package uk.gov.ons.ctp.integration.rhsvc.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCase {

  private String id;
  private String caseRef;
  private String survey;
  private String collectionExerciseId;
  private String address;
  private String state;
  private String actionableFrom;
}