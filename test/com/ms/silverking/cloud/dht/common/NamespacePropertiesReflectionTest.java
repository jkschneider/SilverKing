package com.ms.silverking.cloud.dht.common;

import static com.ms.silverking.testing.Util.getTestMessage;
import static org.junit.jupiter.api.Assertions.*;

import com.ms.silverking.cloud.dht.ConsistencyProtocol;
import com.ms.silverking.cloud.dht.LRURetentionPolicy;
import com.ms.silverking.cloud.dht.NamespaceOptions;
import com.ms.silverking.cloud.dht.NamespaceServerSideCode;
import com.ms.silverking.cloud.dht.NamespaceVersionMode;
import com.ms.silverking.cloud.dht.RevisionMode;
import com.ms.silverking.cloud.dht.StorageType;
import com.ms.silverking.cloud.dht.daemon.storage.serverside.LRUTrigger;
import com.ms.silverking.cloud.dht.trace.NoTraceIDProvider;
import org.junit.jupiter.api.Test;

public class NamespacePropertiesReflectionTest {
  private static final long nanosPerMilli = 1000000;

  final private NamespaceOptions dummyNsOptions = new NamespaceOptions(StorageType.FILE, ConsistencyProtocol.LOOSE,
      NamespaceVersionMode.SYSTEM_TIME_NANOS, RevisionMode.NO_REVISIONS, NamespaceUtil.metaNSDefaultPutOptions,
      NamespaceUtil.metaNSDefaultInvalidationOptions, NamespaceUtil.metaNSDefaultGetOptions,
      DHTConstants.standardWaitOptions, 1996, 4096, 1996, false, DHTConstants.defaultStorageFormat,
      new LRURetentionPolicy(8L * 1024L * 1024L, LRURetentionPolicy.DO_NOT_PERSIST),
      NamespaceServerSideCode.singleTrigger(LRUTrigger.class));

  final private String dummyParentName = "SteelBallRun";
  final private String dummyNsName = "StoneFree";
  final private long dummyCreationTime = System.currentTimeMillis() * nanosPerMilli;
  final private long dummyMinVersion = 1996;

  @Test
  public void reflectionFull() {
    NamespaceProperties initialized = new NamespaceProperties(dummyNsOptions, dummyNsName, dummyParentName,
        dummyMinVersion, dummyCreationTime);

    String skDef = initialized.toString();
    NamespaceProperties parsed = NamespaceProperties.parse(skDef);
    String testName = "reflectionFull";
    assertTrue(
        initialized.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be true for full initialization"));
    assertTrue(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be true for full initialization"));
    assertTrue(
        initialized.hasName(), getTestMessage(testName, "reflection.hasName() shall be true for full initialization"));
    assertTrue(
        parsed.hasName(), getTestMessage(testName, "reflection.hasName() shall be true for full initialization"));
    assertEquals(initialized, parsed, getTestMessage(testName, "reflection shall work for full initialization"));
    assertEquals(skDef,
        parsed.toString(), getTestMessage(testName, "reflection.toString shall work for full initialization"));
  }

  @Test
  public void reflectionNoCreationTime() {
    NamespaceProperties initialized = (new NamespaceProperties(dummyNsOptions)).name(dummyNsName);

    String skDef = initialized.toString();
    NamespaceProperties parsed = NamespaceProperties.parse(skDef);
    String testName = "reflectionNoCreationTime";
    assertFalse(
        initialized.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertFalse(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertTrue(
        initialized.hasName(), getTestMessage(testName, "reflection.hasName() shall be true for partial initialization"));
    assertTrue(
        parsed.hasName(), getTestMessage(testName, "reflection.hasName() shall be true for partial initialization"));
    assertEquals(initialized, parsed, getTestMessage(testName, "reflection shall work for partial initialization"));
    assertEquals(skDef,
        parsed.toString(), getTestMessage(testName, "reflection.toString shall work for partial initialization"));
  }

  @Test
  public void reflectionNoName() {
    NamespaceProperties initialized = (new NamespaceProperties(dummyNsOptions)).creationTime(dummyCreationTime);

    String skDef = initialized.toString();
    NamespaceProperties parsed = NamespaceProperties.parse(skDef);
    String testName = "reflectionNoName";
    assertTrue(
        initialized.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be true for partial initialization"));
    assertTrue(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be true for partial initialization"));
    assertFalse(
        initialized.hasName(), getTestMessage(testName, "reflection.hasName() shall be false for partial initialization"));
    assertFalse(
        parsed.hasName(), getTestMessage(testName, "reflection.hasName() shall be false for partial initialization"));
    assertEquals(initialized, parsed, getTestMessage(testName, "reflection shall work for partial initialization"));
    assertEquals(skDef,
        parsed.toString(), getTestMessage(testName, "reflection.toString shall work for partial initialization"));
  }

  @Test
  public void reflectionNoCreationTimeNoName1() {
    NamespaceProperties initialized = new NamespaceProperties(dummyNsOptions);

    String skDef = initialized.toString();
    NamespaceProperties parsed = NamespaceProperties.parse(skDef);
    String testName = "reflectionNoCreationTimeNoName1";
    assertFalse(
        initialized.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertFalse(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertFalse(
        initialized.hasName(), getTestMessage(testName, "reflection.hasName() shall be false for partial initialization"));
    assertFalse(
        parsed.hasName(), getTestMessage(testName, "reflection.hasName() shall be false for partial initialization"));
    assertEquals(initialized, parsed, getTestMessage(testName, "reflection shall work for partial initialization"));
    assertEquals(skDef,
        parsed.toString(), getTestMessage(testName, "reflection.toString shall work for partial initialization"));
  }

  @Test
  public void reflectionNoCreationTimeNoName2() {
    NamespaceProperties initialized = new NamespaceProperties(dummyNsOptions, dummyParentName, 1);

    String skDef = initialized.toString();
    NamespaceProperties parsed = NamespaceProperties.parse(skDef);
    String testName = "reflectionNoCreationTimeNoName2";
    assertFalse(
        initialized.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertFalse(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertFalse(
        initialized.hasName(), getTestMessage(testName, "reflection.hasName() shall be false for partial initialization"));
    assertFalse(
        parsed.hasName(), getTestMessage(testName, "reflection.hasName() shall be false for partial initialization"));
    assertEquals(initialized, parsed, getTestMessage(testName, "reflection shall work for partial initialization"));
    assertEquals(skDef,
        parsed.toString(), getTestMessage(testName, "reflection.toString shall work for partial initialization"));
  }

  private void testDefaultOperationOptionsTraceIDProvider(String testName, NamespaceProperties parsed) {
    NamespaceOptions nsOptions = parsed.getOptions();
    assertTrue(
        nsOptions.getDefaultPutOptions().getTraceIDProvider() instanceof NoTraceIDProvider, getTestMessage(testName, "DefaultPutOptions shall use NoTraceIDProvider"));
    assertTrue(
        nsOptions.getDefaultGetOptions().getTraceIDProvider() instanceof NoTraceIDProvider, getTestMessage(testName, "DefaultGetOptions shall use NoTraceIDProvider"));
    assertTrue(
        nsOptions.getDefaultWaitOptions().getTraceIDProvider() instanceof NoTraceIDProvider, getTestMessage(testName, "DefaultWaitOptions shall use NoTraceIDProvider"));
    assertTrue(
        nsOptions.getDefaultInvalidationOptions().getTraceIDProvider() instanceof NoTraceIDProvider, getTestMessage(testName, "DefaultInvalidationOptions shall use NoTraceIDProvider"));
  }

  @Test
  public void canParsePossibleSKDefWithTraceIdProvider() {
    // Possible Sk def which contains"name" and TraceIdProvider in its OperationOptions
    String possibleSKDefWithTraceIdProvider =
        "options={storageType=FILE,consistencyProtocol=LOOSE," + "versionMode" + "=SYSTEM_TIME_NANOS," +
            "revisionMode=NO_REVISIONS," + "defaultPutOptions={opTimeoutController" + "=<OpSizeBasedTimeoutController" +
            ">{maxAttempts=4,constantTime_ms=300000,itemTime_ms=305," + "nonKeyedOpMaxRelTimeout_ms=1500000," +
            "exclusionChangeRetryInterval_ms=5000}," + "traceIDProvider" + "=<NoTraceIDProvider>{}," + "compression" +
            "=NONE,checksumType=MD5,checksumCompressedValues=false,version=0," + "requiredPreviousVersion=0," +
            "lockSeconds=0,fragmentationThreshold=10485760,}," + "defaultInvalidationOptions" +
            "={opTimeoutController=<OpSizeBasedTimeoutController>{maxAttempts=4,constantTime_ms=300000," +
            "itemTime_ms=305," + "nonKeyedOpMaxRelTimeout_ms=1500000,exclusionChangeRetryInterval_ms=5000}," +
            "traceIDProvider" + "=<NoTraceIDProvider>{}," + "compression=NONE,checksumType=SYSTEM," +
            "checksumCompressedValues=false,version=0," + "requiredPreviousVersion=0,lockSeconds=0," +
            "fragmentationThreshold=10485760,}," + "defaultGetOptions" + "={opTimeoutController" +
        "=<OpSizeBasedTimeoutController>{maxAttempts=4,constantTime_ms=300000,itemTime_ms=305," +
        "nonKeyedOpMaxRelTimeout_ms=1500000,exclusionChangeRetryInterval_ms=5000}," + "traceIDProvider" +
            "=<NoTraceIDProvider>{}," + "retrievalType=VALUE,waitMode=GET," +
            "versionConstraint={min=-9223372036854775808," + "max=9223372036854775807,mode=GREATEST," +
            "maxCreationTime=9223372036854775807},nonExistenceResponse=NULL_VALUE," + "verifyChecksums=true," +
            "returnInvalidations=false,forwardingMode=ALL,updateSecondariesOnMiss=false,}," + "defaultWaitOptions" +
            "={opTimeoutController=<WaitForTimeoutController>{internalRetryIntervalSeconds=20," +
            "internalExclusionChangeRetryIntervalSeconds=2}," + "traceIDProvider=<NoTraceIDProvider>{}," +
            "retrievalType=VALUE,waitMode=WAIT_FOR," + "versionConstraint={min=-9223372036854775808," +
            "max=9223372036854775807,mode=GREATEST,maxCreationTime=9223372036854775807}," +
            "nonExistenceResponse=NULL_VALUE,verifyChecksums=true,returnInvalidations=false," + "forwardingMode" +
            "=FORWARD,updateSecondariesOnMiss=false,timeoutSeconds=2147483647,threshold=100," +
            "timeoutResponse=EXCEPTION},secondarySyncIntervalSeconds=1996,segmentSize=4096,maxValueSize=1996," +
            "allowLinks=false," + "valueRetentionPolicy=<LRURetentionPolicy>{capacityBytes=8388608,}," +
            "namespaceServerSideCode={url=," + "putTrigger=com.ms.silverking.cloud.dht.daemon.storage.serverside" +
            ".LRUTrigger,retrieveTrigger=com.ms.silverking.cloud.dht.daemon.storage.serverside.LRUTrigger}}," +
            "parent=SteelBallRun," + "minVersion=1," + "name=" + dummyNsName;

    NamespaceProperties parsed = NamespaceProperties.parse(possibleSKDefWithTraceIdProvider);
    String testName = "canParsePossibleSKDefWithTraceIdProvider";
    assertFalse(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertEquals(dummyNsName,
        parsed.getName(), getTestMessage(testName, "reflection.getName() shall be correctly reflected"));

    testDefaultOperationOptionsTraceIDProvider(testName, parsed);
  }

  @Test
  public void canParsePossibleSKDef() {
    // Possible Sk def which contains"name"
    String possibleSKDef = "options={storageType=FILE,consistencyProtocol=LOOSE,versionMode=SYSTEM_TIME_NANOS," +
        "revisionMode=NO_REVISIONS," + "defaultPutOptions={opTimeoutController=<OpSizeBasedTimeoutController" +
        ">{maxAttempts=4,constantTime_ms=300000,itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000," +
        "exclusionChangeRetryInterval_ms=5000}," + "compression=NONE,checksumType=MD5,checksumCompressedValues=false,"
        + "version=0,requiredPreviousVersion=0,lockSeconds=0,fragmentationThreshold=10485760,}," +
        "defaultInvalidationOptions={opTimeoutController=<OpSizeBasedTimeoutController>{maxAttempts=4," +
        "constantTime_ms=300000,itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000," +
        "exclusionChangeRetryInterval_ms=5000}," + "compression=NONE,checksumType=SYSTEM," +
        "checksumCompressedValues=false,version=0,requiredPreviousVersion=0,lockSeconds=0," + "fragmentationThreshold"
        + "=10485760,}," + "defaultGetOptions={opTimeoutController=<OpSizeBasedTimeoutController" + ">{maxAttempts=4,"
        + "constantTime_ms=300000,itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000," +
        "exclusionChangeRetryInterval_ms=5000}," + "retrievalType=VALUE,waitMode=GET," + "versionConstraint={min" +
        "=-9223372036854775808,max=9223372036854775807,mode=GREATEST," + "maxCreationTime=9223372036854775807}," +
        "nonExistenceResponse=NULL_VALUE," + "verifyChecksums=true," + "returnInvalidations=false,forwardingMode=ALL,"
        + "updateSecondariesOnMiss=false,}," + "defaultWaitOptions" + "={opTimeoutController" +
        "=<WaitForTimeoutController" + ">{internalRetryIntervalSeconds=20," +
        "internalExclusionChangeRetryIntervalSeconds=2},retrievalType=VALUE," + "waitMode=WAIT_FOR," +
        "versionConstraint" + "={min=-9223372036854775808,max=9223372036854775807," + "mode=GREATEST," +
        "maxCreationTime=9223372036854775807}," + "nonExistenceResponse=NULL_VALUE," + "verifyChecksums=true," +
        "returnInvalidations=false," + "forwardingMode=FORWARD," + "updateSecondariesOnMiss" + "=false,timeoutSeconds" +
        "=2147483647,threshold=100,timeoutResponse=EXCEPTION}," + "secondarySyncIntervalSeconds" + "=1996,segmentSize" +
        "=4096,maxValueSize=1996,allowLinks=false," + "valueRetentionPolicy=<LRURetentionPolicy" + ">{capacityBytes" +
        "=8388608,}," + "namespaceServerSideCode={url=," + "putTrigger=com.ms.silverking" + ".cloud.dht.daemon" +
        ".storage.serverside" + ".LRUTrigger,retrieveTrigger=com.ms.silverking.cloud.dht.daemon" + ".storage" +
        ".serverside.LRUTrigger}}," + "parent" + "=SteelBallRun," + "minVersion=1," + "name=" + dummyNsName;

    NamespaceProperties parsed = NamespaceProperties.parse(possibleSKDef);
    String testName = "canParsePossibleSkDef";
    assertFalse(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertEquals(dummyNsName,
        parsed.getName(), getTestMessage(testName, "reflection.getName() shall be correctly reflected"));

    testDefaultOperationOptionsTraceIDProvider(testName, parsed);
  }

  @Test
  public void canParseLegacySKDef() {
    // Legacy Sk def which contains no "name" and no "creationTime" (same format as current production env)
    String legacySKDef = "options={storageType=FILE,consistencyProtocol=LOOSE,versionMode=SYSTEM_TIME_NANOS," +
        "revisionMode=NO_REVISIONS," + "defaultPutOptions={opTimeoutController=<OpSizeBasedTimeoutController" +
        ">{maxAttempts=4,constantTime_ms=300000,itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000," +
        "exclusionChangeRetryInterval_ms=5000}," + "compression=NONE,checksumType=MD5,checksumCompressedValues=false,"
        + "version=0,requiredPreviousVersion=0,lockSeconds=0,fragmentationThreshold=10485760,}," +
        "defaultInvalidationOptions={opTimeoutController=<OpSizeBasedTimeoutController>{maxAttempts=4," +
        "constantTime_ms=300000,itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000," +
        "exclusionChangeRetryInterval_ms=5000}," + "compression=NONE,checksumType=SYSTEM," +
        "checksumCompressedValues=false,version=0,requiredPreviousVersion=0,lockSeconds=0," + "fragmentationThreshold"
        + "=10485760,}," + "defaultGetOptions={opTimeoutController=<OpSizeBasedTimeoutController" + ">{maxAttempts=4,"
        + "constantTime_ms=300000,itemTime_ms=305,nonKeyedOpMaxRelTimeout_ms=1500000," +
        "exclusionChangeRetryInterval_ms=5000}," + "retrievalType=VALUE,waitMode=GET," + "versionConstraint={min" +
        "=-9223372036854775808,max=9223372036854775807,mode=GREATEST," + "maxCreationTime=9223372036854775807}," +
        "nonExistenceResponse=NULL_VALUE," + "verifyChecksums=true," + "returnInvalidations=false,forwardingMode=ALL,"
        + "updateSecondariesOnMiss=false,}," + "defaultWaitOptions" + "={opTimeoutController" +
        "=<WaitForTimeoutController" + ">{internalRetryIntervalSeconds=20," +
        "internalExclusionChangeRetryIntervalSeconds=2},retrievalType=VALUE," + "waitMode=WAIT_FOR," +
        "versionConstraint" + "={min=-9223372036854775808,max=9223372036854775807," + "mode=GREATEST," +
        "maxCreationTime=9223372036854775807}," + "nonExistenceResponse=NULL_VALUE," + "verifyChecksums=true," +
        "returnInvalidations=false," + "forwardingMode=FORWARD," + "updateSecondariesOnMiss" + "=false,timeoutSeconds" +
        "=2147483647,threshold=100,timeoutResponse=EXCEPTION}," + "secondarySyncIntervalSeconds" + "=1996,segmentSize" +
        "=4096,maxValueSize=1996,allowLinks=false," + "valueRetentionPolicy=<LRURetentionPolicy" + ">{capacityBytes" +
        "=8388608,}," + "namespaceServerSideCode={url=," + "putTrigger=com.ms.silverking" + ".cloud.dht.daemon" +
        ".storage.serverside" + ".LRUTrigger,retrieveTrigger=com.ms.silverking.cloud.dht.daemon" + ".storage" +
        ".serverside.LRUTrigger}}," + "parent" + "=SteelBallRun," + "minVersion=1";

    NamespaceProperties parsed = NamespaceProperties.parse(legacySKDef);
    String testName = "canParseLegacySkDef";
    assertFalse(
        parsed.hasCreationTime(), getTestMessage(testName, "reflection.hasCreationTime() shall be false for partial initialization"));
    assertFalse(
        parsed.hasName(), getTestMessage(testName, "reflection.hasName() shall be false for partial initialization"));

    testDefaultOperationOptionsTraceIDProvider(testName, parsed);
  }

  @Test
  public void backwardCompatibilityFunctionalityFull() {
    NamespaceProperties initialized = new NamespaceProperties(dummyNsOptions, dummyNsName, dummyParentName,
        dummyMinVersion, dummyCreationTime);
    String advancedDef = initialized.toString();
    String legacyDef = initialized.toLegacySKDef();

    String[] doggyFields = { "creationTime=", "name=" };

    NamespaceProperties advancedParsed = NamespaceProperties.parse(advancedDef);
    NamespaceProperties legacyParsed = NamespaceProperties.parse(legacyDef);

    String testName = "backwardCompatibility";
    assertNotEquals(advancedDef, legacyDef, getTestMessage(testName, "legacyDef shall drop new fields"));
    for (String doggyField : doggyFields) {
      assertFalse(
          legacyDef.contains(doggyField), getTestMessage(testName, "legacyDef shall drop [" + doggyField + "] field"));
    }
    for (String doggyField : doggyFields) {
      assertTrue(
          advancedDef.contains(doggyField), getTestMessage(testName, "advancedDef shall keep [" + doggyField + "] field"));
    }

    assertFalse(
        legacyParsed.hasCreationTime(), getTestMessage(testName, "legacyDef shall drop [creationTime] field and successfully parsed"));
    assertFalse(
        legacyParsed.hasName(), getTestMessage(testName, "legacyDef shall drop [name] field and successfully parsed"));
    assertTrue(
        legacyParsed.getParent().equals(advancedParsed.getParent()), getTestMessage(testName, "Non-doggy fields shall be same between advanced and legacy"));
    assertTrue(
        legacyParsed.getOptions().equals(advancedParsed.getOptions()), getTestMessage(testName, "Non-doggy fields shall be same between advanced and legacy"));
    assertTrue(
        legacyParsed.getMinVersion() == advancedParsed.getMinVersion(), getTestMessage(testName, "Non-doggy fields shall be same between advanced and legacy"));

    testDefaultOperationOptionsTraceIDProvider(testName, advancedParsed);
    testDefaultOperationOptionsTraceIDProvider(testName, legacyParsed);
  }

  @Test
  public void backwardCompatibilityFunctionalityNoName() {
    NamespaceProperties initialized = new NamespaceProperties(dummyNsOptions, dummyParentName,
        dummyMinVersion).creationTime(dummyCreationTime);
    String advancedDef = initialized.toString();
    String legacyDef = initialized.toLegacySKDef();

    String[] doggyFields = { "creationTime=" };

    NamespaceProperties advancedParsed = NamespaceProperties.parse(advancedDef);
    NamespaceProperties legacyParsed = NamespaceProperties.parse(legacyDef);

    String testName = "backwardCompatibility";
    assertNotEquals(advancedDef, legacyDef, getTestMessage(testName, "legacyDef shall drop new fields"));
    for (String doggyField : doggyFields) {
      assertFalse(
          legacyDef.contains(doggyField), getTestMessage(testName, "legacyDef shall drop [" + doggyField + "] field"));
    }
    for (String doggyField : doggyFields) {
      assertTrue(
          advancedDef.contains(doggyField), getTestMessage(testName, "advancedDef shall keep [" + doggyField + "] field"));
    }

    assertFalse(
        legacyParsed.hasCreationTime(), getTestMessage(testName, "legacyDef shall drop [creationTime] field and successfully parsed"));
    assertFalse(
        legacyParsed.hasName(), getTestMessage(testName, "legacyDef shall drop [name] field and successfully parsed"));
    assertTrue(
        legacyParsed.getParent().equals(advancedParsed.getParent()), getTestMessage(testName, "Non-doggy fields shall be same between advanced and legacy"));
    assertTrue(
        legacyParsed.getOptions().equals(advancedParsed.getOptions()), getTestMessage(testName, "Non-doggy fields shall be same between advanced and legacy"));
    assertTrue(
        legacyParsed.getMinVersion() == advancedParsed.getMinVersion(), getTestMessage(testName, "Non-doggy fields shall be same between advanced and legacy"));

    testDefaultOperationOptionsTraceIDProvider(testName, advancedParsed);
    testDefaultOperationOptionsTraceIDProvider(testName, legacyParsed);
  }
}
