package com.ms.silverking.cloud.dht;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableSet;
import com.ms.silverking.cloud.dht.client.SecondaryTargetType;
import com.ms.silverking.cloud.dht.client.gen.OmitGeneration;
import com.ms.silverking.cloud.dht.common.DHTConstants;
import com.ms.silverking.cloud.dht.common.InternalRetrievalOptions;
import com.ms.silverking.cloud.dht.net.MessageGroup;
import com.ms.silverking.cloud.dht.net.ProtoRetrievalMessageGroup;
import com.ms.silverking.cloud.dht.net.protocol.RetrievalMessageFormat;
import com.ms.silverking.cloud.dht.trace.TraceIDProvider;
import com.ms.silverking.id.UUIDBase;
import java.util.Set;
import org.junit.jupiter.api.Test;

@OmitGeneration
public class ArbitraryUserRetrievalOptionsTest {

  private void assertUserOptions(byte[] testUserOpts, String expectedStr, byte[] expectedBytes) {

    assertNotNull(testUserOpts, "rebuilt options should not be null");
    assertTrue(testUserOpts.length > 0, "rebuilt options should not be empty");

    String rebuiltStr = new String(testUserOpts);
    assertEquals(expectedStr, rebuiltStr, "rebuilt user options should match original string");

    assertArrayEquals(expectedBytes, testUserOpts, "rebuilt user options should match original bytes");
  }

  @Test
  public void testUserOptionsPreserved() {
    String usrStr = "dog,123";
    byte[] usrBytes = usrStr.getBytes();
    GetOptions opts = DHTConstants.standardGetOptions.userOptions(usrBytes);

    InternalRetrievalOptions internalOpts = new InternalRetrievalOptions(opts);

    byte[] originator = { 0, 1, 2, 3, 4, 5, 6, 7 };

    ProtoRetrievalMessageGroup message = new ProtoRetrievalMessageGroup(UUIDBase.random(), 1L, internalOpts, originator,
        1, MessageGroup.minDeadlineRelativeMillis, ForwardingMode.DO_NOT_FORWARD, TraceIDProvider.noTraceID);

    RetrievalOptions rebuiltOpts = ProtoRetrievalMessageGroup.getRetrievalOptions(
        message.toMessageGroup()).getRetrievalOptions();

    int optsLength = RetrievalMessageFormat.getOptionsBufferLength(rebuiltOpts);
    int expectedLength = RetrievalMessageFormat.getOptionsBufferLength(opts);
    assertEquals(expectedLength, optsLength, "options length should be preserved");

    byte[] rebuiltUserOpts = rebuiltOpts.getUserOptions();
    assertUserOptions(rebuiltUserOpts, usrStr, usrBytes);
  }

  @Test
  public void testUserOptionsAndSecondaryTargetsPreserved() {
    String usrStr = "dog,123";
    byte[] usrBytes = usrStr.getBytes();

    SecondaryTarget sta = new SecondaryTarget(SecondaryTargetType.NodeID, "foo");
    SecondaryTarget stb = new SecondaryTarget(SecondaryTargetType.NodeID, "bar");
    Set<SecondaryTarget> secondaryTargets = ImmutableSet.of(sta, stb);

    GetOptions opts = DHTConstants.standardGetOptions.userOptions(usrBytes).secondaryTargets(secondaryTargets);
    InternalRetrievalOptions internalOpts = new InternalRetrievalOptions(opts);

    byte[] originator = { 0, 1, 2, 3, 4, 5, 6, 7 };
    ProtoRetrievalMessageGroup message = new ProtoRetrievalMessageGroup(UUIDBase.random(), 1L, internalOpts, originator,
        1, MessageGroup.minDeadlineRelativeMillis, ForwardingMode.DO_NOT_FORWARD, TraceIDProvider.noTraceID);

    RetrievalOptions rebuiltOpts = ProtoRetrievalMessageGroup.getRetrievalOptions(
        message.toMessageGroup()).getRetrievalOptions();

    byte[] rebuiltUserOpts = rebuiltOpts.getUserOptions();

    int optsLength = RetrievalMessageFormat.getOptionsBufferLength(rebuiltOpts);
    int expectedLength = RetrievalMessageFormat.getOptionsBufferLength(opts);
    assertEquals(expectedLength, optsLength, "options length should be preserved");

    assertUserOptions(rebuiltUserOpts, usrStr, usrBytes);

    Set<SecondaryTarget> rebuiltSecondaries = rebuiltOpts.getSecondaryTargets();
    assertEquals(rebuiltSecondaries, secondaryTargets, "Rebuilt secondaries and original should match");
  }

  @Test
  public void testNullUserOptionsPreserved() {
    GetOptions opts = DHTConstants.standardGetOptions;
    assertNull(opts.getUserOptions(), "Standard options should have null user options");

    InternalRetrievalOptions internalOpts = new InternalRetrievalOptions(opts);

    byte[] originator = { 0, 1, 2, 3, 4, 5, 6, 7 };
    ProtoRetrievalMessageGroup message = new ProtoRetrievalMessageGroup(UUIDBase.random(), 1L, internalOpts, originator,
        1, MessageGroup.minDeadlineRelativeMillis, ForwardingMode.DO_NOT_FORWARD, TraceIDProvider.noTraceID);

    RetrievalOptions rebuiltOpts = ProtoRetrievalMessageGroup.getRetrievalOptions(
        message.toMessageGroup()).getRetrievalOptions();

    assertNull(rebuiltOpts.getUserOptions(), "Rebuilt options should still be null");

    int optsLength = RetrievalMessageFormat.getOptionsBufferLength(rebuiltOpts);
    int expectedLength = RetrievalMessageFormat.getOptionsBufferLength(opts);
    assertEquals(expectedLength, optsLength, "options length should be preserved");

  }

}
