package uk.gov.ons.ctp.integration.rhsvc.cloud;


import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest(GCSDataStore.class)
public class GCSDataStoreUnitTest {
    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    @Mock
    private StorageOptions mockStorageOptions;
    @Mock
    private Storage mockStorage;
    @Mock
    private Blob mockBlob;

    GCSDataStore underTest;

    @Before
    public void setUp() throws Exception {


        mockStorage = PowerMockito.mock(Storage.class);
        MockitoAnnotations.initMocks(this);
        underTest  = new GCSDataStore();
    }
    @Test
    public void storeObject() {

    }
    @Test
    public void retrieveObject() {

/*
        BlobInfo.Builder b = Blob.newBuilder("bucket", "blobid");
        b.setBlobId(BlobId.of("bucket", "name"));
        // CollectionUtil mock = org.mockito.Mockito.mock(CollectionUtil.class);
        // PowerMockito.mockStatic(CollectionUtil.class,w_abc);
//        PowerMockito.when(CollectionUtil.createHashMap(Mockito.eq("abc:89"), Mockito.eq(":"))).thenReturn(w_abc);
        try {
            PowerMockito.when(underTest.retrieveObject("uac", "uac"), "").thenReturn(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        assertEquals("abc:89:", TestHarnes.removeHashedSettings("1", "abc:89", ":"));


*/
        BlobId b = mock(BlobId.class);
        Mockito.when(mockStorage.get(b)).thenReturn(mockBlob);
        Mockito.when(mockBlob.getContent()).thenReturn(null);
        Assert.assertEquals(underTest.retrieveObject("uac", "uac"), "");
    }
}