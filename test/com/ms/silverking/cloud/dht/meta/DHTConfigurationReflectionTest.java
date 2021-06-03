package com.ms.silverking.cloud.dht.meta;

import static com.ms.silverking.cloud.dht.common.DHTConstants.defaultNamespaceOptions;
import static com.ms.silverking.cloud.dht.meta.DHTConfiguration.defaultEnableMsgGroupTrace;
import static com.ms.silverking.cloud.dht.meta.DHTConfiguration.defaultNamespaceOptionsMode;
import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.*;

import com.ms.silverking.cloud.dht.NamespaceCreationOptions;
import com.ms.silverking.cloud.dht.common.NamespaceOptionsMode;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class DHTConfigurationReflectionTest {
  @Test
  public void reflectLegacyDHTConfig() {
    // Legacy DHTConfig def
    String legacyDHTConfig = "ringName=ring.85071aaa-afbe-4ca3-acb6-5fc570b360a4,port=5836," + "passiveNodeHostGroups"
        + "={none}," + "nsCreationOptions={defaultNSOptions={storageType=FILE," + "consistencyProtocol=LOOSE," +
        "versionMode=SINGLE_VERSION," + "revisionMode=NO_REVISIONS," + "defaultPutOptions={opTimeoutController" +
        "=<OpSizeBasedTimeoutController>" + "{maxAttempts=4," + "constantTime_ms=300000,itemTime_ms=305," +
        "nonKeyedOpMaxRelTimeout_ms=1500000," + "exclusionChangeRetryInterval_ms=5000}," + "compression=LZ4," +
        "checksumType=MURMUR3_32," + "checksumCompressedValues=false,version=0,requiredPreviousVersion=0," +
        "lockSeconds=0," + "fragmentationThreshold" + "=10485760,},defaultInvalidationOptions={opTimeoutController" + "=<OpSizeBasedTimeoutController>{maxAttempts=4," + "constantTime_ms=300000," + "itemTime_ms=305," + "nonKeyedOpMaxRelTimeout_ms=1500000," + "exclusionChangeRetryInterval_ms=5000},compression=NONE," + "checksumType=SYSTEM," + "checksumCompressedValues" + "=false,version=0,requiredPreviousVersion=0," + "lockSeconds=0,fragmentationThreshold=10485760,}," + "defaultGetOptions={opTimeoutController" + "=<OpSizeBasedTimeoutController>{maxAttempts=4,constantTime_ms=300000," + "itemTime_ms=305," + "nonKeyedOpMaxRelTimeout_ms=1500000,exclusionChangeRetryInterval_ms=5000}," + "retrievalType" + "=VALUE," + "waitMode=GET,versionConstraint={min=-9223372036854775808,max=9223372036854775807,mode=GREATEST," + "maxCreationTime=9223372036854775807},nonExistenceResponse=NULL_VALUE," + "verifyChecksums=true," + "returnInvalidations=false,forwardingMode=FORWARD,updateSecondariesOnMiss=false,}," + "defaultWaitOptions" + "={opTimeoutController=<WaitForTimeoutController>{internalRetryIntervalSeconds=20," + "internalExclusionChangeRetryIntervalSeconds=2},retrievalType=VALUE,waitMode=WAIT_FOR," + "versionConstraint" + "={min=-9223372036854775808,max=9223372036854775807,mode=GREATEST," + "maxCreationTime=9223372036854775807}," + "nonExistenceResponse=NULL_VALUE,verifyChecksums=true," + "returnInvalidations=false,forwardingMode=FORWARD,updateSecondariesOnMiss=false,timeoutSeconds=2147483647," + "threshold=100,timeoutResponse=EXCEPTION}," + "secondarySyncIntervalSeconds=1800,segmentSize=67108864," + "maxValueSize=1073741824,allowLinks=false," + "valueRetentionPolicy=<InvalidatedRetentionPolicy>{invalidatedRetentionIntervalSeconds=60}," + "namespaceServerSideCode={url=,putTrigger=,retrieveTrigger=}}},hostGroupToClassVarsMap={Host.VM=host_vars}," + "version=0,zkid=-9223372036854775808,defaultClassVars=host_vars";

    DHTConfiguration parsed = DHTConfiguration.parse(legacyDHTConfig, 0);
    assertEquals(
        parsed.getNamespaceOptionsMode(), defaultNamespaceOptionsMode, getTestMessage("reflectLegacyDHTConfig",
        "legacy dhg shall be reflected as defaultMode [" + defaultNamespaceOptionsMode + "]"));

    assertEquals(
        parsed.getNamespaceOptionsMode(), defaultNamespaceOptionsMode, getTestMessage("reflectLegacyDHTConfig",
        "legacy dhg shall be reflected as defaultEnableMsgGroupTrace [" + defaultEnableMsgGroupTrace + "]"));
  }

  @Test
  public void newFieldShallBeIncluded() {
    DHTConfiguration dhtCfg = new DHTConfiguration("Steel_Ball_Run", 1996, "Stone_Ocean",
        new NamespaceCreationOptions(NamespaceCreationOptions.Mode.OptionalAutoCreation_AllowMatches, ".*",
            defaultNamespaceOptions), new HashMap<>(), NamespaceOptionsMode.ZooKeeper, 0, 0, "Golden_Wind", "Foo",
        false, false);
    assertTrue(
        dhtCfg.toString().contains("namespaceOptionsMode=ZooKeeper"), getTestMessage("newFieldShallBeIncluded", "reflection string shall have [namespaceOptionsMode] field"));

    assertTrue(
        dhtCfg.toString().contains("enableMsgGroupTrace=false"), getTestMessage("newFieldShallBeIncluded", "reflection string shall have [enableMsgGroupTrace] field"));
  }

  @Test
  public void reflectNewVersionDHTConfig() {
    String newDHTConfig = "ringName=ring.85071aaa-afbe-4ca3-acb6-5fc570b360a4,port=5836,passiveNodeHostGroups=," +
        "nsCreationOptions={mode=OptionalAutoCreation_AllowMatches,regex=^_.*," + "defaultNSOptions={storageType" +
        "=FILE,consistencyProtocol=TWO_PHASE_COMMIT,versionMode=SINGLE_VERSION," + "revisionMode=NO_REVISIONS," +
        "defaultPutOptions={opTimeoutController=<OpSizeBasedTimeoutController>{maxAttempts=4," + "constantTime_ms" +
        "=300000,itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000,exclusionChangeRetryInterval_ms=5000}," +
        "compression=LZ4,checksumType=MURMUR3_32,checksumCompressedValues=false,version=0,requiredPreviousVersion=0," + "lockSeconds=0,fragmentationThreshold=10485760,}," + "defaultInvalidationOptions={opTimeoutController=<OpSizeBasedTimeoutController>{maxAttempts=4,constantTime_ms=300000," + "itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000,exclusionChangeRetryInterval_ms=5000},compression=NONE,checksumType=SYSTEM," + "checksumCompressedValues=false,version=0,requiredPreviousVersion=0,lockSeconds=0,fragmentationThreshold=10485760,}," + "defaultGetOptions={opTimeoutController=<OpSizeBasedTimeoutController>{maxAttempts=4,constantTime_ms=300000,itemTime_ms=305," + "nonKeyedOpMaxRelTimeout_ms=1500000,exclusionChangeRetryInterval_ms=5000},retrievalType=VALUE,waitMode=GET,versionConstraint=" + "{min=-9223372036854775808,max=9223372036854775807,mode=GREATEST,maxCreationTime=9223372036854775807}," + "nonExistenceResponse=NULL_VALUE,verifyChecksums=true,returnInvalidations=false,forwardingMode=FORWARD,updateSecondariesOnMiss=false,}," + "defaultWaitOptions={opTimeoutController=<WaitForTimeoutController>{internalRetryIntervalSeconds=20," + "internalExclusionChangeRetryIntervalSeconds=2},retrievalType=VALUE,waitMode=WAIT_FOR,versionConstraint={min=-9223372036854775808," + "max=9223372036854775807,mode=GREATEST,maxCreationTime=9223372036854775807},nonExistenceResponse=NULL_VALUE,verifyChecksums=true," + "returnInvalidations=false,forwardingMode=FORWARD,updateSecondariesOnMiss=false,timeoutSeconds=2147483647,threshold=100,timeoutResponse=EXCEPTION}," + "secondarySyncIntervalSeconds=1800,segmentSize=67108864,maxValueSize=1073741824,allowLinks=false,valueRetentionPolicy=<InvalidatedRetentionPolicy>{invalidatedRetentionIntervalSeconds=60},}}," + "hostGroupToClassVarsMap={SimpleHostGroup=classVars.85071aaa-afbe-4ca3-acb6-5fc570b360a4}," + "namespaceOptionsMode=ZooKeeper," + "version=0,zkid=-9223372036854775808," + "ipAliasMap={192.168.0.0.1=10.102.102.10:7777,192.168.0.0.2=10.102.102.11:7777,192.168.0.0.3=10.102.102.10:6614}," + "unDeclaredFieldsShouldBeIgnored=Yes," + "ipAliasMapName=Foo," + "enableMsgGroupTrace=true";

    DHTConfiguration parsed = DHTConfiguration.parse(newDHTConfig, 0);
    assertEquals(parsed.getNamespaceOptionsMode(),
        NamespaceOptionsMode.ZooKeeper, getTestMessage("reflectNewVersionDHTConfig",
        "new version of test dhgConfig shall be reflected as [ZooKeeper] mode"));
    assertNotNull(
        parsed.getIpAliasMapName(), getTestMessage("reflectNewVersionDHTConfig", "alias node name must be read from config"));
    assertEquals("Foo",
        parsed.getIpAliasMapName(), getTestMessage("reflectNewVersionDHTConfig", "alias node name must be read from config"));
    assertTrue(
        parsed.getEnableMsgGroupTrace(), getTestMessage("reflectNewVersionDHTConfig", "enableMsgGroupTrace shall be true"));
  }
}
