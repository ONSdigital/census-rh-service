package uk.gov.ons.ctp.integration.rhsvc.domain.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseContext implements Serializable {
    private String caseId;
    private String uprn;
    private Address address;
    private String postcode;
    private String caseType;
    private String region;
    private String collectionExercise;
    private String timestamp;
}