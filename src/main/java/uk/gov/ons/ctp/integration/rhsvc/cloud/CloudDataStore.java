package uk.gov.ons.ctp.integration.rhsvc.cloud;

public interface CloudDataStore {

  void storeObject(final String bucket, final String key, final String value);

  String retrieveObject(final String bucket, final String key);
}
