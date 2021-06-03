package com.ms.silverking.cloud.dht.daemon.storage.fsm;

import com.ms.silverking.numeric.NumConversion;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class LTVElementTest {
  @Test
  public void ltvTest() {
    ByteBuffer b;
    LTVElement e;
    int length;
    int type;
    int value;

    length = NumConversion.BYTES_PER_INT * 3;
    type = 1;
    value = 0x01020304;

    b = ByteBuffer.allocate(12);
    b = b.order(ByteOrder.nativeOrder());
    b.putInt(length);
    b.putInt(type);
    b.putInt(value);

    e = new LTVElement(b);
    Assertions.assertEquals(length, e.getLength());
    Assertions.assertEquals(type, e.getType());
    Assertions.assertEquals(value, e.getValueBuffer().getInt());
  }
}
