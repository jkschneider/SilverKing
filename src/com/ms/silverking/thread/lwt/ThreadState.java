package com.ms.silverking.thread.lwt;

import com.ms.silverking.numeric.MutableInteger;

/**
 * Per-thread LWT state.
 */
final class ThreadState {
  private static final ThreadLocal<MutableInteger> depth = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> isLWTThread = new ThreadLocal<>();

  public static void setLWTThread() {
    isLWTThread.set(new Boolean(true));
  }

  public static boolean isLWTThread() {
    //if (Thread.currentThread() instanceof LWTThread) {
    if (Thread.currentThread() instanceof LWTCompatibleThread) {
      return true;
    } else {
      Boolean isLWT;

      isLWT = isLWTThread.get();
      return isLWT != null && isLWT.booleanValue();
    }
  }

  public static boolean isLWTCompatibleThread() {
    return Thread.currentThread() instanceof LWTCompatibleThread;
  }

  public static int getDepth() {
    Thread curThread;

    curThread = Thread.currentThread();
    if (curThread instanceof LWTThread) {
      return ((LWTThread) curThread).getDepth();
    } else {
      MutableInteger d;

      d = depth.get();
      if (d == null) {
        d = new MutableInteger();
        depth.set(d);
      }
      return d.getValue();
    }
  }

  public static void incrementDepth() {
    Thread curThread;

    curThread = Thread.currentThread();
    if (curThread instanceof LWTThread) {
      ((LWTThread) curThread).incrementDepth();
    } else {
      MutableInteger d;

      d = depth.get();
      if (d == null) {
        d = new MutableInteger();
        depth.set(d);
      }
      d.increment();
    }
  }

  public static void decrementDepth() {
    Thread curThread;

    curThread = Thread.currentThread();
    if (curThread instanceof LWTThread) {
      ((LWTThread) curThread).decrementDepth();
    } else {
      depth.get().decrement();
    }
  }
}
