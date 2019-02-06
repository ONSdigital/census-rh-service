package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GCSDataStore implements CloudDataStore {
  private static final Logger log = LoggerFactory.getLogger(GCSDataStore.class);
  private static final String EUROPE_WEST_2 = "europe-west2";

  /**
   * Write object in Cloud Storage for UAC details inside specified bucket
   *
   * @param bucket - represents the bucket where the object will be stored
   * @param key - represents the unique object identifier in the bucket for the object stored
   * @param value - represents the string value representation of the object to be stored
   */
  @Override
  public void storeObject(final String bucket, final String key, final String value)
      throws StorageException {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    try {
      createBucket(bucket, storage);
    } catch (StorageException se) {
      // This Storage Exception is the only one declared on this API.
      // If a bucket exists, this exception will be thrown
      log.with(bucket).error("Bucket exists in the cloud storage.");
    }
    storeObject(bucket, key, value, storage);
  }

  /**
   * Read object in Cloud Storage for Case details inside specified bucket
   *
   * @param bucket - represents the bucket where the object will be stored
   * @param key - represents the unique object identifier in the bucket for the object stored
   * @return - JSON string representation of the object retrieved
   */
  @Override
  public Optional<String> retrieveObject(final String bucket, final String key)
      throws StorageException {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    if (null == bucket || bucket.length() == 0) {
      log.with(bucket).info("Bucket name was not set for object retrieval");
      return Optional.empty();
    }
    if (null == key || key.length() == 0) {
      log.with(key).info("Key was not set for object retrieval");
      return Optional.empty();
    }
    BlobId blobId = BlobId.of(bucket, key);
    Blob blob = storage.get(blobId);
    if (retrieveObject(bucket, key, blob)) return Optional.empty();

    String value = new String(blob.getContent());
    log.with(blobId).debug("Found BLOB: " + value);
    return Optional.of(value);
  }

  private void storeObject(String bucket, String key, String value, Storage storage)
      throws StorageException {
    BlobId blobId = BlobId.of(bucket, key);
    BlobInfo.Builder builder = BlobInfo.newBuilder(blobId);
    BlobInfo blobInfo = builder.setContentType("text/plain").build();
    storage.create(blobInfo, value.getBytes());
    log.with(key).debug("Blob has been created in cloud storage");
  }

  private void createBucket(String bucket, Storage storage) {
    storage.create(
        BucketInfo.newBuilder(bucket)
            // This is the cheapest option
            // See here for possible values: http://g.co/cloud/storage/docs/storage-classes
            .setStorageClass(StorageClass.COLDLINE)
            // As John mentioned, I used Europe west 2 - location where data will be held
            // Possible values: http://g.co/cloud/storage/docs/bucket-locations#location-mr
            .setLocation(EUROPE_WEST_2)
            .build());
  }

  private boolean retrieveObject(String bucket, String key, Blob blob) {
    if (null == blob) {
      log.with(key)
          .debug("Object could not be retrieved within cloud in bucket = <" + bucket + ">");
      return true;
    }
    return false;
  }
}
