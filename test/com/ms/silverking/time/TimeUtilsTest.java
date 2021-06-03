package com.ms.silverking.time;

import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TimeUtilsTest {

  @Test
  public void testInMillis() {
    int[][] testCases = { { -348, -1_252_800_000 }, { -10, -36_000_000 }, { -5, -18_000_000 }, { -1, -3_600_000 },
        { 0, 0 }, { 1, 3_600_000 }, { 5, 18_000_000 }, { 10, 36_000_000 }, { 348, 1_252_800_000 }, };

    for (int[] testCase : testCases) {
      int hours = testCase[0];
      int expectedMillis = testCase[1];

      int minutes = hours * 60;
      int seconds = minutes * 60;

      checkHoursInMillis(hours, expectedMillis);
      checkMinutesInMillis(minutes, expectedMillis);
      checkSecondsInMillis(seconds, expectedMillis);
    }
  }

  private void checkHoursInMillis(int hours, int expected) {
    assertEquals(expected, TimeUtils.hoursInMillis(hours), getTestMessage("hoursInMillis", hours));
  }

  private void checkMinutesInMillis(int minutes, int expected) {
    assertEquals(expected, TimeUtils.minutesInMillis(minutes), getTestMessage("minutesInMillis", minutes));
  }

  private void checkSecondsInMillis(int seconds, int expected) {
    assertEquals(expected, TimeUtils.secondsInMillis(seconds), getTestMessage("secondInMillis", seconds));
  }

}
