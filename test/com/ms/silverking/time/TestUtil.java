package com.ms.silverking.time;

import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtil {

  public static final long absMillisTime = 0;
  public static final long absNanosTime = 0;

  public static final ConstantAbsMillisTimeSource constantSource = new ConstantAbsMillisTimeSource(absMillisTime);
  public static final SystemTimeSource systemSource = SystemTimeSource.createWithMillisOrigin(absNanosTime);
  public static final TimerDrivenTimeSource timerSource = new TimerDrivenTimeSource();

  public static void test_AbsTimeMillis(Object[][] testCases) {
    for (Object[] testCase : testCases) {
      long expected = (long) testCase[0];
      AbsMillisTimeSource source = (AbsMillisTimeSource) testCase[1];

      assertEquals(expected, source.absTimeMillis(), getTestMessage("absTimeMillis", expected, source));
    }
  }

  public static void test_RelMillisRemaining(Object[][] testCases) {
    for (Object[] testCase : testCases) {
      long expected = (long) testCase[0];
      AbsMillisTimeSource source = (AbsMillisTimeSource) testCase[1];
      long deadline = (long) testCase[2];

      assertEquals(expected,
          source.relMillisRemaining(deadline), getTestMessage("relMillisRemaining", expected, source, deadline));
    }
  }

  public static void test_AbsTimeNanos(Object[][] testCases) {
    for (Object[] testCase : testCases) {
      long expected = (long) testCase[0];
      AbsNanosTimeSource source = (AbsNanosTimeSource) testCase[1];

      assertEquals(expected, source.absTimeNanos(), getTestMessage("absTimeNanos", expected, source));
    }
  }

  public static void test_GetNanosOriginTime(Object[][] testCases) {
    for (Object[] testCase : testCases) {
      long expected = (long) testCase[0];
      AbsNanosTimeSource source = (AbsNanosTimeSource) testCase[1];

      assertEquals(expected, source.getNanosOriginTime(), getTestMessage("getNanosOriginTime", expected, source));
    }
  }
}
