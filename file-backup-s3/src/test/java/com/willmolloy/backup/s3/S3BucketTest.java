package com.willmolloy.backup.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.willmolloy.backup.FileTree;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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

  @BeforeEach
  void setUp() {
    sut = new S3Bucket(mockS3Client, "my-bucket", Path.of("my/bucket/prefix/"));
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(mockS3Client);
  }

  @Test
  void scan_returnsMapOfKeysToFiles() {
    // Given
    S3Object a = S3Object.builder().key("my/bucket/prefix/A").build();
    S3Object b = S3Object.builder().key("my/bucket/prefix/B").build();
    ListObjectsV2Response page1 = ListObjectsV2Response.builder().contents(a, b).build();
    S3Object e = S3Object.builder().key("my/bucket/prefix/C/D/E").build();
    ListObjectsV2Response page2 = ListObjectsV2Response.builder().contents(e).build();
    S3Object i = S3Object.builder().key("my/bucket/prefix/F/G/H/I").build();
    S3Object z = S3Object.builder().key("my/bucket/prefix/X/Y/Z").build();
    ListObjectsV2Response page3 = ListObjectsV2Response.builder().contents(i, z).build();

    ListObjectsV2Iterable response = mock(ListObjectsV2Iterable.class);
    when(response.iterator()).thenReturn(List.of(page1, page2, page3).iterator());

    ListObjectsV2Request request =
        ListObjectsV2Request.builder().bucket("my-bucket").prefix("my/bucket/prefix/").build();
    when(mockS3Client.listObjectsV2Paginator(request)).thenReturn(response);

    // When
    FileTree scan = sut.scan();

    // Then
    assertThat(scan)
        .isEqualTo(
            FileTree.from(
                Set.of(
                    new S3File(sut, a),
                    new S3File(sut, b),
                    new S3File(sut, e),
                    new S3File(sut, i),
                    new S3File(sut, z))));
  }

  @Test
  void toString_includesBucketUri_whichLinksToBucketInAwsConsole() {
    assertThat(sut.toString())
        .isEqualTo(
            "S3Bucket[https://s3.console.aws.amazon.com/s3/buckets/my-bucket?prefix=my/bucket/prefix/]");
  }

  @Test
  void objectUri_linksToObjectInAwsConsole() {
    assertThat(sut.objectUri(Path.of("A/B/C")))
        .isEqualTo(
            "https://s3.console.aws.amazon.com/s3/object/my-bucket?prefix=my/bucket/prefix/A/B/C");
  }

  @Test
  void folderUri_linksToFolderInAwsConsole() {
    assertThat(sut.folderUri(Path.of("A/B/C")))
        .isEqualTo(
            "https://s3.console.aws.amazon.com/s3/buckets/my-bucket?prefix=my/bucket/prefix/A/B/C/");
  }
}
