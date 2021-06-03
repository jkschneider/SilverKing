package com.ms.silverking.cloud.dht;

import static com.ms.silverking.cloud.dht.NamespaceVersionMode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ms.silverking.cloud.dht.client.gen.OmitGeneration;
import org.junit.jupiter.api.Test;

@OmitGeneration
public class NamespaceVersionModeTest {

  @Test
  public void testIsValidVersion() {
    NamespaceVersionMode[] testCases = { SINGLE_VERSION, CLIENT_SPECIFIED, SEQUENTIAL, SYSTEM_TIME_MILLIS,
        SYSTEM_TIME_NANOS, };

    for (NamespaceVersionMode testCase : testCases)
      assertTrue(testCase.validVersion(1_000_000_000));
  }

  @Test
  public void testIsSystemSpecified() {
    Object[][] testCases = { { SINGLE_VERSION, false }, { CLIENT_SPECIFIED, false }, { SEQUENTIAL, true },
        { SYSTEM_TIME_MILLIS, true }, { SYSTEM_TIME_NANOS, true }, };

    for (Object[] testCase : testCases) {
      NamespaceVersionMode mode = (NamespaceVersionMode) testCase[0];
      boolean expected = (boolean) testCase[1];

      assertEquals(expected, mode.isSystemSpecified());
    }
  }

}
