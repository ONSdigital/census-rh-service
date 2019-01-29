package uk.gov.ons.ctp.integration.rhsvc.cloud;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.cloud.storage.*;
import org.springframework.stereotype.Service;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class CloudStorageGCSService implements CloudStorage {
    private static final Logger log = LoggerFactory.getLogger(CloudStorageGCSService.class);

    private static final String UAC_DETAILS_BUCKET_NAME = "uac_details_bucket";
    private static final String CASE_DETAILS_BUCKET_NAME = "case_details_bucket";

    /**
     * Create object in Cloud Storage for UAC details inside specified bucket
     * @param uac - represents the name of the object to be stored
     * @param data - represents the data to be stored in the object
     */
    @Override
    public void createUacDetails(final String uac, final String data) {
        create(UAC_DETAILS_BUCKET_NAME, uac, data);
    }

    /**
     * Create object in Cloud Storage for case details inside specified bucket
     * @param caseID - represents the name of the object to be stored
     * @param data - represents the data to be stored in the object
     */
    @Override
    public void createCaseDetails(final String caseID, final String data) {
        create(CASE_DETAILS_BUCKET_NAME, caseID, data);
    }

    /**
     * Delete object from Cloud storage inside specified bucket
     * @param uac - id of the object to be deleted
     */
    @Override
    public void deleteUacDetails(final String uac) {
        delete(UAC_DETAILS_BUCKET_NAME, uac);
    }

    /**
     * Delete object from Cloud storage inside specified bucket
     * @param caseID - id of the object to be deleted
     */
    @Override
    public void deleteCaseDetails(final String caseID) {
        delete(CASE_DETAILS_BUCKET_NAME, caseID);
    }


    private void create(final String bucketName, final String blobName, final String data) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        try {
            storage.create(BucketInfo.of(bucketName));
        } catch (StorageException se) {
            log.info("Bucket with name = " + bucketName + " exists in the cloud storage.");
        }
        storage.create(
                BlobInfo.newBuilder(BlobId.of(bucketName, blobName))
                        .setContentType("text/plain")
                        .build(), data.getBytes(UTF_8)
        );
        log.info("Blob with name = " + blobName + " has been created in cloud storage");

    }

    private void delete(final String bucketName, final String blobName) {
        Storage storage = StorageOptions.getDefaultInstance().getService();
        BlobId blobId = BlobId.of(bucketName, blobName);
        boolean deleted = storage.delete(blobId);
        if (deleted) {
            log.info("Blob with id = " + blobName + " has been removed from storage");
        } else {
            log.info("Blob with id = " + blobName + " could not be removed from storage");
        }
    }
}
