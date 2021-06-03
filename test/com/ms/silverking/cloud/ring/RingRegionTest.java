package com.ms.silverking.cloud.ring;

import static com.ms.silverking.testing.Assert.exceptionNameChecker;
import static com.ms.silverking.testing.AssertFunction.*;
import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ms.silverking.testing.Util.ExceptionChecker;
import org.junit.jupiter.api.Test;

public class RingRegionTest {

  private static final RingRegion zeroZero = new RingRegion(0, 0);
  private static final RingRegion zeroZeroCopy = new RingRegion(0, 0);
  private static final RingRegion zeroOne = new RingRegion(0, 1);
  private static final RingRegion oneOne = new RingRegion(1, 1);
  private static final RingRegion twoTwo = new RingRegion(2, 2);
  private static final RingRegion startToEnd = new RingRegion(LongRingspaceTest.start, LongRingspaceTest.end);
  private static final RingRegion endToStart = new RingRegion(LongRingspaceTest.end, LongRingspaceTest.start);
  private static final RingRegion startStart = new RingRegion(LongRingspaceTest.start, LongRingspaceTest.start);
  private static final RingRegion endEnd = new RingRegion(LongRingspaceTest.end, LongRingspaceTest.end);
  private static final RingRegion startToEndShrunk = new RingRegion(LongRingspaceTest.start + 1,
      LongRingspaceTest.end - 1);
  private static final RingRegion endToStartExpanded = new RingRegion(LongRingspaceTest.end - 1,
      LongRingspaceTest.start + 1);
  private static final RingRegion startToEndRotated = new RingRegion(2, 1);

  @Test
  public void testStart_and_End() {
    Object[][] testCases = { { zeroZero, 0L, 0L }, { zeroOne, 0L, 1L },
        { startToEnd, LongRingspaceTest.start, LongRingspaceTest.end },
        { startToEndRotated, LongRingspaceTest.start, LongRingspaceTest.end },
        { endToStart, LongRingspaceTest.end, LongRingspaceTest.start }, };

    for (Object[] testCase : testCases) {
      RingRegion region = (RingRegion) testCase[0];
      long expectedStart = (long) testCase[1];
      long expectedEnd = (long) testCase[2];

      assertEquals(expectedStart, region.getStart(), getTestMessage("testStart", region));
      assertEquals(expectedEnd, region.getEnd(), getTestMessage("testEnd", region));
    }
  }

  @Test
  public void testGetSize() {
    Object[][] testCases = { { zeroZero, 1L }, { zeroOne, 2L }, { startToEnd, LongRingspaceTest.size },
        { endToStart, 2L }, { startToEndRotated, LongRingspaceTest.size }, };

    for (Object[] testCase : testCases) {
      RingRegion region = (RingRegion) testCase[0];
      long expected = (long) testCase[1];

      assertEquals(expected, region.getSize(), getTestMessage("getSize", region));
    }
  }

  @Test
  public void testBefore_and_After() {
    Object[][] testCases = { { zeroZero, 0L, 0L, false, false }, { zeroZero, 0L, 1L, true, false },
        { zeroZero, 1L, 1L, false, false }, { zeroOne, 0L, 0L, false, false }, { zeroOne, 0L, 1L, true, false },
        { zeroOne, 1L, 1L, false, false }, { zeroOne, 2L, 2L, false, false }, { zeroOne, -1L, -1L, false, false },
        { zeroOne, -1L, 0L, false, true },    // ??
        { zeroOne, 0L, -1L, true, false },    // ??
        { endToStart, LongRingspaceTest.end, LongRingspaceTest.start, true, false },
        { endToStartExpanded, LongRingspaceTest.start, LongRingspaceTest.end, false, true }, };

    for (Object[] testCase : testCases) {
      RingRegion region = (RingRegion) testCase[0];
      long p0 = (long) testCase[1];
      long p1 = (long) testCase[2];
      boolean expectedBefore = (boolean) testCase[3];
      boolean expectedAfter = (boolean) testCase[4];

      assertEquals(expectedBefore, region.before(p0, p1), getTestMessage("testBefore", region, p0, p1));
      assertEquals(expectedAfter, region.after(p0, p1), getTestMessage("testAfter", region, p0, p1));
    }
  }

  @Test
  public void testGetSizeBD() {
    assertEquals(LongRingspace.size + "", startToEnd.getSizeBD().toString());
  }

  @Test
  public void testGetRingspaceFraction() {
    Object[][] testCases = { { zeroZero, 1.0d / LongRingspaceTest.size }, { zeroOne, 2.0d / LongRingspaceTest.size },
        { startToEnd, 1.0d }, { endToStart, 2.0d / LongRingspaceTest.size }, };

    for (Object[] testCase : testCases) {
      RingRegion region = (RingRegion) testCase[0];
      double expected = (double) testCase[1];

      int delta = 0;
      assertEquals(expected, region.getRingspaceFraction(), delta, getTestMessage("getRingspaceFraction", region));
    }
  }

  // same tests as testEquals() - test_Equals
  @Test
  public void testEnsureIdentical() {
    RingRegion[][] testCases = { { zeroZero, zeroZero }, { zeroZero, zeroZeroCopy }, };

    for (Object[] testCase : testCases) {
      RingRegion r0 = (RingRegion) testCase[0];
      RingRegion r1 = (RingRegion) testCase[1];

      RingRegion.ensureIdentical(r0, r1);
    }
  }

  // same tests as testEquals - test_NotEquals
  @Test
  public void testEnsureIdentical_Exceptions() {
    RingRegion[][] testCases = { { zeroZero, zeroOne }, };

    for (RingRegion[] testCase : testCases) {
      String testMessage = getTestMessage("ensureIdentical_Exceptions", testCase);
      ExceptionChecker ec = new ExceptionChecker() {
        @Override
        public void check() { RingRegion.ensureIdentical(testCase[0], testCase[1]); }
      };
      exceptionNameChecker(ec, testMessage, RuntimeException.class);
    }
  }

  @Test
  public void testEquals() {
    test_Equals(new Object[][] { { zeroZero, zeroZero }, { zeroZero, zeroZeroCopy }, });

    test_NotEquals(new Object[][] { { zeroZero, zeroOne }, });
  }

  @Test
  public void testHashCode() {
    checkHashCodeEquals(zeroZero, zeroZero);
    checkHashCodeEquals(zeroZero, zeroZeroCopy);
    checkHashCodeNotEquals(zeroZero, zeroOne);
  }

  @Test
  public void testParseZKString_and_toZKString_and_toString() {
    Object[][] testCases = { { "0:0", zeroZero, 1L }, { "0:1", zeroOne, 2L }, { "1:1", oneOne, 1L },
        { LongRingspaceTest.start + ":" + LongRingspaceTest.end, startToEnd, LongRingspaceTest.size },
        { LongRingspaceTest.end + ":" + LongRingspaceTest.start, endToStart, 2L }, };

    for (Object[] testCase : testCases) {
      String s = (String) testCase[0];
      RingRegion expected = (RingRegion) testCase[1];
      long size = (long) testCase[2];

      assertEquals(expected, RingRegion.parseZKString(s), getTestMessage("parseZkString", s));
      assertEquals(s, expected.toZKString(), getTestMessage("toZKString", expected));
      assertEquals("[" + s + ":" + size + "]", expected.toString(), getTestMessage("toString", expected));
    }
  }

  @Test
  public void testIsContiguousWith() {
    Object[][] testCases = { { zeroZero, zeroZero, false }, { zeroZero, zeroOne, false }, { zeroOne, oneOne, false },
        { zeroZero, oneOne, true }, { oneOne, zeroZero, true }, { startToEnd, zeroZero, false },
        { startToEnd, startToEnd, true }, { startToEnd, startStart, true }, { startToEnd, endEnd, true },
        { startToEnd, endToStart, false }, { startToEnd, startToEndShrunk, false },
        { startToEndShrunk, startToEnd, false }, { startToEndShrunk, endToStart, true },
        { endToStart, startToEndShrunk, true }, };

    for (Object[] testCase : testCases) {
      RingRegion r1 = (RingRegion) testCase[0];
      RingRegion r2 = (RingRegion) testCase[1];
      boolean expected = (boolean) testCase[2];

      assertEquals(expected, r1.isContiguousWith(r2), getTestMessage("isContiguousWith", r1, r2));
    }
  }

  @Test
  public void testContains() {
    Object[][] testCases = { { zeroZero, -1L, false }, { zeroZero, 0L, true }, { zeroZero, 1L, false },
        { zeroOne, -1L, false }, { zeroOne, 0L, true }, { zeroOne, 1L, true }, { zeroOne, 2L, false },
        { startToEnd, LongRingspaceTest.start - 1, false }, { startToEnd, LongRingspaceTest.start, true },
        { startToEnd, LongRingspaceTest.start + 1, true }, { startToEnd, LongRingspaceTest.end - 1, true },
        { startToEnd, LongRingspaceTest.end, true }, { startToEnd, LongRingspaceTest.end + 1, false },
        { endToStart, LongRingspaceTest.end - 1, false }, { endToStart, LongRingspaceTest.end, true },
        { endToStart, LongRingspaceTest.end + 1, true },// FIXME:bph: should be false
        { endToStart, LongRingspaceTest.start - 1, true },// FIXME:bph: should be false
        { endToStart, LongRingspaceTest.start, true }, { endToStart, LongRingspaceTest.start + 1, false }, };

    for (Object[] testCase : testCases) {
      RingRegion region1 = (RingRegion) testCase[0];
      long point = (long) testCase[1];
      boolean expected = (boolean) testCase[2];

      assertEquals(expected, region1.contains(point), getTestMessage("contains", region1, point));
    }
  }

  @Test
  public void testOverlaps() {
    Object[][] testCases = { { zeroZero, zeroZero, true }, { zeroZero, zeroOne, true }, { zeroOne, zeroZero, true },
        { zeroOne, oneOne, true }, { zeroOne, twoTwo, false }, { endToStart, zeroZero, false },
        { endToStart, startToEnd, true }, { endToStart, endToStartExpanded, true },
        { endToStartExpanded, endToStart, true }, };

    for (Object[] testCase : testCases) {
      RingRegion region1 = (RingRegion) testCase[0];
      RingRegion region2 = (RingRegion) testCase[1];
      boolean expected = (boolean) testCase[2];

      assertEquals(expected, region1.overlaps(region2), getTestMessage("overlaps", region1, region2));
    }
  }

}
