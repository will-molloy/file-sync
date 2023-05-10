package com.willmolloy.backup;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

/**
 * StatisticsTest.
 *
 * @author <a href=https://willmolloy.com>Will Molloy</a>
 */
class StatisticsTest {

  @Test
  void statistics_toString_formatsNumbers() {
    BackupRunner.Statistics statistics =
        new BackupRunner.Statistics(1000, 2000, 3000, 40000, 10_000_000, 200_000_000);
    assertThat(statistics.toString())
        .isEqualTo(
            "1,000 files created, 2,000 files updated, 3,000 files deleted, 40,000 files same. 10MB added, 200MB removed");
  }

  @Test
  void errorStatistics_toString_formatsNumbers() {
    BackupRunner.ErrorStatistics statistics = new BackupRunner.ErrorStatistics(1000, 2000, 3000);
    assertThat(statistics.toString())
        .isEqualTo("1,000 failed creates, 2,000 failed updates, 3,000 failed deletes");
  }
}
