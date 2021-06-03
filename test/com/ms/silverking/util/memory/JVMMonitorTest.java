package com.ms.silverking.util.memory;

import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JVMMonitorTest {

  private JVMMonitor jm;

  @BeforeEach
  public void setUp() throws Exception {
    jm = new JVMMonitor(0, 0, 0, true, 0, null);
  }

  @Test
  public void testMemoryLow() {
    assertFalse(jm.memoryLow());
  }

  @Test
  public void testbytesToMB() {
    Object[][] testCases = { { -1024L, -.0009765625d }, { 0L, 0d }, { 1024L, .0009765625d }, { 524_288L, .5d },
        { 1_048_576L, 1d }, };

    for (Object[] testCase : testCases) {
      long bytes = (long) testCase[0];
      double expectedMb = (double) testCase[1];

      checkBytesToMb(bytes, expectedMb);
    }
  }

  private void checkBytesToMb(long bytes, double expectedMb) {
    assertEquals(expectedMb, jm.bytesToMB(bytes), 0, getTestMessage("bytesToMB", bytes));
  }
}
