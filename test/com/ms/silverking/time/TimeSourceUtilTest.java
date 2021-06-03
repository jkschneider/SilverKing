package com.ms.silverking.time;

import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TimeSourceUtilTest {

  @Test
  public void testRelTimeRemainingAsInt_Overflow() {
    assertThrows(IllegalArgumentException.class, () -> {
      TimeSourceUtil.relTimeRemainingAsInt(4_000_000_000L, 0L);
    });
  }

  @Test
  public void testRelTimeRemainingAsInt() {
    Object[][] testCases = { { 0L, 0L, 0 }, { 1L, 0L, 1 }, { 1_000_000_000L, 0L, 1_000_000_000 }, };

    for (Object[] testCase : testCases) {
      long deadline = (long) testCase[0];
      long currentTime = (long) testCase[1];
      int expectedRemaining = (int) testCase[2];

      checkRelTimeRemainingAsInt(deadline, currentTime, expectedRemaining);
    }
  }

  private void checkRelTimeRemainingAsInt(long deadline, long currentTime, int expectedRemaining) {
    assertEquals(expectedRemaining,
        TimeSourceUtil.relTimeRemainingAsInt(deadline, currentTime), getTestMessage("relTimeRemainingAsInt", deadline, currentTime));
  }

}
