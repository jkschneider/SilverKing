package com.ms.silverking.cloud.ring;

import static com.ms.silverking.testing.Assert.exceptionNameChecker;
import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ms.silverking.testing.Util.ExceptionChecker;
import org.junit.jupiter.api.Test;

public class LongRingspaceTest {

  static final long start = -4611686018427385904L;
  static final long end = 4611686018427385903L;
  static final long size = end - start + 1;
  private static final long halfRingspaceErrorRoom = 2000;

  @Test
  public void testLongToRingspace() {
    long[][] testCases = { { Long.MIN_VALUE, start }, { start, start >> 1 }, { -1, -1 }, { 0, 0 }, { 1, 0 }, { 2, 1 },
        { 3, 1 }, { 4, 2 }, { end, end >> 1 }, { size, size >> 1 },
        { Long.MAX_VALUE, (end + halfRingspaceErrorRoom) }, };

    for (long[] testCase : testCases) {
      long input = testCase[0];
      long expected = testCase[1];

      assertEquals(expected, LongRingspace.longToRingspace(input), getTestMessage("longToRingspace", input));
    }
  }

  @Test
  public void testLongToFraction() {
    Object[][] testCases = { { Long.MIN_VALUE, "-1.000000000000000433680868994201962" }, { start, "-0.5" },
        { -1L, "-1.084202172485504904205193129785149E-19" }, { 0L, "0" },
        { 1L, "1.084202172485504904205193129785149E-19" }, { end, "0.4999999999999999998915797827514495" },
        { size, "1" }, { Long.MAX_VALUE, "1.000000000000000433572448776953411" }, };

    for (Object[] testCase : testCases) {
      long l = (long) testCase[0];
      String expected = (String) testCase[1];

      assertEquals(expected, LongRingspace.longToFraction(l).toString(), getTestMessage("longToFraction", l));
    }
  }

  @Test
  public void testFractionToLong() {
    Object[][] testCases = {
        //            {-1d, }, can't test b/c of assert
        { 0d, 0L }, { Double.MIN_VALUE, 0L }, { .5, end + 1 }, { 1d, size }
        //            {Double.MAX_VALUE, }, can't test b/c of assert
    };

    for (Object[] testCase : testCases) {
      double f = (double) testCase[0];
      long expected = (long) testCase[1];

      assertEquals(expected, LongRingspace.fractionToLong(f), getTestMessage("fractionToLong", f));
    }
  }

  @Test
  public void testAddRingspace_Exceptions() {
    long[][] testCases = { { Long.MIN_VALUE, 1 }, { -1, 0 }, { 0, -1 }, };

    for (long[] testCase : testCases) {
      long a = testCase[0];
      long b = testCase[1];
      String testMessage = getTestMessage("addRingspace_Exceptions", a, b);
      ExceptionChecker ec = new ExceptionChecker() {
        @Override
        public void check() { LongRingspace.addRingspace(a, b); }
      };
      exceptionNameChecker(ec, testMessage, IllegalArgumentException.class);
    }
  }

  @Test
  public void testAddRingspace() {
    long[][] testCases = { { 0, 0, 0 }, { 1, 0, 1 }, { 0, 1, 1 }, { 1, 1, 2 }, { 1000, 10000, 11000 },
        { size - 1, 0, size - 1 }, { size, 0, size }, { Long.MAX_VALUE, 2, size },
        { Long.MAX_VALUE, Long.MAX_VALUE, size }, };

    for (long[] testCase : testCases) {
      long a = testCase[0];
      long b = testCase[1];
      long expected = testCase[2];

      assertEquals(expected, LongRingspace.addRingspace(a, b), getTestMessage("addRingspace", a, b));
    }
  }

  @Test
  public void testAdd_Exceptions() {
    assertThrows(IllegalArgumentException.class, () -> {
      LongRingspace.add(start, -1);
    });
  }

  @Test
  public void testSubtract_Exceptions() {
    assertThrows(IllegalArgumentException.class, () -> {
      LongRingspace.subtract(start - 1, -1);
    });
  }

  @Test
  public void testAddAndSubtract() {
    long[][] testCases = { { start, end, -1 }, { 0, 0, 0 }, { 0, 1, 1 }, { end, 0, end }, { end, 1, start }, };

    for (long[] testCase : testCases) {
      long p = testCase[0];
      long a = testCase[1];
      long expected = testCase[2];

      long actualAdd = LongRingspace.add(p, a);
      assertEquals(expected, actualAdd, getTestMessage("add", p, a));

      long actualSubtract = LongRingspace.subtract(actualAdd, a);
      assertEquals(p, actualSubtract, getTestMessage("subtract", actualAdd, a));
    }
  }

  @Test
  public void testClockwiseDistance() {
    long[][] testCases = { { start, end, size - 1 }, { 0, 0, 0 }, { 0, 1, 1 }, { end, 0, -start + 1 },
        { end, start, 1 }, };

    for (long[] testCase : testCases) {
      long p0 = testCase[0];
      long p1 = testCase[1];
      long expected = testCase[2];

      assertEquals(expected, LongRingspace.clockwiseDistance(p0, p1), getTestMessage("clockwiseDistance", p0, p1));
    }
  }

  @Test
  public void testNextPoint() {
    long[][] testCases = { { start, start + 1 }, { 0, 1 }, { end - 1, end }, { end, start }
        //            {Long.MIN_VALUE,  Long.MIN_VALUE+1}, // can't test b/c of assert
    };

    for (long[] testCase : testCases) {
      long p = testCase[0];
      long expected = testCase[1];

      assertEquals(expected, LongRingspace.nextPoint(p), getTestMessage("nextPoint", p));
    }
  }

  @Test
  public void testPrevPoint() {
    long[][] testCases = { { end, end - 1 }, { 0, -1 }, { start + 1, start }, { start, end }, };

    for (long[] testCase : testCases) {
      long p = testCase[0];
      long expected = testCase[1];

      assertEquals(expected, LongRingspace.prevPoint(p), getTestMessage("prevPoint", p));
    }
  }

  @Test
  public void testInRingspace() {
    Object[][] testCases = { { Long.MIN_VALUE, false }, { start, true }, { -1L, true }, { 0L, true }, { 1L, true },
        { end, true }, { Long.MAX_VALUE, false }, };

    for (Object[] testCase : testCases) {
      long p = (long) testCase[0];
      boolean expected = (boolean) testCase[1];

      assertEquals(expected, LongRingspace.inRingspace(p), getTestMessage("inRingspace", p));
    }
  }

  @Test
  public void testMapRegionPointToRingspace() {
    Object[][] testCases = {
        //            {RingRegion.allRingspace, start-1, start-1}, // start-1, end?
        //            {RingRegion.allRingspace, start,   start},
        //            {RingRegion.allRingspace, 0L,      0L},
        //            {RingRegion.allRingspace, end,     end},
        //            {RingRegion.allRingspace, end+1,   end+1},    // end+1, start?
        // add a smaller region like 1-1000, and
    };

    for (Object[] testCase : testCases) {
      RingRegion region = (RingRegion) testCase[0];
      long p = (long) testCase[1];
      long expected = (long) testCase[2];

      assertEquals(expected,
          LongRingspace.mapRegionPointToRingspace(region, p), getTestMessage("mapRegionPointToRingspace", p));
    }
  }

}
