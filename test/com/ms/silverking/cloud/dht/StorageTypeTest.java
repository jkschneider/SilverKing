package com.ms.silverking.cloud.dht;

import static com.ms.silverking.cloud.dht.StorageType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ms.silverking.cloud.dht.client.gen.OmitGeneration;
import org.junit.jupiter.api.Test;

@OmitGeneration
public class StorageTypeTest {

  @Test
  public void testIsFileBased() {
    Object[][] testCases = { { RAM, false }, { FILE, true }, { FILE_SYNC, true }, };

    for (Object[] testCase : testCases) {
      StorageType type = (StorageType) testCase[0];
      boolean expected = (boolean) testCase[1];

      assertEquals(expected, type.isFileBased());
    }
  }

}
