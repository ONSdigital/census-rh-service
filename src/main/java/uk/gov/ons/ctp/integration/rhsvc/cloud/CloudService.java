package uk.gov.ons.ctp.integration.rhsvc.cloud;

public interface CloudService {
    void storeObject(final String bucket, final String key, final String value);
    String retrieveObject(final String bucket, final String key);


}
