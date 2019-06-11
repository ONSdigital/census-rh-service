package uk.gov.ons.ctp.integration.rhsvc.cloud;

import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*"})
//PMB@PrepareForTest(BlobId.class)
public class GCSDataStoreUnitTest {
//PMB  private static final String BUCKET = "bucket";
//  private static final String KEY = "key";
//  @Mock private Storage mockStorage;
//
//  private GCSDataStore underTest;
//
//  @Before
//  public void setUp() {
//
//    mockStorage = PowerMockito.mock(Storage.class);
//    MockitoAnnotations.initMocks(this);
//    underTest = new GCSDataStore();
//  }
//
//  @Test
//  @PrepareForTest(BlobInfo.class)
//  @Ignore
//  public void storeObject() {
//    String value = "blob content";
//
//    BlobId blobIdMock = PowerMockito.mock(BlobId.class);
//    BlobInfo blobInfoMock = PowerMockito.mock(BlobInfo.class);
//    Blob blobMock = PowerMockito.mock(Blob.class);
//    BlobInfo.Builder bibMock = PowerMockito.mock(BlobInfo.Builder.class);
//    mockStatic(BlobInfo.class);
//
//    PowerMockito.when(BlobInfo.newBuilder(blobIdMock)).thenReturn(bibMock);
//    PowerMockito.when(mockStorage.create(blobInfoMock)).thenReturn(blobMock);
//
//    PowerMockito.when(BlobInfo.newBuilder(blobIdMock)).thenReturn(bibMock);
//    PowerMockito.when(BlobInfo.newBuilder(blobIdMock).build()).thenReturn(blobInfoMock);
//    PowerMockito.when(mockStorage.create(blobInfoMock, value.getBytes())).thenReturn(blobMock);
//
//    underTest.storeObject(BUCKET, KEY, value);
//  }
//
//  @Test
//  @Ignore
//  public void retrieveObject() {
//    String value = "blob content";
//
//    BlobId fcMock = PowerMockito.mock(BlobId.class);
//    BlobInfo blobInfoMock = PowerMockito.mock(BlobInfo.class);
//    Blob blobMock = PowerMockito.mock(Blob.class);
//    BlobInfo.Builder bibMock = PowerMockito.mock(BlobInfo.Builder.class);
//    PowerMockito.when(BlobInfo.newBuilder(fcMock)).thenReturn(bibMock);
//    PowerMockito.when(BlobInfo.newBuilder(fcMock).build()).thenReturn(blobInfoMock);
//    PowerMockito.when(mockStorage.create(blobInfoMock, value.getBytes())).thenReturn(blobMock);
//  }
}
