package com.ms.silverking.testing;

import static com.ms.silverking.testing.Util.createList;
import static com.ms.silverking.testing.Util.createSet;
import static org.junit.jupiter.api.Assertions.*;

import com.ms.silverking.testing.Util.ExceptionChecker;
import java.util.Collection;

public class Assert {

  public static void exceptionConditionChecker(boolean expected, ExceptionChecker ec) {
    boolean started = true;
    try {
      ec.check();
    } catch (RuntimeException re) {
      started = false;
    }

    assertEquals(expected, started);
  }

  public static void exceptionNameChecker(ExceptionChecker ec, String testMessage, Class<?> expected) {
    try {
      ec.check();
      fail(testMessage);
    } catch (Exception ex) {
      checkException(testMessage, expected, ex);
    }
  }

  public static void checkException(String testMessage, Class<?> expected, Exception ex) {
    assertEquals(expected, ex.getClass(), testMessage);
  }

  public static void assertPass(String msg) {
    assertTrue(true, msg);
  }

  public static void assertZero(int actualSize) {
    assertEquals(0, actualSize);
  }

  public static <T> void checkEqualsEmptySet(Collection<T> actual) {
    assertEquals(createSet(), actual);
  }

  public static <T> void checkEqualsSetOne(Collection<T> actual) {
    assertEquals(createSet(1), actual);
  }

  public static <T> void checkEqualsEmptyList(Collection<T> actual) {
    assertEquals(createList(), actual);
  }
}
