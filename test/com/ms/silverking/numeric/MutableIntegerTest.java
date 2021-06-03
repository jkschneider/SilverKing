package com.ms.silverking.numeric;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MutableIntegerTest {

  private MutableInteger mi;

  @BeforeEach
  public void setUp() throws Exception {
    mi = new MutableInteger(0);
  }

  @Test
  public void testMutableInteger() {
    for (int i = 0; i < 1000; i++) {
      checkGetValue(i);
      checkHashCode(i);
      mi.increment();
    }

    for (int i = 1000; i > -1000; i--) {
      checkGetValue(i);
      checkHashCode(i);
      mi.decrement();
    }

    // test overflow wrap-arounds
    mi = new MutableInteger(MAX_VALUE);
    checkGetValue(MAX_VALUE);
    checkHashCode(MAX_VALUE);
    mi.increment();
    checkGetValue(MIN_VALUE);
    checkHashCode(MIN_VALUE);
    mi.decrement();
    checkGetValue(MAX_VALUE);
    checkHashCode(MAX_VALUE);
  }

  private void checkGetValue(int expected) {
    assertEquals(expected, mi.getValue());
  }

  private void checkHashCode(int expected) {
    assertEquals(expected, mi.hashCode());
  }

}
