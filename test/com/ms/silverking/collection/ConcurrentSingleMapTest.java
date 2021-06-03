package com.ms.silverking.collection;

import static com.ms.silverking.collection.TestUtil.*;
import static com.ms.silverking.testing.Assert.checkEqualsEmptySet;
import static com.ms.silverking.testing.Assert.checkEqualsSetOne;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;

public class ConcurrentSingleMapTest {

  private ConcurrentSingleMap<Integer, Integer> map;

  @BeforeEach
  public void setUp() throws Exception {
    clearMap();
  }

  private void clearMap() {
    map = new ConcurrentSingleMap<>();
  }

  @Test
  public void testEmpty() {
    assertTrue(map.isEmpty());
    checkSize(0);
    checkContainsKey(null, false);
    checkContainsValue(null, false);
    assertNull(map.get(null));
    checkEqualsEmptySet(map.keySet());
    checkEqualsEmptySet(map.values());
    checkPutReturnsNull(null, null);
    //        checkEqualsEmptySet(map.keySet()); FIXME:bph: NPE b/c kvPair.key in keySet() is null
    //        checkEqualsEmptySet(map.values()); FIXME:bph: NPE b/c kvPair.key in keySet() is null
  }

  @Test
  public void testPut() {
    checkPutReturnsNull(key1, value2);
    checkContainsKey(key1, true);
    checkContainsValue(value2, true);
    assertEquals(value2, map.get(key1).intValue());
  }

  @Test(expected = RuntimeException.class)
  public void testPut_AlreadySet() {
    putElement_1_1();
    putElement_1_2();
  }

  @Test
  public void testKeySet() {
    checkEqualsEmptySet(map.keySet());
    putElement_1_1();
    checkEqualsSetOne(map.keySet());
  }

  @Test
  public void testValues() {
    checkEqualsEmptySet(map.values());
    putElement_1_1();
    checkEqualsSetOne(map.values());
  }

  @Test
  public void testEntrySet() {
    checkEqualsEmptySet(map.entrySet());
    // FUTURE:bph: maybe should add more here
  }

  private void putElement_1_1() {
    map.put(key1, value1);
  }

  private void putElement_1_2() {
    map.put(key1, value2);
  }

  private void checkSize(int expectedSize) {
    assertEquals(expectedSize, map.size());
  }

  private void checkContainsKey(Object key, boolean expected) {
    assertEquals(expected, map.containsKey(key));
  }

  private void checkContainsValue(Object value, boolean expected) {
    assertEquals(expected, map.containsValue(value));
  }

  private void checkPutReturnsNull(Integer key, Integer value) {
    assertNull(map.put(key, value));
  }
}
