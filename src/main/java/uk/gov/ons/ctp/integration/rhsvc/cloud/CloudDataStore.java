package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.google.cloud.storage.StorageException;
import java.util.Optional;

public interface CloudDataStore {

  void storeObject(final String bucket, final String key, final String value);

  Optional<String> retrieveObject(final String bucket, final String key) throws StorageException;

  void deleteObject(final String key, final String bucket);
}
