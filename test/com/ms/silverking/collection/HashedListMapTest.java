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

public class HashedListMapTest {

  private HashedListMap<Integer, Integer> map;

  @BeforeEach
  public void setUp() throws Exception {
    clearMap();
  }

  private void clearMap() {
    map = new HashedListMap<>();
  }

  @Test
  public void testEmpty() {
    checkEqualsEmptySet(map.keySet());
    checkEqualsEmptySet(map.valueSet());
    checkContainsValue(null, null, false);
    checkTotalSize(0);
    checkListSize(null, 0);
    checkRemoveValue(null, null, false);
    checkGetFirstValue(null, null);
    checkRemoveFirstValue(null, null);
    checkGetList(null, null);
    checkEqualsEmptyList(map.getLists());
    checkEqualsEmptyList(map.getKeys());
    checkRemoveList(null, null);
  }

  @Test
  public void testSequence() {
    checkTotalSize(0);
    checkListSize(key1, 0);

    putElement_1_1();
    int expectedSize = 1;
    checkEqualsSetOne(map.keySet());
    checkEqualsSetOne(map.valueSet());
    checkContainsValue(key1, value1, true);
    checkTotalSize(expectedSize);
    checkListSize(key1, expectedSize);
    checkGetFirstValue(key1, value1);
    List<Integer> expectedList = createList(value1);
    checkGetList(key1, expectedList);
    checkGetLists(createList(expectedList));
    checkGetKeys(createList(key1));

    putElement_1_1();
    expectedSize = 2;
    checkEqualsSetOne(map.keySet());
    checkEqualsSetOne(map.valueSet());
    checkContainsValue(key1, value1, true);
    checkTotalSize(expectedSize);
    checkListSize(key1, expectedSize);
    checkGetFirstValue(key1, value1);
    expectedList = createList(value1, value1);
    checkGetList(key1, expectedList);
    checkGetLists(createList(expectedList));
    checkGetKeys(createList(key1));

    putElement_1_2();
    expectedSize = 3;
    checkEqualsSetOne(map.keySet());
    checkValueSet(createSet(value1, value2));
    checkContainsValue(key1, value1, true);
    checkContainsValue(key1, value2, true);
    checkTotalSize(expectedSize);
    checkListSize(key1, expectedSize);
    checkGetFirstValue(key1, value1);
    expectedList = createList(value1, value1, value2);
    checkGetList(key1, expectedList);
    checkGetLists(createList(expectedList));
    checkGetKeys(createList(key1));
  }

  @Test
  public void testFillOnGet() {
    map = new HashedListMap<>(true);
    checkGetList(1, createList());
    checkGetFirstValue(1, null);
  }

  @Test
  public void testAddRemove() {
    putElement_1_1();
    putElement_1_1();
    checkRemoveValue(key1, value1, true);
    checkRemoveValue(key1, value1, true);

    putElement_1_1();
    putElement_1_2();
    checkGetFirstValue(key1, value1);
    checkRemoveFirstValue(key1, value1);
    checkGetFirstValue(key1, value2);

    // going to be 2,1,2,3,4
    putElement_1_1();
    int expectedSize = 5;
    map.addValues(key1, createList(2, 3, 4));
    checkListSize(key1, expectedSize);
    checkTotalSize(expectedSize);

    checkRemoveValue(key1, 4, true);
    checkRemoveList(key1, createList(value2, value1, 2, 3));
    checkTotalSize(0);
  }

  @Test
  public void testGetList() {
    put15Elements();
    checkGetList(key1, createList_12345());
    checkGetList(2, createList_12345());
    checkGetList(3, createList_12345());
  }

  @Test
  public void testRemoveList() {
    put15Elements();
    checkRemoveList(key1, createList_12345());
    checkRemoveList(key1, null);
    checkRemoveList(2, createList_12345());
    checkRemoveList(3, createList_12345());
    checkTotalSize(0);
  }

  @Test
  public void testGetLists() {
    put15Elements();
    List<List<Integer>> expected = new ArrayList<>();
    expected.add(createList_12345());
    expected.add(createList_12345());
    expected.add(createList_12345());
    checkGetLists(expected);
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
    map.addValues(key, createList_12345());
  }

  private List<Integer> createList_12345() {
    return createList(value1, 2, 3, 4, 5);
  }

  private void checkValueSet(Set<Integer> expected) {
    assertEquals(expected, map.valueSet(), "Checking value set");
  }

  private void checkTotalSize(int expectedSize) {
    assertEquals(expectedSize, map.totalSize(), "Checking total size");
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

  private void checkGetFirstValue(Integer key, Integer expected) {
    assertEquals(expected, map.getFirstValue(key), "Checking get first value");
  }

  private void checkRemoveFirstValue(Integer key, Integer expected) {
    assertEquals(expected, map.removeFirstValue(key), "Checking remove first value");
  }

  private void checkGetList(Integer key, List<Integer> expected) {
    assertEquals(expected, map.getList(key), "Checking get list");
  }

  private void checkRemoveList(Integer key, List<Integer> expected) {
    assertEquals(expected, map.removeList(key), "Checking remove list");
  }

  private void checkGetLists(List<List<Integer>> expected) {
    assertEquals(expected, map.getLists(), "Checking get lists");
  }

  private void checkGetKeys(List<Integer> expected) {
    assertEquals(expected, map.getKeys(), "Checking get keys");
  }
}
