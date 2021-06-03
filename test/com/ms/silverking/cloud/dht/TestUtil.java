package com.ms.silverking.cloud.dht;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ms.silverking.cloud.dht.VersionConstraint.Mode;
import com.ms.silverking.cloud.dht.client.ChecksumType;
import com.ms.silverking.cloud.dht.client.Compression;
import com.ms.silverking.cloud.dht.client.OpSizeBasedTimeoutController;
import com.ms.silverking.cloud.dht.client.WaitForTimeoutController;
import com.ms.silverking.cloud.dht.client.gen.OmitGeneration;
import com.ms.silverking.cloud.dht.common.DHTConstants;
import com.ms.silverking.cloud.dht.common.DHTKey;
import com.ms.silverking.cloud.dht.daemon.storage.retention.KeyLevelValueRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.ValueRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.ValueRetentionPolicyImpl.ImplementationType;
import com.ms.silverking.cloud.dht.daemon.storage.retention.ValueRetentionState;
import java.util.HashSet;

@OmitGeneration
public class TestUtil {

  // using ImmutableSet.of() to add variety of param testing
  public static final PutOptions poCopy = new PutOptions(new OpSizeBasedTimeoutController(), null, Compression.LZ4,
      ChecksumType.MURMUR3_32, false, 0, 0, PutOptions.noLock, DHTConstants.defaultFragmentationThreshold, null);
  public static final PutOptions poDiff = new PutOptions(new WaitForTimeoutController(), new HashSet<>(),
      Compression.LZ4, ChecksumType.MURMUR3_32, false, 0, 0, PutOptions.noLock,
      DHTConstants.defaultFragmentationThreshold, null);

  public static final InvalidationOptions ioCopy = new InvalidationOptions(new OpSizeBasedTimeoutController(), null,
      0, 0, PutOptions.noLock);
  public static final InvalidationOptions ioDiff = new InvalidationOptions(new WaitForTimeoutController(),
      new HashSet<>(), 0, 0, PutOptions.noLock);

  public static final GetOptions goCopy = new GetOptions(new OpSizeBasedTimeoutController(), null, RetrievalType.VALUE,
      new VersionConstraint(Long.MIN_VALUE, Long.MAX_VALUE, Mode.GREATEST), NonExistenceResponse.NULL_VALUE, true,
      false, ForwardingMode.FORWARD, false);
  public static final GetOptions goDiff = new GetOptions(new WaitForTimeoutController(), new HashSet<>(),
      RetrievalType.VALUE, new VersionConstraint(Long.MIN_VALUE, Long.MAX_VALUE, Mode.GREATEST),
      NonExistenceResponse.NULL_VALUE, true, false, ForwardingMode.FORWARD, false);

  public static final WaitOptions woCopy = new WaitOptions(new WaitForTimeoutController(), null, RetrievalType.VALUE,
      new VersionConstraint(Long.MIN_VALUE, Long.MAX_VALUE, Mode.GREATEST), NonExistenceResponse.NULL_VALUE, true,
      false, false, Integer.MAX_VALUE, 100, TimeoutResponse.EXCEPTION);
  public static final WaitOptions woDiff = new WaitOptions(new WaitForTimeoutController(), new HashSet<>(),
      RetrievalType.EXISTENCE, new VersionConstraint(Long.MIN_VALUE, Long.MAX_VALUE, Mode.GREATEST),
      NonExistenceResponse.NULL_VALUE, true, false, false, Integer.MAX_VALUE, 100, TimeoutResponse.EXCEPTION);

  public static ImplementationType getImplementationType(ValueRetentionPolicy policy) {
    return ValueRetentionPolicyImpl.fromPolicy(policy, null).getImplementationType();
  }

  public static void checkRetains(Object[][] testCases) {
    for (Object[] testCase : testCases) {
      ValueRetentionPolicy policy = (ValueRetentionPolicy) testCase[0];
      ValueRetentionPolicyImpl impl = ValueRetentionPolicyImpl.fromPolicy(policy, null);
      DHTKey key = (DHTKey) testCase[1];
      long version = (long) testCase[2];
      long creationTimeNanos = (long) testCase[3];
      boolean invalidated = (boolean) testCase[4];
      ValueRetentionState state = (ValueRetentionState) testCase[5];
      long curTimeNanos = (long) testCase[6];
      boolean expected = (boolean) testCase[7];

      if (impl instanceof KeyLevelValueRetentionPolicyImpl<?>) {
        assertEquals(expected,
            ((KeyLevelValueRetentionPolicyImpl<ValueRetentionState>) impl).retains(key, version, creationTimeNanos,
                invalidated, state, curTimeNanos, -1));
    }
  }
  }

}
