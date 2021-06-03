package com.ms.silverking.time;

import static com.ms.silverking.testing.AssertFunction.checkHashCodeEquals;
import static com.ms.silverking.testing.AssertFunction.checkHashCodeNotEquals;
import static com.ms.silverking.testing.AssertFunction.test_FirstEqualsSecond_FirstNotEqualsThird;
import static com.ms.silverking.testing.AssertFunction.test_NotEquals;
import static com.ms.silverking.time.TestUtil.absMillisTime;
import static com.ms.silverking.time.TestUtil.constantSource;
import static com.ms.silverking.time.TestUtil.test_AbsTimeMillis;
import static com.ms.silverking.time.TestUtil.test_RelMillisRemaining;

import org.junit.jupiter.api.Test;

public class ConstantAbsMillisTimeSourceTest {

  private static final long absMillisTimeCopy = 0;
  private static final long absMillisTimeDiff = 1;

  private static final ConstantAbsMillisTimeSource constantSourceCopy = new ConstantAbsMillisTimeSource(
      absMillisTimeCopy);
  private static final ConstantAbsMillisTimeSource constantSourceDiff = new ConstantAbsMillisTimeSource(
      absMillisTimeDiff);

  @Test
  public void testAbsTimeMillis() {
    Object[][] testCases = { { absMillisTime, constantSource }, { absMillisTimeCopy, constantSourceCopy },
        { absMillisTimeDiff, constantSourceDiff }, };

    test_AbsTimeMillis(testCases);
  }

  @Test
  public void testRelMillisRemaining() {
    Object[][] testCases = { { 1L, constantSource, 1L }, { -1L, constantSourceCopy, -1L },
        { 0L, constantSourceDiff, 1L }, };

    test_RelMillisRemaining(testCases);
  }

  @Test
  public void testHashCode() {
    checkHashCodeEquals(constantSource, constantSource);
    checkHashCodeEquals(constantSource, constantSourceCopy);
    checkHashCodeNotEquals(constantSource, constantSourceDiff);
  }

  @Test
  public void testEqualsObject() {
    ConstantAbsMillisTimeSource[][] testCases = { { constantSource, constantSource, constantSourceDiff },
        { constantSourceCopy, constantSource, constantSourceDiff },
        { constantSourceDiff, constantSourceDiff, constantSource }, };
    test_FirstEqualsSecond_FirstNotEqualsThird(testCases);

    test_NotEquals(
        new Object[][] { { constantSource, TestUtil.systemSource }, { constantSource, TestUtil.timerSource }, });
  }
}
