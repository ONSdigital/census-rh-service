package uk.gov.ons.ctp.integration.rhsvc.cloud;

import java.util.Optional;

public interface CloudDataStore {

  void storeObject(final String bucket, final String key, final String value);

  Optional<String> retrieveObject(final String bucket, final String key);

  void deleteObject(final String key, final String bucket);
}
