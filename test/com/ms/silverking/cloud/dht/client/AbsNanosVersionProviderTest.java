package com.ms.silverking.cloud.dht.client;

import static com.ms.silverking.cloud.dht.client.TestUtil.absMillisProvider;
import static com.ms.silverking.cloud.dht.client.TestUtil.absNanosProvider;
import static com.ms.silverking.cloud.dht.client.TestUtil.constantProvider;
import static com.ms.silverking.cloud.dht.client.TestUtil.relNanosProvider;
import static com.ms.silverking.cloud.dht.client.TestUtil.test_GetVersion;
import static com.ms.silverking.testing.AssertFunction.checkHashCodeEquals;
import static com.ms.silverking.testing.AssertFunction.checkHashCodeNotEquals;
import static com.ms.silverking.testing.AssertFunction.test_FirstEqualsSecond_FirstNotEqualsThird;
import static com.ms.silverking.testing.AssertFunction.test_NotEquals;

import com.ms.silverking.cloud.dht.client.gen.OmitGeneration;
import com.ms.silverking.cloud.dht.common.SystemTimeUtil;
import com.ms.silverking.time.AbsNanosTimeSource;
import com.ms.silverking.time.SafeAbsNanosTimeSource;
import org.junit.jupiter.api.Test;

@OmitGeneration
public class AbsNanosVersionProviderTest {

  private static final AbsNanosTimeSource antsCopy = new SafeAbsNanosTimeSource(0);
  private static final AbsNanosTimeSource antsDiff = SystemTimeUtil.skSystemTimeSource;

  private static final AbsNanosVersionProvider absNanosProviderCopy = new AbsNanosVersionProvider(antsCopy);
  private static final AbsNanosVersionProvider absNanosProviderDiff = new AbsNanosVersionProvider(antsDiff);

  // FUTURE:bph: comments

  @Test
  public void testVersion() {
    Object[][] testCases = {
        //            {0L, absNanosProvider},    // SafeAbsNanosTimeSource - absTimeNanos is dynamic
        //            {0L, absNanosProviderCopy},    // SafeAbsNanosTimeSource - absTimeNanos is dynamic
        //            {, absNanosProviderDiff},    // SystemTimeSource - SafeAbsNanosTimeSource - absTimeNanos is
        //            dynamic
    };

    test_GetVersion(testCases);
  }

  @Test
  public void testHashCode() {
    checkHashCodeEquals(absNanosProvider, absNanosProvider);
    checkHashCodeNotEquals(absNanosProvider,
        absNanosProviderCopy);    // SafeAbsNanosTimeSource - hashcode uses identity
    checkHashCodeNotEquals(absNanosProvider, absNanosProviderDiff);
  }

  @Test
  public void testEqualsObject() {
    AbsNanosVersionProvider[][] testCases = { { absNanosProvider, absNanosProvider, absNanosProviderDiff },
        { absNanosProviderDiff, absNanosProviderDiff, absNanosProvider }, };
    test_FirstEqualsSecond_FirstNotEqualsThird(testCases);

    test_NotEquals(new Object[][] { { absNanosProvider, absMillisProvider }, { absNanosProvider, constantProvider },
        { absNanosProvider, relNanosProvider }, { absNanosProvider, absNanosProviderCopy },
        // SafeAbsNanosTimeSource - hashcode uses identity
    });
  }
}
