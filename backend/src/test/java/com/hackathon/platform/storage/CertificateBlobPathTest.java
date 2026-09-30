package com.hackathon.platform.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CertificateBlobPathTest {

  @Test
  void certificateAsset_buildsKey() {
    assertThat(BlobPath.certificateAsset("t1", "logo.png"))
        .isEqualTo("certificates/templates/t1/assets/logo.png");
  }

  @Test
  void certificateAsset_sanitisesFilename() {
    assertThat(BlobPath.certificateAsset("t1", " ../a/b\\c.png "))
        .isEqualTo("certificates/templates/t1/assets/__a_b_c.png");
  }

  @Test
  void certificateBackground_buildsKey() {
    assertThat(BlobPath.certificateBackground("t1", "bg.png"))
        .isEqualTo("certificates/templates/t1/background/bg.png");
  }

  @Test
  void certificateBackground_sanitisesFilename() {
    assertThat(BlobPath.certificateBackground("t1", "../bg.png"))
        .isEqualTo("certificates/templates/t1/background/__bg.png");
  }

  @Test
  void certificatePdf_buildsKey() {
    assertThat(BlobPath.certificatePdf("e1", "r1", "c1"))
        .isEqualTo("certificates/events/e1/runs/r1/c1.pdf");
  }
}
