package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.storage.*;
import org.springframework.stereotype.Service;

@Service
public class GCSService implements CloudService {
    private static final Logger log = LoggerFactory.getLogger(GCSService.class);
    public static final String EUROPE_WEST_2 = "europe-west2";

    /**
     * Write object in Cloud Storage for UAC details inside specified bucket
     *
     * @param bucket - represents the bucket where the object will be stored
     * @param key    - represents the unique object identifier in the bucket for the object stored
     * @param value  - represents the string value representation of the object to be stored
     */

    @Override
    public void storeObject(final String bucket, final String key, final String value) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        try {
            storage.create(BucketInfo.newBuilder(bucket)
                    // This is the cheapes option
                    // See here for possible values: http://g.co/cloud/storage/docs/storage-classes
                    .setStorageClass(StorageClass.COLDLINE)
                    // As John mentioned, I used Europe west 2 - location where data will be held
                    // Possible values: http://g.co/cloud/storage/docs/bucket-locations#location-mr
                    .setLocation(EUROPE_WEST_2)
                    .build());


        } catch (StorageException se) {
            //This Storage Exception is the only one declared on this API.
            // If a bucket exists, this exception will be thrown
            log.info("Bucket with name = " + bucket + " exists in the cloud storage.");
        }
        storage.create(
                BlobInfo.newBuilder(BlobId.of(bucket, key))
                        .setContentType("text/plain")
                        .build(), value.getBytes()
        );
        log.info("Blob with name = " + key + " has been created in cloud storage");
    }

    /**
     * Read object in Cloud Storage for Case details inside specified bucket
     *
     * @param bucket - represents the bucket where the object will be stored
     * @param key    - represents the unique object identifier in the bucket for the object stored
     * @return
     */
    @Override
    public String retrieveObject(final String bucket, final String key) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobId blobId = BlobId.of(bucket, key);
        Blob blob = storage.get(blobId);
        String value = new String(blob.getContent());
        if (blob != null) {
            log.info("Found BLOB:\n blobId = " + blobId + "\ncontent= " + value);
        } else {
            log.info("Blob not found");
        }
        return value;
    }
}
