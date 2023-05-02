package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.Backup.File;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * S3BucketTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
@SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
@ExtendWith(MockitoExtension.class)
class S3BucketTest {

  @Mock private S3Client mockS3Client;
  private S3Bucket sut;
  private static final String BUCKET_NAME = "root";

  @BeforeEach
  void setUp() {
    sut = new S3Bucket(mockS3Client, BUCKET_NAME);
  }

  @Test
  void scan_returnsMapOfKeysToFiles() {
    // Given
    S3Object a = S3Object.builder().key("A").build();
    S3Object b = S3Object.builder().key("B").build();
    ListObjectsV2Response page1 = ListObjectsV2Response.builder().contents(a, b).build();
    S3Object e = S3Object.builder().key("C/D/E").build();
    ListObjectsV2Response page2 = ListObjectsV2Response.builder().contents(e).build();
    S3Object i = S3Object.builder().key("F/G/H/I").build();
    S3Object z = S3Object.builder().key("X/Y/Z").build();
    ListObjectsV2Response page3 = ListObjectsV2Response.builder().contents(i, z).build();

    // TODO how to create a real instance?
    ListObjectsV2Iterable response = mock(ListObjectsV2Iterable.class);
    when(response.stream()).thenReturn(Stream.of(page1, page2, page3));

    ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(BUCKET_NAME).build();
    when(mockS3Client.listObjectsV2Paginator(request)).thenReturn(response);

    // When
    Map<String, File> scan = sut.scan();

    // Then
    assertThat(scan)
        .containsExactly(
            "A", new S3File(a),
            "B", new S3File(b),
            "C/D/E", new S3File(e),
            "F/G/H/I", new S3File(i),
            "X/Y/Z", new S3File(z));
  }
}
