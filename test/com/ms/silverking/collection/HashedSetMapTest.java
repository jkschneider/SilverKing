package com.ms.silverking.collection;

import static com.ms.silverking.collection.TestUtil.*;
import static com.ms.silverking.testing.Assert.checkEqualsEmptyList;
import static com.ms.silverking.testing.Assert.checkEqualsEmptySet;
import static com.ms.silverking.testing.Assert.checkEqualsSetOne;
import static com.ms.silverking.testing.Util.createList;
import static com.ms.silverking.testing.Util.createSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HashedSetMapTest {

  private HashedSetMap<Integer, Integer> map;

  @BeforeEach
  public void setUp() throws Exception {
    clearMap();
  }

  private void clearMap() {
    map = new HashedSetMap<>();
  }

  @Test
  public void testEmpty() {
    checkEqualsEmptySet(map.keySet());
    checkContainsValue(null, null, false);
    checkListSize(null, 0);
    checkRemoveValue(null, null, false);
    checkGetAnyValue(null, null);
    checkGetSet(null, null);
    checkEqualsEmptyList(map.getSets());
    checkEqualsEmptyList(map.getKeys());
    checkRemoveSet(null, null);
    checkGetNumKeys(0);
  }

  @Test
  public void testSequence() {
    checkListSize(key1, 0);

    putElement_1_1();
    int expectedSize = 1;
    checkEqualsSetOne(map.keySet());
    checkContainsValue(key1, value1, true);
    checkListSize(key1, expectedSize);
    checkGetAnyValue(key1, value1);
    Set<Integer> expectedSet = createSet(value1);
    checkGetSet(key1, expectedSet);
    checkGetSets(createList(expectedSet));
    checkGetKeys(createList(key1));
    checkGetNumKeys(1);

    putElement_1_1();
    checkEqualsSetOne(map.keySet());
    checkContainsValue(key1, value1, true);
    checkListSize(key1, expectedSize);
    checkGetAnyValue(key1, value1);
    checkGetSet(key1, expectedSet);
    checkGetSets(createList(expectedSet));
    checkGetKeys(createList(key1));
    checkGetNumKeys(1);

    putElement_1_2();
    expectedSize = 2;
    checkEqualsSetOne(map.keySet());
    checkContainsValue(key1, value1, true);
    checkContainsValue(key1, value2, true);
    checkListSize(key1, expectedSize);
    expectedSet = createSet(value1, value2);
    checkGetSet(key1, expectedSet);
    checkGetSets(createList(expectedSet));
    checkGetKeys(createList(key1));
    checkGetNumKeys(1);
  }

  @Test
  public void testFillOnGet() {
    map = new HashedSetMap<>(true);
    checkGetSet(1, createSet());
    //        checkGetAnyValue(1, null);
  }

  @Test
  public void testAddRemove() {
    putElement_1_1();
    putElement_1_1();
    checkRemoveValue(key1, value1, true);
    checkRemoveValue(key1, value1, false);

    putElement_1_1();
    putElement_1_2();
    checkRemoveValue(key1, value1, true);
    checkGetAnyValue(key1, value2);

    // going to be 2,1,3,4
    putElement_1_1();
    int expectedSize = 4;
    map.addValues(key1, createList(2, 3, 4));
    checkListSize(key1, expectedSize);

    checkRemoveValue(key1, 4, true);
    checkRemoveSet(key1, createSet(value2, value1, 3));
    checkGetNumKeys(0);
  }

  @Test
  public void testGetSet() {
    put15Elements();
    checkGetSet(key1, createSet_12345());
    checkGetSet(2, createSet_12345());
    checkGetSet(3, createSet_12345());
  }

  @Test
  public void testRemoveSet() {
    put15Elements();
    checkRemoveSet(key1, createSet_12345());
    checkRemoveSet(key1, null);
    checkRemoveSet(2, createSet_12345());
    checkRemoveSet(3, createSet_12345());
    checkGetNumKeys(0);
  }

  @Test
  public void testGetSets() {
    put15Elements();
    List<Set<Integer>> expected = new ArrayList<>();
    expected.add(createSet_12345());
    expected.add(createSet_12345());
    expected.add(createSet_12345());
    checkGetSets(expected);
  }

  private void putElement_1_1() {
    map.addValue(key1, value1);
  }

  private void putElement_1_2() {
    map.addValue(key1, value2);
  }

  private void put15Elements() {
    put5Elements(key1);
    put5Elements(2);
    put5Elements(3);
  }

  private void put5Elements(Integer key) {
    map.addValues(key, createSet_12345());
  }

  private Set<Integer> createSet_12345() {
    return createSet(value1, 2, 3, 4, 5);
  }

  private void checkListSize(Integer key, int expectedSize) {
    assertEquals(expectedSize, map.listSize(key), "Checking list size");
  }

  private void checkContainsValue(Integer key, Integer value, boolean expected) {
    assertEquals(expected, map.containsValue(key, value), "Checking contains value");
  }

  private void checkRemoveValue(Integer key, Integer value, boolean expected) {
    assertEquals(expected, map.removeValue(key, value), "Checking remove value");
  }

  private void checkGetAnyValue(Integer key, Integer expected) {
    assertEquals(expected, map.getAnyValue(key), "Checking get any value");
  }

  private void checkGetSet(Integer key, Set<Integer> expected) {
    assertEquals(expected, map.getSet(key), "Checking get set");
  }

  private void checkRemoveSet(Integer key, Set<Integer> expected) {
    assertEquals(expected, map.removeSet(key), "Checking remove set");
  }

  private void checkGetSets(List<Set<Integer>> expected) {
    assertEquals(expected, map.getSets(), "Checking get sets");
  }

  private void checkGetKeys(List<Integer> expected) {
    assertEquals(expected, map.getKeys(), "Checking get keys");
  }

  private void checkGetNumKeys(int expected) {
    assertEquals(expected, map.getNumKeys(), "Checking get num keys");
  }
}
