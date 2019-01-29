package uk.gov.ons.ctp.integration.rhsvc.cloud;

public interface CloudStorage {
    void createUacDetails(final String uac, final String data);
    void deleteUacDetails(final String uac);

    void createCaseDetails(final String caseID, final String data);
    void deleteCaseDetails(final String caseID);

}
