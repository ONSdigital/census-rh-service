package uk.gov.ons.ctp.integration.rhsvc.cloud;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*"})
@PrepareForTest(BlobId.class)
public class GCSDataStoreUnitTest {
  private static final String BUCKET = "bucket";
  private static final String KEY = "key";
  @Mock private Storage mockStorage;

  private GCSDataStore underTest;

  @Before
  public void setUp() {

    mockStorage = PowerMockito.mock(Storage.class);
    MockitoAnnotations.initMocks(this);
    underTest = new GCSDataStore();
  }

  @Test
  @PrepareForTest(BlobInfo.class)
  @Ignore
  public void storeObject() {
    String value = "blob content";

    BlobId blobIdMock = PowerMockito.mock(BlobId.class);
    BlobInfo blobInfoMock = PowerMockito.mock(BlobInfo.class);
    Blob blobMock = PowerMockito.mock(Blob.class);
    BlobInfo.Builder bibMock = PowerMockito.mock(BlobInfo.Builder.class);
    mockStatic(BlobInfo.class);

    PowerMockito.when(BlobInfo.newBuilder(blobIdMock)).thenReturn(bibMock);
    PowerMockito.when(mockStorage.create(blobInfoMock)).thenReturn(blobMock);

    PowerMockito.when(BlobInfo.newBuilder(blobIdMock)).thenReturn(bibMock);
    PowerMockito.when(BlobInfo.newBuilder(blobIdMock).build()).thenReturn(blobInfoMock);
    PowerMockito.when(mockStorage.create(blobInfoMock, value.getBytes())).thenReturn(blobMock);

    underTest.storeObject(BUCKET, KEY, value);
  }

  @Test
  @Ignore
  public void retrieveObject() {
    String value = "blob content";

    BlobId fcMock = PowerMockito.mock(BlobId.class);
    BlobInfo blobInfoMock = PowerMockito.mock(BlobInfo.class);
    Blob blobMock = PowerMockito.mock(Blob.class);
    BlobInfo.Builder bibMock = PowerMockito.mock(BlobInfo.Builder.class);
    PowerMockito.when(BlobInfo.newBuilder(fcMock)).thenReturn(bibMock);
    PowerMockito.when(BlobInfo.newBuilder(fcMock).build()).thenReturn(blobInfoMock);
    PowerMockito.when(mockStorage.create(blobInfoMock, value.getBytes())).thenReturn(blobMock);
  }
}
