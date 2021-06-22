package com.ms.silverking.cloud.dht.daemon.storage;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.logging.Level;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ms.silverking.cloud.dht.NamespaceOptions;
import com.ms.silverking.cloud.dht.NamespaceServerSideCode;
import com.ms.silverking.cloud.dht.NamespaceVersionMode;
import com.ms.silverking.cloud.dht.NonExistenceResponse;
import com.ms.silverking.cloud.dht.PutOptions;
import com.ms.silverking.cloud.dht.RetrievalOptions;
import com.ms.silverking.cloud.dht.RetrievalType;
import com.ms.silverking.cloud.dht.RevisionMode;
import com.ms.silverking.cloud.dht.StorageType;
import com.ms.silverking.cloud.dht.VersionConstraint;
import com.ms.silverking.cloud.dht.WaitMode;
import com.ms.silverking.cloud.dht.client.ChecksumType;
import com.ms.silverking.cloud.dht.client.Compression;
import com.ms.silverking.cloud.dht.collection.DHTKeyIntEntry;
import com.ms.silverking.cloud.dht.collection.IntArrayDHTKeyCuckoo;
import com.ms.silverking.cloud.dht.common.CCSSUtil;
import com.ms.silverking.cloud.dht.common.CorruptValueException;
import com.ms.silverking.cloud.dht.common.DHTConstants;
import com.ms.silverking.cloud.dht.common.DHTKey;
import com.ms.silverking.cloud.dht.common.DHTKeyComparator;
import com.ms.silverking.cloud.dht.common.InternalRetrievalOptions;
import com.ms.silverking.cloud.dht.common.JVMUtil;
import com.ms.silverking.cloud.dht.common.KeyUtil;
import com.ms.silverking.cloud.dht.common.MetaDataConstants;
import com.ms.silverking.cloud.dht.common.MetaDataUtil;
import com.ms.silverking.cloud.dht.common.NamespaceProperties;
import com.ms.silverking.cloud.dht.common.NamespaceUtil;
import com.ms.silverking.cloud.dht.common.OpResult;
import com.ms.silverking.cloud.dht.common.OptionsHelper;
import com.ms.silverking.cloud.dht.common.SegmentIndexLocation;
import com.ms.silverking.cloud.dht.common.SystemTimeUtil;
import com.ms.silverking.cloud.dht.common.ValueUtil;
import com.ms.silverking.cloud.dht.daemon.ActiveProxyRetrieval;
import com.ms.silverking.cloud.dht.daemon.KeyToReplicaResolver;
import com.ms.silverking.cloud.dht.daemon.NodeRingMaster2;
import com.ms.silverking.cloud.dht.daemon.PeerHealthIssue;
import com.ms.silverking.cloud.dht.daemon.PeerHealthMonitor;
import com.ms.silverking.cloud.dht.daemon.Waiter;
import com.ms.silverking.cloud.dht.daemon.storage.FileSegment.SegmentPrereadMode;
import com.ms.silverking.cloud.dht.daemon.storage.convergence.ActiveRegionSync;
import com.ms.silverking.cloud.dht.daemon.storage.convergence.ChecksumNode;
import com.ms.silverking.cloud.dht.daemon.storage.convergence.ChecksumTreeRequest;
import com.ms.silverking.cloud.dht.daemon.storage.convergence.ChecksumTreeServer;
import com.ms.silverking.cloud.dht.daemon.storage.convergence.ConvergencePoint;
import com.ms.silverking.cloud.dht.daemon.storage.fsm.FileSegmentStorageFormat;
import com.ms.silverking.cloud.dht.daemon.storage.management.ManagedNamespaceStore;
import com.ms.silverking.cloud.dht.daemon.storage.management.PurgeResult;
import com.ms.silverking.cloud.dht.daemon.storage.protocol.PutCommunicator;
import com.ms.silverking.cloud.dht.daemon.storage.protocol.StorageProtocolUtil;
import com.ms.silverking.cloud.dht.daemon.storage.retention.EmptyValueRetentionState;
import com.ms.silverking.cloud.dht.daemon.storage.retention.InternalPurgeKeyRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.InternalPurgeKeyRetentionState;
import com.ms.silverking.cloud.dht.daemon.storage.retention.KeyLevelValueRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.NeverRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.PermanentRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.SegmentLevelValueRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.ValueRetentionPolicyImpl;
import com.ms.silverking.cloud.dht.daemon.storage.retention.ValueRetentionState;
import com.ms.silverking.cloud.dht.meta.LinkCreationListener;
import com.ms.silverking.cloud.dht.meta.LinkCreationWatcher;
import com.ms.silverking.cloud.dht.net.MessageGroup;
import com.ms.silverking.cloud.dht.net.MessageGroupBase;
import com.ms.silverking.cloud.dht.net.MessageGroupConnection;
import com.ms.silverking.cloud.dht.net.MessageGroupKeyOrdinalEntry;
import com.ms.silverking.cloud.dht.net.MessageGroupRetrievalResponseEntry;
import com.ms.silverking.cloud.dht.serverside.PutTrigger;
import com.ms.silverking.cloud.dht.serverside.RetrieveCallback;
import com.ms.silverking.cloud.dht.serverside.RetrieveTrigger;
import com.ms.silverking.cloud.dht.serverside.SSNamespaceStore;
import com.ms.silverking.cloud.dht.serverside.SSRetrievalOptions;
import com.ms.silverking.cloud.dht.serverside.SSStorageParametersAndRequirements;
import com.ms.silverking.cloud.dht.trace.TracerFactory;
import com.ms.silverking.cloud.ring.RingRegion;
import com.ms.silverking.cloud.storagepolicy.StoragePolicy;
import com.ms.silverking.cloud.zookeeper.SilverKingZooKeeperClient;
import com.ms.silverking.collection.CollectionUtil;
import com.ms.silverking.collection.HashedSetMap;
import com.ms.silverking.collection.MapUtil;
import com.ms.silverking.collection.Pair;
import com.ms.silverking.collection.Quintuple;
import com.ms.silverking.collection.SKImmutableList;
import com.ms.silverking.collection.Triple;
import com.ms.silverking.collection.cuckoo.IntCuckooConstants;
import com.ms.silverking.collection.cuckoo.TableFullException;
import com.ms.silverking.collection.cuckoo.WritableCuckooConfig;
import com.ms.silverking.id.UUIDBase;
import com.ms.silverking.io.FileUtil;
import com.ms.silverking.io.util.BufferUtil;
import com.ms.silverking.log.Log;
import com.ms.silverking.net.IPAndPort;
import com.ms.silverking.text.StringUtil;
import com.ms.silverking.thread.ThreadUtil;
import com.ms.silverking.time.SimpleStopwatch;
import com.ms.silverking.time.Stopwatch;
import com.ms.silverking.time.SystemTimeSource;
import com.ms.silverking.util.ArrayUtil;
import com.ms.silverking.util.PropertiesHelper;
import com.ms.silverking.util.jvm.Finalization;

public class NamespaceStore implements SSNamespaceStore, ManagedNamespaceStore {
  private final long ns;
  private NamespaceStore parent;
  private final File nsDir;
  private final File ssDir;
  private final File ssTempDir;
  private final NamespaceProperties nsProperties;
  private final NamespaceOptions nsOptions;
  private final boolean verifyStorageState;
  private final MessageGroupBase mgBase;
  private final NodeRingMaster2 ringMaster;
  private final ChecksumTreeServer checksumTreeServer;
  private volatile WritableSegmentBase headSegment;
  //private final Lock headCreationLock;
  // private final ConcurrentMap<DHTKey,Integer> valueSegments;
  // maps DHTKeys to the segment that stores an existing entry for that key
  // if more than one entry exists, it will contain the index of the
  // OffsetList for the DHTKey
  /**
   * Maps keys to:
   * a) the segment where the value is stored (for single value storage)
   * b) the list of segments where the value is stored
   */
  private IntArrayDHTKeyCuckoo valueSegments;
  private final AtomicInteger nextSegmentID;
  private final OffsetListStore offsetListStore;
  private final ReadWriteLock metaRWLock;
  private final Lock metaReadLock;
  private final Lock metaWriteLock;
  private final ReentrantReadWriteLock rwLock;
  private final ReadLock readLock;
  private final WriteLock writeLock;
  private final ReentrantLock reapLock;
  private long minVersion;
  private long curSnapshot;
  private final ConcurrentMap<UUIDBase, ActiveProxyRetrieval> activeRetrievals;
  private final ConcurrentMap<DHTKey, Set<PendingWaitFor>> pendingWaitFors;
  private final Cache<Integer, FileSegment> fileSegmentCache;
  private final Map<Integer, RAMSegment> ramSegments;
  private final NamespaceMetrics nsMetrics;
  private long lastConvergenceVersion;
  private long lastConvergenceTotalKeys;
  protected final SystemTimeSource systemTimeSource;
  private final Set<Integer> deletedSegments;
  private final PutTrigger putTrigger;
  private final RetrieveTrigger retrieveTrigger;
  private final ReapPolicy reapPolicy;
  private final ReapPolicyState reapPolicyState;
  private int deletionsSinceFinalization;
  private final Finalization finalization;
  private final FileSegmentCompactor fileSegmentCompactor;
  private SVPMapper svpMapper;
  private static SVPMapper.Mode svpMapperMode = (SVPMapper.Mode) PropertiesHelper.systemHelper.getEnum(
      NamespaceStore.class.getPackage().getName() + ".SVPMapper.Mode", SVPMapper.Mode.FileBackedMap);
  private static final int defaultSVPMapThreshold = 16383;
  private static int svpMapThreshold = PropertiesHelper.systemHelper.getInt(
      NamespaceStore.class.getPackage().getName() + ".SVPMapThreshold", defaultSVPMapThreshold);

  private final ConcurrentMap<UUIDBase, ActiveRegionSync> activeRegionSyncs;
  private final ValueRetentionPolicyImpl vrp;

  private static final byte[] emptyUserData = new byte[0];

  private static final boolean debug = PropertiesHelper.systemHelper.getBoolean(
      NamespaceStore.class.getCanonicalName() + ".debug", false);
  private static final boolean debugConvergence = false || debug;
  private static final boolean debugParent = true || debug;
  private static final boolean debugVersion = false || debug;
  private static final boolean debugSegments = false || debug;
  private static final boolean debugWaitFor = false || debug;
  private static final boolean debugReap = false || debug;

  private static final int vsNumSubTables = 4;
  private static final int vsEntriesPerBucket = 4;
  private static final int vsTotalEntries = vsNumSubTables * vsEntriesPerBucket;
  private static final int vsCuckooLimit = 32;
  private static final WritableCuckooConfig valueSegmentsConfig = new WritableCuckooConfig(vsTotalEntries,
      vsNumSubTables, vsEntriesPerBucket, vsCuckooLimit);
  private static final InternalRetrievalOptions oldestVersionOptions = new InternalRetrievalOptions(
      OptionsHelper.newRetrievalOptions(RetrievalType.META_DATA, WaitMode.GET, VersionConstraint.least));
  private static final InternalRetrievalOptions newestVersionOptions = new InternalRetrievalOptions(
      OptionsHelper.newRetrievalOptions(RetrievalType.META_DATA, WaitMode.GET, VersionConstraint.greatest));
  private static final int fileSegmentCacheCapacity = StoreConfiguration.fileSegmentCacheCapacity;
  private static final long noSuchVersion = MetaDataConstants.noSuchVersion;

  private static final boolean testStorageCorruption = false;
  private static final double storageCorruptionProbability = 0.3;

  private static final int fileSegmentAttempts = 10;
  private static final int fileSegmentAttemptDelayMillis = 1 * 1000;

  private static final ByteBuffer valueStorageStateInvalidForRead = ByteBuffer.allocate(0);

  // FUTURE - allow for a richer compaction policy
  private static final double defaultCompactionThreshold;
  private static final double compactionThreshold = 0.1;

  private static final int maxFailedStores = 1000000;

  private static final boolean immediateDeletion = true;

  private static final long nsIdleIntervalMillis = 4 * 60 * 1000;
  private static final long minFullReapIntervalMillis = 4 * 60 * 60 * 1000;

  private static final long maxInvalidSSLockSeconds = 10;

  public enum DirCreationMode {
    CreateNSDir, DoNotCreateNSDir, CreateNSDirNoPropertiesFileBootstrap
  }

  private enum FileSegmentLoadMode {ReadWrite, ReadOnly, ReadIndexOnly}

  private enum VersionCheckResult {Invalid, Valid, Equal, Valid_New_Key}

  private enum LockCheckResult {Unlocked, Locked, Ignored}

  private static final int VERSION_INDEX = 0;
  private static final int STORAGE_TIME_INDEX = 1;

  private static final SegmentIndexLocation segmentIndexLocation;
  private static final int nsPrereadGB;
  private static final SegmentPrereadMode readSegmentPrereadMode = SegmentPrereadMode.NoPreread;
  private static final SegmentPrereadMode updateSegmentPrereadMode = SegmentPrereadMode.NoPreread;
  private static final boolean forceDataSegmentLoadOnReap;

  public static final boolean enablePendingPuts;

  private static final int minFinalizationIntervalMillis;
  private static final long maxUnfinalizedDeletedBytes;

  private static final boolean verboseReapLogInfo;

  static {
    segmentIndexLocation = SegmentIndexLocation.valueOf(
        PropertiesHelper.systemHelper.getString(DHTConstants.segmentIndexLocationProperty,
            DHTConstants.defaultSegmentIndexLocation.toString()));
    Log.warningf("segmentIndexLocation: %s", segmentIndexLocation);
    nsPrereadGB = PropertiesHelper.systemHelper.getInt(DHTConstants.nsPrereadGBProperty,
        DHTConstants.defaultNSPrereadGB);
    Log.warningf("nsPrereadGB: %s", nsPrereadGB);
    minFinalizationIntervalMillis = PropertiesHelper.systemHelper.getInt(
        DHTConstants.minFinalizationIntervalMillisProperty, DHTConstants.defaultMinFinalizationIntervalMillis);
    Log.warningf("minFinalizationIntervalMillis: %d", minFinalizationIntervalMillis);
    enablePendingPuts = PropertiesHelper.systemHelper.getBoolean(DHTConstants.enablePendingPutsProperty,
        DHTConstants.defaultEnablePendingPuts);
    Log.warningf("enablePendingPuts: %s", enablePendingPuts);
    maxUnfinalizedDeletedBytes = PropertiesHelper.systemHelper.getLong(DHTConstants.maxUnfinalizedDeletedBytesProperty,
        DHTConstants.defaultMaxUnfinalizedDeletedBytes);
    Log.warningf("maxUnfinalizedDeletedBytes: %d", maxUnfinalizedDeletedBytes);
    Log.warningf("fileSegmentCacheCapacity: %d", fileSegmentCacheCapacity);

    defaultCompactionThreshold = PropertiesHelper.systemHelper.getDouble(
        DHTConstants.defaultCompactionThresholdProperty, 0.1);
    // FUTURE - threshold disabled by default for now; alter implementation to correctly support
    Log.warningf("defaultCompactionThreshold: %f", defaultCompactionThreshold);

    forceDataSegmentLoadOnReap = PropertiesHelper.systemHelper.getBoolean(
        DHTConstants.forceDataSegmentLoadOnReapProperty, false);
    Log.warningf("forceDataSegmentLoadOnReap: %s", forceDataSegmentLoadOnReap);

    Preconditions.checkState(minFinalizationIntervalMillis > 0, "minFinalizationIntervalMillis must be non-negative");
    Preconditions.checkState(maxUnfinalizedDeletedBytes > 0, "maxUnfinalizedDeletedBytes must be non-negative");

    verboseReapLogInfo = PropertiesHelper.systemHelper.getBoolean(DHTConstants.verboseReapLogInfoProperty, false);
    Log.warningf("verboseReapLogInfo: %s", verboseReapLogInfo);
  }

  // TODO: remove these two throw-way methods below when we figure out a way to manage the binds between System
  //  property and static variables
  protected static int getMinFinalizationIntervalMillis() {
    return minFinalizationIntervalMillis;
  }

  protected static long getMaxUnfinalizedDeletedBytes() {
    return maxUnfinalizedDeletedBytes;
  }

  private static PeerHealthMonitor peerHealthMonitor;

  public static void setPeerHealthMonitor(PeerHealthMonitor _peerHealthMonitor) {
    peerHealthMonitor = _peerHealthMonitor;
  }

  /////////////////////////////////
    /*
    private static final int    ssWorkerPoolTargetSize = 2;
    private static final int    ssWorkerPoolMaxSize = 2;

    static LWTPool ssWorkerPool = LWTPoolProvider.createPool(LWTPoolParameters.create("NamespaceStorePool")
    .targetSize(ssWorkerPoolTargetSize).maxSize(ssWorkerPoolMaxSize));

    class SSWorker extends BaseWorker<Object> {
        SSWorker() {
            super(ssWorkerPool, true, 0);
        }

        @Override
        public void doWork(Object[] o) {
        }

        @
    }

    class KeyedOpResultRelay implements KeyedOpResultListener {
        private final Map<DHTKey, KeyedOpResultListener>    listeners;

        KeyedOpResultRelay() {

        }

        @Override
        public void sendResult(DHTKey key, OpResult result) {
            KeyedOpResultListener    listener;

            listener = listeners.get(key);
            if (listener != null) {
                listener.sendResult(key, result);
            } else {
                Log.warningf("KeyedOpResultRelay can't find listener for %s", key);
            }
        }
    }

    class PutWork {
        final List<StorageValueAndParameters>    values;
        final byte[] userData;
        final KeyedOpResultListener resultListener;

        PutWork(List<StorageValueAndParameters> values, byte[] userData, KeyedOpResultListener resultListener) {
            this.values = values;
            this.userData = userData;
            this.resultListener = resultListener;
        }
    }
    */

  /////////////////////////////////

  /**
   * Full constructor is for testing only. FileSegmentCompactor is not normally specified.
   * // FUTURE - consider removal
   */
  public NamespaceStore(long ns, File nsDir, DirCreationMode dirCreationMode, NamespaceProperties nsProperties,
      NamespaceStore parent, MessageGroupBase mgBase, NodeRingMaster2 ringMaster, boolean isRecovery,
      ConcurrentMap<UUIDBase, ActiveProxyRetrieval> activeRetrievals, ReapPolicy reapPolicy, Finalization finalization,
      FileSegmentCompactor fileSegmentCompactor) {
    this.ns = ns;
    this.nsDir = nsDir;
    ssDir = new File(nsDir, DHTConstants.ssSubDirName);
    ssTempDir = new File(nsDir, DHTConstants.ssTempSubDirName);
    activeRegionSyncs = new ConcurrentHashMap<>();
    this.nsOptions = nsProperties.getOptions();
    verifyStorageState = StorageProtocolUtil.requiresStorageStateVerification(nsOptions.getConsistencyProtocol());
    this.nsProperties = nsProperties;
    this.parent = parent;
    this.mgBase = mgBase;
    this.ringMaster = ringMaster;
    checksumTreeServer = new ChecksumTreeServer(this,
        mgBase != null ? mgBase.getAbsMillisTimeSource() : SystemTimeUtil.timerDrivenTimeSource);
    this.reapPolicy = reapPolicy;
    reapPolicyState = reapPolicy.createInitialState();
    switch (dirCreationMode) {
    case CreateNSDirNoPropertiesFileBootstrap:
      if (!nsDir.mkdirs()) {
        if (!nsDir.exists()) {
          throw new RuntimeException("Unable to make nsDir");
        } else {
          Log.warning("nsDir creation ignored due to dir already exists");
        }
      }
      try {
        // Still force write a "properties" file to support the mode switch between ZK and NSP mode
        // (This file will not be directly used in ZK mode)
        NamespacePropertiesIO.rewrite(nsDir, nsProperties);
      } catch (IOException ioe) {
        peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
        throw new RuntimeException(ioe);
      }
      break;
    case CreateNSDir:
      if (!nsDir.mkdirs()) {
        if (!nsDir.exists()) {
          throw new RuntimeException("Unable to make nsDir");
        } else {
          Log.warning("nsDir creation ignored due to dir already exists");
        }
      }
      try {
        NamespacePropertiesIO.write(nsDir, nsProperties);
      } catch (IOException ioe) {
        peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
        throw new RuntimeException(ioe);
      }
      break;
    case DoNotCreateNSDir:
      break;
    default:
      throw new RuntimeException("panic");
    }
    nextSegmentID = new AtomicInteger(0);
    switch (nsOptions.getStorageType()) {
    case RAM:
      ramSegments = new ConcurrentHashMap<>(); // FUTURE - consider a more efficient map
      fileSegmentCache = null;
      break;
    case FILE_SYNC:
    case FILE:
      ramSegments = null;
      fileSegmentCache = CacheBuilder.newBuilder().maximumSize(fileSegmentCacheCapacity).build();
      break;
    default:
      throw new RuntimeException("Panic");
    }
    if (fileSegmentCompactor != null) {
      fileSegmentCompactor.setFileSegmentCache(fileSegmentCache);
    }
    if (!isRecovery) {
      createInitialHeadSegment();
    }
    //headCreationLock = new ReentrantLock();
    // valueSegments = new ConcurrentHashMap<>();
    valueSegments = new IntArrayDHTKeyCuckoo(valueSegmentsConfig);
    offsetListStore = new RAMOffsetListStore(nsOptions);
    metaRWLock = new ReentrantReadWriteLock();
    metaReadLock = metaRWLock.readLock();
    metaWriteLock = metaRWLock.writeLock();
    rwLock = new ReentrantReadWriteLock();
    readLock = rwLock.readLock();
    writeLock = rwLock.writeLock();
    reapLock = new ReentrantLock();
    pendingWaitFors = new ConcurrentSkipListMap<>(DHTKeyComparator.dhtKeyComparator);
    minVersion = nsProperties.getMinVersion();
    if (minVersion > Long.MIN_VALUE) {
      curSnapshot = minVersion - 1;
      // FUTURE - probably eliminate curSnapshot
    } else {
      curSnapshot = Long.MIN_VALUE;
    }
    this.activeRetrievals = activeRetrievals;
    systemTimeSource = SystemTimeUtil.skSystemTimeSource;
    nsMetrics = new NamespaceMetrics();
    deletedSegments = new HashSet<>();

    Pair<PutTrigger, RetrieveTrigger> triggers;
    triggers = createTriggers();
    putTrigger = triggers.getV1();
    retrieveTrigger = triggers.getV2();
    if (enablePendingPuts && putTrigger != null && putTrigger.supportsMerge()) {
      try {
        switch (svpMapperMode) {
        case NoMap:
          svpMapper = null;
          break;
        case PrivateMap:
          svpMapper = SVPMapper.newPrivateModeMapper(nsOptions.getMaxValueSize());
          break;
        case FileBackedMap:
          svpMapper = SVPMapper.newFileBackedMapper(ssTempDir);
          break;
        default:
          throw new RuntimeException("panic");
        }
      } catch (IOException ioe) {
        Log.logErrorWarning(ioe);
        svpMapper = null;
      }
    } else {
      svpMapper = null;
    }
    this.finalization = finalization;
    this.fileSegmentCompactor = fileSegmentCompactor;
    vrp = ValueRetentionPolicyImpl.fromPolicy(nsOptions.getValueRetentionPolicy(), this);
    //we need to initialize trigger in the end as trigger init might depend on some property of NamespaceStore
    if (!isRecovery) {
      initializeTriggers();
    }
  }

  public NamespaceStore(long ns, File nsDir, DirCreationMode dirCreationMode, NamespaceProperties nsProperties,
      NamespaceStore parent, MessageGroupBase mgBase, NodeRingMaster2 ringMaster, boolean isRecovery,
      ConcurrentMap<UUIDBase, ActiveProxyRetrieval> activeRetrievals, ReapPolicy reapPolicy,
      Finalization finalization) {
    this(ns, nsDir, dirCreationMode, nsProperties, null, mgBase, ringMaster, isRecovery, activeRetrievals, reapPolicy,
        finalization, new NamespaceFileSegmentCompactor(nsDir, nsProperties.getOptions()));
  }

  public NamespaceStore(long ns, File nsDir, DirCreationMode dirCreationMode, NamespaceProperties nsProperties,
      NamespaceStore parent, MessageGroupBase mgBase, NodeRingMaster2 ringMaster, boolean isRecovery,
      ConcurrentMap<UUIDBase, ActiveProxyRetrieval> activeRetrievals, ReapPolicy reapPolicy) {
    this(ns, nsDir, dirCreationMode, nsProperties, null, mgBase, ringMaster, isRecovery, activeRetrievals, reapPolicy,
        JVMUtil.getGlobalFinalization());
  }

  public NamespaceStore(long ns, File nsDir, DirCreationMode dirCreationMode, NamespaceProperties nsProperties,
      MessageGroupBase mgBase, NodeRingMaster2 ringMaster, boolean isRecovery,
      ConcurrentMap<UUIDBase, ActiveProxyRetrieval> activeRetrievals) {
    this(ns, nsDir, dirCreationMode, nsProperties, null, mgBase, ringMaster, isRecovery, activeRetrievals,
        NeverReapPolicy.instance);
  }

  private void createInitialHeadSegment() {
    FileSegment.SyncMode syncMode;

    if (nextSegmentID.get() != 0) {
      throw new RuntimeException("nextSegmentID.get() != 0");
    }
    Log.warning("Creating initial head segment");
    syncMode = FileSegment.SyncMode.NoSync;
    switch (nsOptions.getStorageType()) {
    case FILE_SYNC:
      syncMode = FileSegment.SyncMode.Sync;
      // fall through
    case FILE:
      try {
        headSegment = FileSegment.create(nsDir, nextSegmentID.getAndIncrement(), nsOptions.getSegmentSize(), syncMode,
            nsOptions);
      } catch (IOException ioe) {
        peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
        throw new RuntimeException(ioe);
      }
      break;
    case RAM:
      headSegment = RAMSegment.create(nsDir, nextSegmentID.getAndIncrement(), nsOptions.getSegmentSize(), nsOptions);
      ramSegments.put(headSegment.getSegmentNumber(), (RAMSegment) headSegment);
      break;
    default:
      throw new RuntimeException("panic");
    }
  }

  private static final boolean isNonBlankNonDefaultSSCode(NamespaceServerSideCode ssCode) {
    if (ssCode == null) {
      return false;
    } else {
      if (StringUtil.isNullOrEmptyTrimmed(ssCode.getUrl()) && StringUtil.isNullOrEmptyTrimmed(
          ssCode.getPutTrigger()) && StringUtil.isNullOrEmptyTrimmed(ssCode.getRetrieveTrigger())) {
        return false;
      } else {
        return true;
      }
    }
  }

  protected Pair<PutTrigger, RetrieveTrigger> createTriggers() {
    return instantiateServerSideCode(nsOptions.getNamespaceServerSideCode());
  }

  private static final Pair<PutTrigger, RetrieveTrigger> instantiateServerSideCode(NamespaceServerSideCode ssCode) {
    PutTrigger putTrigger;
    RetrieveTrigger retrieveTrigger;

    putTrigger = null;
    retrieveTrigger = null;
    if (isNonBlankNonDefaultSSCode(ssCode)) {
      if (ssCode.getUrl() != null && ssCode.getUrl().trim().length() != 0) {
        Log.warningf("Ignoring server side code %s. Remote code not currently supported", ssCode.getUrl());
      } else {
        try {
          putTrigger = (PutTrigger) Class.forName(ssCode.getPutTrigger()).getDeclaredConstructor().newInstance();
          if (ssCode.getPutTrigger().equals(ssCode.getRetrieveTrigger())) {
            retrieveTrigger = (RetrieveTrigger) putTrigger;
          } else {
            retrieveTrigger = (RetrieveTrigger) Class.forName(ssCode.getRetrieveTrigger()).getDeclaredConstructor().newInstance();
          }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
          Log.logErrorWarning(e, "Unable to instantiate server side code: " + ssCode);
        }
      }
    }
    return new Pair<>(putTrigger, retrieveTrigger);
  }

  private void initRAMSegments() {
    headSegment = RAMSegment.create(nsDir, nextSegmentID.getAndIncrement(), nsOptions.getSegmentSize(), nsOptions);
    ramSegments.put(headSegment.getSegmentNumber(), (RAMSegment) headSegment);
  }

  public void startWatches(SilverKingZooKeeperClient zk, String nsLinkBasePath, LinkCreationListener linkCreationListener) {
    if (parent == null && nsOptions.getVersionMode() == NamespaceVersionMode.SINGLE_VERSION && nsOptions.getAllowLinks()) {
      watchForLink(zk, nsLinkBasePath, linkCreationListener);
    }
  }

  public File getDir() {
    return nsDir;
  }

  private boolean isIdle() {
    return SystemTimeUtil.timerDrivenTimeSource.absTimeMillis() - nsMetrics.getLastActivityMillis() > nsIdleIntervalMillis;
  }

  private void watchForLink(SilverKingZooKeeperClient zk, String nsLinkBasePath, LinkCreationListener linkCreationListener) {
    new LinkCreationWatcher(zk, nsLinkBasePath, ns, linkCreationListener);
  }

  public void linkParent(NamespaceStore parent) {
    if (this.parent == null) {
      Log.warningf("%x linked to parent %x", ns, parent.getNamespace());
      this.parent = parent;
    } else {
      Log.warning("Ignoring extra call to linkParent");
    }
  }

  public long getNamespace() {
    return ns;
  }

  public NamespaceProperties getNamespaceProperties() {
    return nsProperties;
  }

  public NamespaceMetrics getNamespaceMetrics() {
    return nsMetrics;
  }

  public long getTotalKeys() {
    if (retrieveTrigger != null && retrieveTrigger.subsumesStorage()) {
      return retrieveTrigger.getTotalKeys();
    } else {
      return nsMetrics.getTotalKeys();
    }
  }

  public NamespaceOptions getNamespaceOptions() {
    return nsOptions;
  }

  public ValueRetentionPolicyImpl getValueRetentionPolicyImpl() {
    return this.vrp;
  }

  protected boolean isDynamic() {
    return false;
  }

  public KeyToReplicaResolver getKeyToReplicaResolver() {
    return getRingMaster();
  }

  public NodeRingMaster2 getRingMaster() {
    return ringMaster;
  }

  // must hold lock
  private void valueSegmentsPut(DHTKey key, int value) {
    try {
      valueSegments.put(key, value);
    } catch (TableFullException tfe) {
      Log.warningAsync(String.format("valueSegments full %x. Creating new table.", ns));
      valueSegments = IntArrayDHTKeyCuckoo.rehashAndAdd(valueSegments, key, value);
    }
  }

  // used by recovery. no lock needed in recovery
  private void setHeadSegment(FileSegment segment) {
    Log.warning("Setting head segment: ", segment.getSegmentNumber());
    this.headSegment = segment;
    nextSegmentID.set(segment.getSegmentNumber() + 1);
  }

  @Override
  public void rollOverHeadSegment(boolean force) {
    if (force || TimeBasedSegmentRollOver.needRollOver(headSegment)) {
    writeLockAll();
    try {
      newHeadSegment(true);
    } finally {
      writeUnlockAll();
    }
  }
  }

  // writeLock must be held at caller
  private void newHeadSegment() {
    newHeadSegment(debugSegments);
  }

  // writeLock must be held at caller
  private void newHeadSegment(boolean verboseLogging) {
    WritableSegmentBase oldHead;

    // headCreationLock is currently redundant since we already have a write lock
    // Think about whether or not we want to keep it. Would it ever be needed?
    //headCreationLock.lock();

    // Persist the current head segment first, since the 'FileSegment.create' below will create the new segment file
    // immediately, upon recovering, the new segment will be set as the head segment, which breaks the assumption that
    // all previous segments before the head got fully flushed.
    oldHead = headSegment;
    if (nsOptions.getStorageType().isFileBased()) {
      // FUTURE - persistence may be incomplete...think about this
    try {
        oldHead.persist();
        if (TracerFactory.isInitialized()) {
          TracerFactory.getTracer().onSegmentRollover(oldHead.getSegmentNumber());
        }
        if (verboseLogging) {
          Log.warning("persisted segment: " + oldHead.getSegmentNumber());
        }
      } catch (IOException ioe) {
        peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
        throw new RuntimeException(ioe);
      }
      // FUTURE - consider persisting in another thread - would need to handle mutual exclusion, consistency, etc.
    }

    try {
      WritableSegmentBase newHead;
      FileSegment.SyncMode syncMode;

      syncMode = FileSegment.SyncMode.NoSync;
      switch (nsOptions.getStorageType()) {
      case FILE_SYNC:
        syncMode = FileSegment.SyncMode.Sync;
        // fall through
      case FILE:
        newHead = FileSegment.create(nsDir, nextSegmentID.getAndIncrement(), nsOptions.getSegmentSize(), syncMode,
            nsOptions);
        break;
      case RAM:
        RAMSegment ramSegment;

        ramSegment = RAMSegment.create(nsDir, nextSegmentID.getAndIncrement(), nsOptions.getSegmentSize(), nsOptions);
        ramSegments.put(ramSegment.getSegmentNumber(), ramSegment);
        newHead = ramSegment;
        break;
      default:
        throw new RuntimeException();
      }
      Log.infof("newHeadSegment %d", newHead.getSegmentNumber());
      headSegment = newHead;
    } catch (IOException ioe) {
      peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
      throw new RuntimeException(ioe);
      //} finally {
      //headCreationLock.unlock();
    }
    }

  public List<Integer> getKeySegments(DHTKey key) {
    metaReadLock.lock();
    try {
      Integer newestSegment;

      newestSegment = valueSegments.get(key);
      if (newestSegment == IntCuckooConstants.noSuchValue) {
        newestSegment = null;
      }
      if (newestSegment != null) {
        if (newestSegment >= 0) {
          return ImmutableList.of(newestSegment);
        } else {
          OffsetList offsetList;

          offsetList = offsetListStore.getOffsetList(-newestSegment);
          return ImmutableList.copyOf(offsetList);
        }
      } else {
        return ImmutableList.of();
      }
    } finally {
      metaReadLock.unlock();
    }
  }

  // lock must be held
  private Integer newestSegment(DHTKey key) {
    metaReadLock.lock();
    try {
      Integer newestSegment;

      newestSegment = valueSegments.get(key);
      if (newestSegment == IntCuckooConstants.noSuchValue) {
        newestSegment = null; // FIXME - TEMP
      }
      if (newestSegment != null) {
        if (newestSegment >= 0) {
          return newestSegment;
        } else {
          OffsetList offsetList;

          offsetList = offsetListStore.getOffsetList(-newestSegment);
          return offsetList.getLastOffset();
        }
      } else {
        return null;
      }
    } finally {
      metaReadLock.unlock();
    }
  }

  // writeLock must be held
  private VersionCheckResult checkPutVersion(DHTKey key, long version, long requiredPreviousVersion) {
    // FUTURE - simplify, but must be sure to avoid hurting efficiency
    if (version <= curSnapshot || version < minVersion || !nsOptions.getVersionMode().validVersion(version)) {
      return VersionCheckResult.Invalid;
    } else {
      if (requiredPreviousVersion == PutOptions.noVersionRequired) {
        if (nsOptions.getRevisionMode() == RevisionMode.NO_REVISIONS) {
          long newestVersion;

          newestVersion = newestVersion(key);
          if (newestVersion >= 0) {
            if (debugVersion) {
              Log.warningAsyncf("version %d segmentNewestVersion %d", version, newestVersion);
            }
            if (version > newestVersion) {
              return VersionCheckResult.Valid;
            } else if (version < newestVersion) {
              return VersionCheckResult.Invalid;
            } else {
              return VersionCheckResult.Equal;
            }
          } else {
            return VersionCheckResult.Valid;
          }
        } else {
          return VersionCheckResult.Valid_New_Key;
        }
      } else { // requiredPreviousVersion has been specified
        if (nsOptions.getRevisionMode() == RevisionMode.NO_REVISIONS) {
          if (version < requiredPreviousVersion) { // sanity check PutOptions
            return VersionCheckResult.Invalid;
          } else {
            long newestVersion;

            newestVersion = newestVersion(key);
            if (newestVersion >= 0) {
              // a value exists (might be an invalidation)
              if (debugVersion) {
                Log.warningAsyncf("version %d segmentNewestVersion %d", version, newestVersion);
              }
              if (version > newestVersion) {
                if (requiredPreviousVersion == newestVersion) {
                  return VersionCheckResult.Valid;
                } else {
                  if (requiredPreviousVersion == PutOptions.previousVersionNonexistent) {
                    return VersionCheckResult.Invalid;
                  } else if (requiredPreviousVersion == PutOptions.previousVersionNonexistentOrInvalid) {
                    return checkForInvalidation(key, newestVersion) ?
                        VersionCheckResult.Valid :
                        VersionCheckResult.Invalid;
                  } else {
                    return VersionCheckResult.Invalid;
                  }
                }
              } else if (version < newestVersion) {
                // revisions disallowed by the RevisionMode
                return VersionCheckResult.Invalid;
              } else { // version == newestVersion
                if (requiredPreviousVersion == newestVersion) {
                  return VersionCheckResult.Equal;
                } else {
                  if (requiredPreviousVersion == PutOptions.previousVersionNonexistent) {
                    return VersionCheckResult.Invalid;
                  } else if (requiredPreviousVersion == PutOptions.previousVersionNonexistentOrInvalid) {
                    return checkForInvalidation(key, newestVersion) ?
                        VersionCheckResult.Valid :
                        VersionCheckResult.Invalid;
                  } else {
                    return VersionCheckResult.Invalid;
                  }
                }
              }
            } else { // no value exists for this key
              if (requiredPreviousVersion == PutOptions.previousVersionNonexistent || requiredPreviousVersion == PutOptions.previousVersionNonexistentOrInvalid) {
                return VersionCheckResult.Valid;
              } else {
                return VersionCheckResult.Invalid;
              }
            }
          }
        } else { // revisions allowed
          // FIXME - semantics are not yet defined for this case
          long newestVersion;

          newestVersion = newestVersion(key);
          if (newestVersion >= 0) {
            // a value exists (might be an invalidation)
            if (debugVersion) {
              Log.warningAsyncf("version %d segmentNewestVersion %d", version, newestVersion);
            }
            if (requiredPreviousVersion == newestVersion) {
              // no need to check for nonexistence/invalid required versions in this case
              if (version == newestVersion) {
                return VersionCheckResult.Equal;
              } else {
                return VersionCheckResult.Valid;
              }
            } else {
              if (requiredPreviousVersion == PutOptions.previousVersionNonexistent) {
                return VersionCheckResult.Invalid;
              } else if (requiredPreviousVersion == PutOptions.previousVersionNonexistentOrInvalid) {
                return checkForInvalidation(key, newestVersion) ? VersionCheckResult.Valid : VersionCheckResult.Invalid;
              } else {
                return VersionCheckResult.Invalid;
              }
            }
          } else {
            if (requiredPreviousVersion == PutOptions.previousVersionNonexistent || requiredPreviousVersion == PutOptions.previousVersionNonexistentOrInvalid) {
              return VersionCheckResult.Valid;
            } else {
              return VersionCheckResult.Invalid;
            }
          }
        }
      }
    }
  }

  private LockCheckResult checkForLock(DHTKey key, StorageParameters storageParams) {
    if (storageParams.getLockSeconds() != PutOptions.noLock) {
      RetrievalOptions options;
      ByteBuffer result;

      options = OptionsHelper.newRetrievalOptions(RetrievalType.META_DATA, WaitMode.GET, VersionConstraint.greatest,
          NonExistenceResponse.NULL_VALUE, true);
      result = _retrieve(key, new InternalRetrievalOptions(options), false);
      if (result == null) {
        return LockCheckResult.Unlocked;
      } else {
        short existingLockSeconds;

        existingLockSeconds = MetaDataUtil.getLockSeconds(result, 0);
        if (existingLockSeconds == PutOptions.noLock) {
          return LockCheckResult.Unlocked;
        } else {
          long lockSeconds;
          LockCheckResult lockCheckResult;

          if (StorageProtocolUtil.storageStateValidForRead(nsOptions.getConsistencyProtocol(),
              MetaDataUtil.getStorageState(result, 0))) {
            lockSeconds = (long) existingLockSeconds;
          } else {
            // We have found an partially complete write that holds a lock.
            // Respect that lock for a small number of seconds.
            // After that, we presume that lock has failed.
            lockSeconds = maxInvalidSSLockSeconds;
          }
          lockCheckResult = systemTimeSource.absTimeNanos() <= MetaDataUtil.getCreationTime(result,
              0) + lockSeconds * 1_000_000_000L ? LockCheckResult.Locked : LockCheckResult.Unlocked;
          if (lockCheckResult == LockCheckResult.Locked) {
            if (Arrays.equals(storageParams.getValueCreator(), MetaDataUtil.getCreator(result, 0).getBytes())) {
              lockCheckResult = LockCheckResult.Unlocked;
            }
          }
          return lockCheckResult;
        }
      }
    } else {
      return LockCheckResult.Ignored;
    }
  }

  /**
   * Checks to see if another writer has managed to lock the exact same version that this operation
   * is attempting to lock.
   */
  private LockCheckResult checkForLockOnPutUpdate(DHTKey key, AbstractSegment segment, long version) {
    RetrievalOptions options;
    ByteBuffer result;

    options = OptionsHelper.newRetrievalOptions(RetrievalType.META_DATA, WaitMode.GET,
        VersionConstraint.exactMatch(version), NonExistenceResponse.NULL_VALUE, true);
    result = retrieve(segment, key, new InternalRetrievalOptions(options), true);
    if (result == null) {
      //Log.warningf("%s %d error in checkForLockOnPutUpdate", KeyUtil.keyToString(key), version);
      return LockCheckResult.Unlocked;
    } else {
      short existingLockSeconds;

      existingLockSeconds = MetaDataUtil.getLockSeconds(result, 0);
      //Log.warningf("cflopu: %s %d %d %d %s %s", KeyUtil.keyToString(key), version, existingLockSeconds,
      // MetaDataUtil.getVersion(result, 0), writeLock, writeLock.isHeldByCurrentThread());
      if (existingLockSeconds == PutOptions.noLock) {
        return LockCheckResult.Unlocked;
      } else {
        return systemTimeSource.absTimeNanos() <= MetaDataUtil.getCreationTime(result,
            0) + (long) existingLockSeconds * 1_000_000_000L ? LockCheckResult.Locked : LockCheckResult.Unlocked;
      }
    }
  }

  // FUTURE - make checkForLock and checkForInvalidation use a common retrieval
  private boolean checkForInvalidation(DHTKey key, long version) {
    RetrievalOptions options;
    ByteBuffer result;

    options = OptionsHelper.newRetrievalOptions(RetrievalType.META_DATA, WaitMode.GET,
        VersionConstraint.exactMatch(version), NonExistenceResponse.NULL_VALUE, true);
    result = _retrieve(key, options);
    if (result == null) {
      return true; // shouldn't happen, but safe
    } else {
      return MetaDataUtil.isInvalidation(result, 0);
    }
  }

  // write lock must be held
  protected long newestVersion(DHTKey key) {
    Integer newestSegment;

    newestSegment = newestSegment(key);
    if (debugVersion) {
      Log.warningAsyncf("newestVersion(%s)", key);
      Log.warningAsyncf("newestSegment: (%s)", newestSegment);
    }
    if (newestSegment != null && newestSegment >= 0) {
      long[] segmentNewestVersionAndStorageTime;

      if (newestSegment != headSegment.getSegmentNumber()) {
        segmentNewestVersionAndStorageTime = segmentNewestVersion(newestSegment, key);
      } else {
        segmentNewestVersionAndStorageTime = segmentVersionAndStorageTime(headSegment, key, newestVersionOptions);
      }
      return segmentNewestVersionAndStorageTime[VERSION_INDEX];
    } else {
      return -1;
    }
  }

  // write lock must be held
  private Set<Waiter> checkPendingWaitFors(DHTKey key) {
    if (debugWaitFor) {
      Log.warningAsync("checkPendingWaitFors");
      Log.warningAsyncf("pendingWaitFors.size() %d", pendingWaitFors.size());
    }
    if (pendingWaitFors.size() > 0) {
      Set<Waiter> triggeredWaiters;
      Collection<PendingWaitFor> pendingWaitForCollection;

      triggeredWaiters = null;
      pendingWaitForCollection = pendingWaitFors.get(key);
      if (pendingWaitForCollection != null) {
        for (PendingWaitFor pendingWaitFor : pendingWaitForCollection) {
          ByteBuffer result;

          if (debugWaitFor) {
            Log.warningAsyncf("pendingWaitFor %s options %s", pendingWaitFor, pendingWaitFor.getOptions());
          }
          result = _retrieve(key, pendingWaitFor.getOptions());
          if (result != null) {
            Waiter waiter;

            // we have a result, now we need to send it back...
            waiter = activeRetrievals.get(pendingWaitFor.getOpUUID());
            if (waiter != null) {
              if (triggeredWaiters == null) {
                triggeredWaiters = new HashSet<>();
              }
              triggeredWaiters.add(waiter);
              // duplicate the result so that each
              // waiter gets its own
              if (debugWaitFor) {
                Log.warningAsyncf("Triggering waiter for %s", pendingWaitFor.getOpUUID());
              }
              waiter.waitForTriggered(key, result.duplicate());
              // Give the waiter the result of the waitfor.
              // In addition, we pass back the triggered wait fors so that
              // the waiter can send messages back when
              // they have all been gathered.
            } else {
              if (debugWaitFor) {
                Log.warningAsyncf("No waiter found for %s", pendingWaitFor.getOpUUID());
              }
            }
            pendingWaitForCollection.remove(pendingWaitFor);
          } else {
            if (debugWaitFor) {
              Log.warningAsyncf("No result found for %s", KeyUtil.keyToString(key));
            }
          }
        }
      } else {
        if (debugWaitFor) {
          Log.warningAsync("pendingWaitForCollection not found");
        }
      }
      return triggeredWaiters;
    } else {
      return null;
    }
  }

  private void handleTriggeredWaitFors(Set<Waiter> triggeredWaitFors) {
    if (debugWaitFor) {
      Log.warningAsync("handleTriggeredWaitFors");
    }
    if (triggeredWaitFors != null) {
      for (Waiter triggeredWaitFor : triggeredWaitFors) {
        triggeredWaitFor.relayWaitForResults();
      }
    }
  }

  public void put(List<StorageValueAndParameters> values, byte[] userData, KeyedOpResultListener resultListener) {
    Set<Waiter> triggeredWaitFors;
    NamespaceVersionMode nsVersionMode;
    boolean locked;

    triggeredWaitFors = null;

    nsVersionMode = nsOptions.getVersionMode();

    if (enablePendingPuts && putTrigger != null && putTrigger.supportsMerge() && (!(resultListener instanceof KeyedOpResultMultiplexor))) {
      // Pending puts currently only applied to server side code
      // Pending puts doesn't support userdata
      // Disallow puts to be deferred to the pending queue multiple times
      if (writeLock.tryLock()) {
        locked = true;
      } else {
        locked = false;
        addPendingPut(values, resultListener);
        return;
      }
    } else {
      locked = false;
    }

    //LWTThreadUtil.setBlocked();
    if (!locked) {
      writeLock.lock();
    }
    try {
      int numPuts;
      int numInvalidations;

      numPuts = 0;
      numInvalidations = 0;
      // System.out.printf("NamespaceStore.put() group size: %d\n", values.size());
      for (StorageValueAndParameters value : values) {
        OpResult storageResult;

        //if (MetaDataUtil.isInvalidated(value.getValue(), value.getValue().position())) {
        //    ++numInvalidations;
        //} else {
        ++numPuts;
        //}
        if (putTrigger != null) {
          storageResult = putTrigger.put(this, value.getKey(), value.getValue(),
              new SSStorageParametersImpl(value, value.getValue().remaining()), userData, nsVersionMode);
        } else {
          storageResult = _put(value.getKey(), value.getValue(), value, userData, nsVersionMode);
        }
        //if (storageResult != OpResult.SUCCEEDED) Log.warningf("fail _put %s %s %d", KeyUtil.keyToString(value
        // .getKey()), storageResult, value.getVersion()); // for debugging
        resultListener.sendResult(value.getKey(), storageResult);
        if (storageResult == OpResult.SUCCEEDED) {
          Set<Waiter> _triggeredWaitFors;

          _triggeredWaitFors = checkPendingWaitFors(value.getKey());
          if (_triggeredWaitFors != null) {
            if (triggeredWaitFors == null) {
              triggeredWaitFors = new HashSet<>();
            }
            triggeredWaitFors.addAll(_triggeredWaitFors);
          }
        }
      }
      nsMetrics.addPuts(numPuts, numInvalidations, SystemTimeUtil.timerDrivenTimeSource.absTimeMillis());
    } finally {
      writeLock.unlock();
      //LWTThreadUtil.setNonBlocked();
    }
    if (triggeredWaitFors != null) {
      handleTriggeredWaitFors(triggeredWaitFors);
    }
  }

  // for use by pending put code to notify waiters
  // must hold lock
  // must call handleTriggeredWaitFors() after releasing lock
  public Set<Waiter> notifyAndCheckWaiters(Map<DHTKey, OpResult> results, KeyedOpResultListener resultListener) {
    Set<Waiter> triggeredWaitFors;

    triggeredWaitFors = null;
    // System.out.printf("NamespaceStore.put() group size: %d\n", values.size());
    for (Map.Entry<DHTKey, OpResult> result : results.entrySet()) {
      resultListener.sendResult(result.getKey(), result.getValue());
      if (result.getValue() == OpResult.SUCCEEDED) {
        Set<Waiter> _triggeredWaitFors;

        _triggeredWaitFors = checkPendingWaitFors(result.getKey());
        if (_triggeredWaitFors != null) {
          if (triggeredWaitFors == null) {
            triggeredWaitFors = new HashSet<>();
          }
          triggeredWaitFors.addAll(_triggeredWaitFors);
        }
      }
    }
    return triggeredWaitFors;
  }

  /**
   * Checks to see if this put() should be allowed to proceed on the basis that it is a duplicate of a previous
   * storage operation.
   * <p>
   * NB this currently disregards the userData of the operation.
   *
   * @param key
   * @param value
   * @param storageParams
   * @return
   */
  private SegmentStorageResult checkForDuplicateStore(DHTKey key, ByteBuffer value, StorageParameters storageParams,
      VersionConstraint vc) {
    RetrievalOptions options;
    ByteBuffer result;

    // this comparison isn't returning invalid version like it should for some cases
    options = OptionsHelper.newRetrievalOptions(RetrievalType.VALUE, WaitMode.GET, vc, NonExistenceResponse.EXCEPTION,
        true);
    result = _retrieve(key, options);
    if (result == null) {
      // previous storage operation is not complete
      return SegmentStorageResult.previousStoreIncomplete;
    } else {
      if (!StorageProtocolUtil.storageStateValidForRead(nsOptions.getConsistencyProtocol(),
          MetaDataUtil.getStorageState(result, 0))) {
        // previous storage operation is not complete
        return SegmentStorageResult.previousStoreIncomplete;
      } else {
        // FUTURE - allow for no mutation detection on a per-namespace basis?
        if (storageParams.getChecksumType() == MetaDataUtil.getChecksumType(result, 0)) {
          if (storageParams.getChecksumType() != ChecksumType.NONE) {
            if (Arrays.equals(MetaDataUtil.getChecksum(result, 0), storageParams.getChecksum())) {
              return SegmentStorageResult.duplicateStore;
            } else {
              return SegmentStorageResult.mutation;
            }
          } else {
            if (BufferUtil.equals(result, MetaDataUtil.getDataOffset(result, 0), value, 0, value.limit())) {
              return SegmentStorageResult.duplicateStore;
            } else {
              return SegmentStorageResult.mutation;
            }
          }
        } else {
          return SegmentStorageResult.mutation;
        }
      }
    }
  }

  protected final OpResult _put(DHTKey key, ByteBuffer value, StorageParametersAndRequirements storageParams,
      byte[] userData, NamespaceVersionMode nsVersionMode) {
    WritableSegmentBase storageSegment;
    SegmentStorageResult storageResult;
    LockCheckResult lockCheckResult;
    VersionCheckResult versionCheckResult;

    if (testStorageCorruption && !isDynamic() && ns != NamespaceUtil.metaInfoNamespace.contextAsLong()) {
      MetaDataUtil.testCorruption(value, storageCorruptionProbability, value.limit() - 2);
    }

    if (debugVersion) {
      Log.warningAsyncf("StorageParameters: %s", storageParams);
    }
    if (debug) {
      Log.warningAsyncf("_put %s %s %s", key, value, storageParams.getChecksumType());
      Log.warningAsyncf("userData length: %s", (userData == null ? "null" : userData.length));
    }

    if (isDynamic()) {
      return OpResult.MUTATION; // FUTURE - ADD R/O NS OpResult
    }

    lockCheckResult = checkForLock(key, storageParams);
    if (lockCheckResult == LockCheckResult.Locked) {
      return OpResult.LOCKED;
    }

    // FUTURE - Improve below. Note that we've moved time-based out of here
    // for now, and possibly forever. The feature that currently is not supported
    // is sequential versioning
    if (storageParams.getVersion() == DHTConstants.unspecifiedVersion && nsOptions.getVersionMode().isSystemSpecified()) {
      //versionCheckResult = VersionCheckResult.Valid;
      //storageParams = getSystemVersionParams(key, storageParams);
      throw new RuntimeException("Panic"); // moved to ActiveProxyPut for now
    } else {
      versionCheckResult = checkPutVersion(key, storageParams.getVersion(), storageParams.getRequiredPreviousVersion());
      if (debugVersion) {
        Log.warningf("%s %d %d %s", key, storageParams.getVersion(), storageParams.getRequiredPreviousVersion(),
            versionCheckResult);
      }
    }
    if (versionCheckResult == VersionCheckResult.Invalid) {
      if (debug) {
        Log.warningAsync("_put returning INVALID_VERSION");
      }
      return OpResult.INVALID_VERSION;
    } else {
      if (versionCheckResult == VersionCheckResult.Equal || (versionCheckResult != VersionCheckResult.Valid_New_Key && nsOptions.getVersionMode() == NamespaceVersionMode.SINGLE_VERSION && nsOptions.getRevisionMode() == RevisionMode.NO_REVISIONS)) {
        VersionConstraint vc = versionCheckResult == VersionCheckResult.Equal ? VersionConstraint.exactMatch(
            storageParams.getVersion()) : VersionConstraint.maxBelowOrEqual(storageParams.getVersion());
        storageResult = checkForDuplicateStore(key, value, storageParams, vc);
        if (debug) {
          Log.warningAsyncf("checkForDuplicateStore result %s", storageResult);
        }
        if (storageResult != SegmentStorageResult.previousStoreIncomplete) {
          return storageResult.toOpResult();
        }
        if (debug) {
          Log.warningAsync("fall through after checkForDuplicateStore");
        }
      }

      if (TimeBasedSegmentRollOver.needRollOver(headSegment)) {
        newHeadSegment();
      }
      storageSegment = headSegment;
      try {
        storageResult = storageSegment.put(key, value, storageParams, userData, nsOptions);
      } catch (TableFullException tfe) {
        storageResult = SegmentStorageResult.segmentFull;
        // FUTURE - think about resizing the segment table instead
        // of flipping to a new segment
      }
      while (storageResult.callerShouldRetry()) {
        if (value.remaining() > headSegment.getDataSegmentSize()) {
          throw new RuntimeException("value > segment size");
          // FIXME - we don't support values > segment size
          // client needs to enforce this
        }
        newHeadSegment();
        storageSegment = headSegment;
        storageResult = storageSegment.put(key, value, storageParams, userData, nsOptions);
      }
      if (storageResult == SegmentStorageResult.stored) {
        putSegmentNumberAndVersion(key, storageSegment.getSegmentNumber(), storageParams.getVersion(),
            storageParams.getCreationTime(), null);
        addToSizeStats(storageParams);
      } else {
        if (debugSegments) {
          Log.warning("not stored " + storageResult);
        }
      }
    }
    //if (storageResult != SegmentStorageResult.stored) Log.warningf("!stored _put2 %s %s %d %s", KeyUtil.keyToString
    // (key), storageResult, storageParams.getVersion(), storageParams); // for debugging
    return storageResult.toOpResult();
  }

  public void addToSizeStats(StorageParameters storageParams) {
    addToSizeStats(storageParams.getUncompressedSize(), storageParams.getCompressedSize());
  }

  public void addToSizeStats(int uncompressedSize, int compressedSize) {
    nsMetrics.addBytes(uncompressedSize, compressedSize);
  }

  private StorageParameters getSystemVersionParams(DHTKey key, StorageParameters storageParams) {
    long version;

    switch (nsOptions.getVersionMode()) {
    case SEQUENTIAL:
      version = newestVersion(key) + 1;
      break;
    case SYSTEM_TIME_MILLIS:
      version = systemTimeSource.absTimeMillis();
      break;
    case SYSTEM_TIME_NANOS:
      version = systemTimeSource.absTimeNanos();
      break;
    default:
      throw new RuntimeException("non-system or unexpected VersionMode");
    }
    return storageParams.version(version);
  }

  public void putSegmentNumberAndVersion(DHTKey key, int segmentNumber, long version, long creationTime,
      AbstractSegment recoverySegment) {
    // recoverySegment is only used during recovery; null otherwise
    metaWriteLock.lock();
    try {
      int rawPrevSegment;

      if (debugSegments) {
        Log.warning("putSegmentNumberAndVersion " + key + " " + segmentNumber + " " + version);
      }
      rawPrevSegment = valueSegments.get(key);
      if (debugSegments) {
        Log.warning("prevSegment: ", rawPrevSegment);
      }
      if (rawPrevSegment == IntCuckooConstants.noSuchValue) {
        if (nsOptions.isWriteOnce() || nsOptions.getStorageType() == StorageType.RAM) {
          valueSegmentsPut(key, segmentNumber);
        } else {
          OffsetList offsetList;

          // the current pkc doesn't support version storage; we must either use an offset list,
          // or touch disk to get the version
          // (touching disk is expensive when computing checksum trees)
          // Similar logic in WritableSegmentBase._put()
          if (debugSegments) {
            Log.warning("multi versioning using offset list");
          }
          offsetList = offsetListStore.newOffsetList();
          offsetList.putOffset(version, segmentNumber, creationTime);
          valueSegmentsPut(key, -((RAMOffsetList) offsetList).getIndex());
        }
        // new key, record it in stats
        nsMetrics.incTotalKeys();
      } else if (rawPrevSegment >= 0) {
        if (false && nsOptions.isWriteOnce()) {
          valueSegmentsPut(key, segmentNumber);
        } else {
          int prevSegment;

          prevSegment = rawPrevSegment;
          // this key has been stored previously; check to see if
          // we need to do more work
          if (false && prevSegment == segmentNumber) {
            // stored in the same segment; no action necessary
            if (debugSegments) {
              Log.warning("prevSegment == storageSegment.getSegmentNumber()");
            }
          } else {
            // store in a different segment; ensure multiple
            // segments are set up correctly
            OffsetList offsetList;
            long[] prevVersionAndStorageTime;
            boolean removed;

            // only a single previous segment exists
            // we need to save the old and the new segments in the
            // offset list
            if (debugSegments) {
              Log.warning("prevSegment >= 0");
            }
            offsetList = offsetListStore.newOffsetList();
            if (recoverySegment != null && prevSegment == recoverySegment.getSegmentNumber()) {
              prevVersionAndStorageTime = segmentVersionAndStorageTime(recoverySegment, key, newestVersionOptions);
            } else {
              prevVersionAndStorageTime = segmentNewestVersion(prevSegment, key);
            }
            assert prevVersionAndStorageTime[VERSION_INDEX] != noSuchVersion;
            if (debugSegments) {
              Log.warning("prevVersion: ", prevVersionAndStorageTime[VERSION_INDEX]);
            }
            offsetList.putOffset(prevVersionAndStorageTime[VERSION_INDEX], prevSegment,
                prevVersionAndStorageTime[STORAGE_TIME_INDEX]);
            offsetList.putOffset(version, segmentNumber, creationTime);
            removed = valueSegments.remove(key);
            if (!removed) {
              Log.warning(valueSegments.get(key));
              throw new RuntimeException("valueSegments.remove() failed for: " + key);
            }
            if (debugSegments) {
              Log.warning("removed valueSegments: ", key);
              if (valueSegments.get(key) != IntCuckooConstants.noSuchValue) {
                Log.warning(valueSegments.get(key));
                throw new RuntimeException("remove failed");
              }
            }
            valueSegmentsPut(key, -((RAMOffsetList) offsetList).getIndex());
          }
        }
      } else {
        OffsetList offsetList;
        int prevSegment;

        if (debugSegments) {
          Log.warning("prevSegment < 0");
        }
        offsetList = offsetListStore.getOffsetList(-rawPrevSegment);
        prevSegment = offsetList.getLastOffset();
        if (false && prevSegment == segmentNumber) {
          // stored in the same segment; no action necessary
          if (debugSegments) {
            Log.warning("prevSegment == storageSegment.getSegmentNumber()");
          }
        } else {
          offsetList.putOffset(version, segmentNumber, creationTime);
        }
      }
    } finally {
      metaWriteLock.unlock();
    }
  }

  public List<OpResult> putUpdate(List<? extends DHTKey> updates, long version) {
    List<OpResult> results;
    Set<Waiter> triggeredWaitFors;

    triggeredWaitFors = null;
    results = new ArrayList<>(updates.size());
    writeLock.lock();
    try {
      for (DHTKey update : updates) {
        MessageGroupKeyOrdinalEntry entry;
        OpResult result;

        entry = (MessageGroupKeyOrdinalEntry) update;
        result = _putUpdate(entry, version, entry.getOrdinal());
        results.add(result);
        if (result == OpResult.SUCCEEDED && StorageProtocolUtil.storageStateValidForRead(
            nsOptions.getConsistencyProtocol(), entry.getOrdinal())) {
          Set<Waiter> _triggeredWaitFors;

          _triggeredWaitFors = checkPendingWaitFors(update);
          if (_triggeredWaitFors != null) {
            if (triggeredWaitFors == null) {
              triggeredWaitFors = new HashSet<>();
            }
            triggeredWaitFors.addAll(_triggeredWaitFors);
          }
        }
      }
    } finally {
      writeLock.unlock();
    }
    if (triggeredWaitFors != null) {
      handleTriggeredWaitFors(triggeredWaitFors);
    }
    return results;
  }

  public OpResult putUpdate_(DHTKey key, long version, byte storageState) {
    OpResult result;
    Set<Waiter> triggeredWaitFors;

    if (debug) {
      Log.warningAsync("Single key putUpdate()");
    }
    triggeredWaitFors = null;
    writeLock.lock();
    try {
      result = _putUpdate(key, version, storageState);
      //if (result != OpResult.SUCCEEDED) Log.warningf("fail putUpdate %s %s %d", KeyUtil.keyToString(key), result,
      // version); // for debugging
      if (result == OpResult.SUCCEEDED && StorageProtocolUtil.storageStateValidForRead(
          nsOptions.getConsistencyProtocol(), storageState)) {
        triggeredWaitFors = checkPendingWaitFors(key);
      }
    } finally {
      writeLock.unlock();
    }
    if (triggeredWaitFors != null) {
      handleTriggeredWaitFors(triggeredWaitFors);
    }
    return result;
  }

  private OpResult _putUpdate(DHTKey key, long version, byte storageState) {
    if (putTrigger != null) {
      return putTrigger.putUpdate(this, key, version, storageState);
    } else {
      return __putUpdate(key, version, storageState);
    }
  }

  @Override
  public OpResult putUpdate(DHTKey key, long version, byte storageState) {
    return __putUpdate(key, version, storageState);
  }

  private OpResult __putUpdate(DHTKey key, long version, byte storageState) {
    OpResult result;
    int segmentNumber;

    if (debugVersion) {
      Log.warningAsyncf("putUpdate: %s", key);
      Log.warningAsyncf("version: %s", version);
    }
    segmentNumber = getSegmentNumber(key, VersionConstraint.exactMatch(version));
    assert segmentNumber >= 0 || segmentNumber == IntCuckooConstants.noSuchValue;
    if (segmentNumber == IntCuckooConstants.noSuchValue) {
      if (debug) {
        Log.warningAsync("_putUpdate returning INVALID_VERSION");
      }
      Log.warningf("Couldn't find %s %d in _putUpdate()", KeyUtil.keyToString(key), version);
      return OpResult.ERROR;
      //return OpResult.INVALID_VERSION;
    } else {
      if (headSegment.getSegmentNumber() == segmentNumber) {
        if (debugSegments) {
          Log.warning("PutUpdate, head segment");
        }
        result = headSegment.putUpdate(key, version, storageState, false);
        if (result == OpResult.LOCKED) {
          // OpResult.LOCKED here simply indicates the intent of the store to lock
          if (checkForLockOnPutUpdate(key, headSegment, version) == LockCheckResult.Locked) {
            result = OpResult.LOCKED;
          } else {
            result = headSegment.putUpdate(key, version, storageState, true);
          }
        }
        if (debugSegments) {
          Log.warning("Done PutUpdate, head segment " + result + " " + storageState);
        }
      } else {
        try {
          WritableSegmentBase segment;

          segment = getFileSegment(segmentNumber, SegmentPrereadMode.NoPreread);
          if (debugSegments) {
            Log.warning("Read from file segment");
          }
          result = segment.putUpdate(key, version, storageState, false);
          if (result == OpResult.LOCKED) {
            // OpResult.LOCKED here simply indicates the intent of the store to lock
            if (checkForLockOnPutUpdate(key, segment, version) == LockCheckResult.Locked) {
              result = OpResult.LOCKED;
            } else {
              result = segment.putUpdate(key, version, storageState, true);
            }
          }
          if (debugSegments) {
            Log.warning("Done read from file segment");
            Log.warning("result: " + result);
          }
        } catch (IOException ioe) {
          peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
          Log.logErrorWarning(ioe);
          return OpResult.ERROR;
        }
      }
      return result;
    }
  }

  // For analysis only
  private static final AtomicInteger commonSegment = new AtomicInteger();
  private static final AtomicInteger totalKeys = new AtomicInteger();

  private void checkTriggerAndRetrieve(DHTKey[] keys, InternalRetrievalOptions options,
      RetrieveCallback<ByteBuffer[], Void> callback) {
    if (retrieveTrigger == null) {
      if (keys.length > 1) {
        callback.apply(_retrieve(keys, options));
      } else {
        // special case single retrieval (for speed)
        _retrieve(keys[0], options, (ByteBuffer result) -> {
          ByteBuffer[] results;

          results = new ByteBuffer[1];
          results[0] = result;
          return callback.apply(results);
        });
      }
    } else {
      retrieveViaTrigger(keys, options, callback);
    }
  }

  public void retrieve(List<? extends DHTKey> keys, InternalRetrievalOptions options, UUIDBase opUUID,
      RetrieveCallback<Pair<DHTKey, ByteBuffer>, Void> singleResultCallback) {
    DHTKey[] _keys;
    RetrieveCallback<ByteBuffer[], Void> callback;
    RetrieveCallback<List<ByteBuffer>, Void> groupedCallback;

    groupedCallback = (List<ByteBuffer> results) -> {
      for (int i = 0; i < results.size(); i++) {
        Pair<DHTKey, ByteBuffer> resultAndKey;

        resultAndKey = new Pair<>(keys.get(i), results.get(i));
        singleResultCallback.apply(resultAndKey);
      }
      return null;
    };

    if (debugVersion) {
      Log.fineAsync("retrieve internal options: %s", options);
    }
    nsMetrics.addRetrievals(keys.size(), SystemTimeUtil.timerDrivenTimeSource.absTimeMillis());
    _keys = new DHTKey[keys.size()];
    for (int i = 0; i < _keys.length; i++) {
      _keys[i] = keys.get(i);
    }

    callback = (ByteBuffer[] _results) -> {
      for (int i = 0; i < _results.length; i++) {
        if (parent != null) {
          VersionConstraint vc;

          if (debugParent) {
            Log.warning("parent != null");
          }
          vc = options.getVersionConstraint();

          // We look in parent if the vc could possibly be answered by the parent
          // in a way that would override what we have from this namespace.

          if (_results[i] == null) {
            if (debugParent) {
              Log.warningf("%x null result. Checking parent %x.", ns, parent.getNamespace());
            }
            // If result from this ns is null, look in the parent.
            _results[i] = parent._retrieve(_keys[i], makeOptionsForNestedRetrieve(options));
            if (debugParent) {
              if (_results[i] != null) {
                Log.warning("Found result in parent");
              }
            }
          } else {
            // If we have a non-null value from this ns, and the vc mode is GREATEST
            // then the value that we already have is the best.
            // Otherwise for the LEAST case, look in the parent to see if it has a better result.
            if (vc.getMode() == VersionConstraint.Mode.LEAST) {
              if (debugParent) {
                Log.warning("Non-null result, but mode LEAST. checking parent");
              }
              ByteBuffer parentResult;

              parentResult = parent._retrieve(_keys[i], makeOptionsForNestedRetrieve(options));
              if (parentResult != null) {
                // if the parent had any valid result, then - by virtue of the fact
                // that all parent versions are < child versions - the parent
                // result is preferred
                _results[i] = parentResult;
                if (_results[i] != null) {
                  Log.warning("Found result in parent");
                }
              }
            }
          }
        }

        if (_results[i] == null && options.getWaitMode() == WaitMode.WAIT_FOR && options.getVersionConstraint().getMax() > curSnapshot) {
          // Note that since we hold the readLock, a write cannot come
          // in while we add the pending wait for.
          addPendingWaitFor(_keys[i], options.getRetrievalOptions(), opUUID);
        }
        if (options.getVerifyIntegrity()) {
          _results[i] = verifyIntegrity(_keys[i], _results[i]);
        }
      }

      return groupedCallback.apply(SKImmutableList.copyOf(_results));
    };

    checkTriggerAndRetrieve(_keys, options, callback);
  }

  public void retrieve_nongroupedImpl(List<? extends DHTKey> keys, InternalRetrievalOptions options, UUIDBase opUUID,
      RetrieveCallback<Pair<DHTKey, ByteBuffer>, Void> singleResultCallback) {
    boolean shouldAcquireReadLock;

    if (debugVersion) {
      Log.warningAsyncf("retrieve internal options: %s", options);
    }

    nsMetrics.addRetrievals(keys.size(), SystemTimeUtil.timerDrivenTimeSource.absTimeMillis());

    // Note that we do not acquire the read lock if this is a Callback trigger as those are expected to perform their
    // own lock management. The read lock is acquired when there is no trigger or when the trigger implements the
    // RetrieveTrigger.Direct interface.
    shouldAcquireReadLock = retrieveTrigger == null || RetrieveTrigger.Direct.class.isAssignableFrom(
        retrieveTrigger.getClass());

    if (shouldAcquireReadLock) {
      readLock.lock();
    }

    try {
      for (DHTKey key : keys) {
        RetrieveCallback<ByteBuffer, Void> callback;

        callback = (ByteBuffer result) -> {
          Pair<DHTKey, ByteBuffer> resultAndKey;

          if (parent != null) {
            VersionConstraint vc;

            if (debugParent) {
              Log.warning("parent != null");
            }
            vc = options.getVersionConstraint();

            // We look in parent if the vc could possibly be answered by the parent
            // in a way that would override what we have from this namespace.

            if (result == null) {
              if (debugParent) {
                Log.warningf("%x null result. Checking parent %x.", ns, parent.getNamespace());
              }
              // If result from this ns is null, look in the parent.
              result = parent._retrieve(key, makeOptionsForNestedRetrieve(options));
              if (debugParent) {
                if (result != null) {
                  Log.warning("Found result in parent");
                }
              }
            } else {
              // If we have a non-null value from this ns, and the vc mode is GREATEST
              // then the value that we already have is the best.
              // Otherwise for the LEAST case, look in the parent to see if it has a better result.
              if (vc.getMode() == VersionConstraint.Mode.LEAST) {
                ByteBuffer parentResult;

                if (debugParent) {
                  Log.warning("Non-null result, but mode LEAST. checking parent");
                }
                parentResult = parent._retrieve(key, makeOptionsForNestedRetrieve(options));
                if (parentResult != null) {
                  // if the parent had any valid result, then - by virtue of the fact
                  // that all parent versions are < child versions - the parent
                  // result is preferred
                  result = parentResult;
                  if (debugParent) {
                    Log.warning("Found result in parent");
                  }
                }
              }
            }

            if (result == null && options.getWaitMode() == WaitMode.WAIT_FOR && options.getVersionConstraint().getMax() > curSnapshot) {
              // Note that since we hold the readLock, a write cannot come
              // in while we add the pending wait for.
              addPendingWaitFor(key, options.getRetrievalOptions(), opUUID);
            }
            if (options.getVerifyIntegrity()) {
              result = verifyIntegrity(key, result);
            }
          }

          resultAndKey = new Pair<>(key, result);
          singleResultCallback.apply(resultAndKey);
          return null;
        };

        if (retrieveTrigger != null) {
          if (RetrieveTrigger.Callback.class.isAssignableFrom(retrieveTrigger.getClass())) {
            ((RetrieveTrigger.Callback) retrieveTrigger).retrieve(this, key, options, callback);
          } else {
            // special case single retrieval (for speed)
            ByteBuffer result;

            result = ((RetrieveTrigger.Direct) retrieveTrigger).retrieve(this, key, options);
            callback.apply(result);
          }
        } else {
          // TODO (OPTIMUS-35616): the namespace read lock remains held here while performing result processing; it
          // should be released earlier to reduce contention.
          _retrieve(key, options, callback);
        }
      }
    } finally {
      if (shouldAcquireReadLock) {
        readLock.unlock();
      }
    }
  }

  /**
   * Executes a retrieve of multiple {@link DHTKey}s using the trigger.
   * <p>
   * If the trigger implements {@link RetrieveTrigger.Direct} then the trigger's {@code retrieve} method will be be
   * invoked on the calling thread, which will block until that method completes and then invoke the {@code callback}
   * with the result of that retrieval. The namespace's read lock will be acquired before invoking the trigger.
   * <p>
   * Alternatively, if the trigger implements {@link RetrieveTrigger.Callback} then the trigger's {@code retrieve}
   * method will be invoked on the calling thread and the {@code callback} will be passed through to the trigger. In
   * this case the read lock is <strong>not</strong> acquired before invoking {@code retrieve}. It is the responsibility
   * of the trigger both to perform its own lock management of the namespace lock, and also to later invoke the
   * {@code callback}, which may happen from any thread of its choosing. As such there is no guarantee that the
   * operation will have been completed when the {@code retrieve} call returns.
   *
   * @param keys     The keys to retrieve
   * @param options  {@link RetrievalOptions} for the retrieve operation
   * @param callback The operation to perform on the result of the retrieve.
   */
  private void retrieveViaTrigger(DHTKey[] keys, SSRetrievalOptions options,
      RetrieveCallback<ByteBuffer[], Void> callback) {
    if (retrieveTrigger == null) {
      throw new IllegalArgumentException("Cannot retrieveViaTrigger when retrieveTrigger is null");
    }
    if (keys.length > 1) {
      // Note that we do not acquire the read lock before invoking a Callback trigger as those are expected to perform
      // their own lock management.
      if (RetrieveTrigger.Callback.class.isAssignableFrom(retrieveTrigger.getClass())) {
        ((RetrieveTrigger.Callback) retrieveTrigger).retrieve(this, keys, options, callback);
      } else {
        ByteBuffer[] results;

        readLock.lock();
        try {
          results = ((RetrieveTrigger.Direct) retrieveTrigger).retrieve(this, keys, options);
          callback.apply(results);
        } finally {
          readLock.unlock();
        }
      }
    } else {
      RetrieveCallback<ByteBuffer, Void> singleKeyCallback;

      singleKeyCallback = (ByteBuffer result) -> {
        ByteBuffer[] results;

        results = new ByteBuffer[1];
        results[0] = result;
        return callback.apply(results);
      };

      if (RetrieveTrigger.Callback.class.isAssignableFrom(retrieveTrigger.getClass())) {
        ((RetrieveTrigger.Callback) retrieveTrigger).retrieve(this, keys[0], options, singleKeyCallback);
      } else {
        // special case single retrieval (for speed)
        ByteBuffer result;

        readLock.lock();
        try {
          result = ((RetrieveTrigger.Direct) retrieveTrigger).retrieve(this, keys[0], options);
          singleKeyCallback.apply(result);
        } finally {
          readLock.unlock();
        }
      }
    }
  }

  /**
   * Adjust options for retrieval from parent. I.e. consider minVersion or creationTime as
   * appropriate. Note that there is some looseness to the semantics for versioning
   * that relies on clock synchronization.
   *
   * @param options
   * @return
   */
  private InternalRetrievalOptions makeOptionsForNestedRetrieve(InternalRetrievalOptions options) {
    VersionConstraint oldVC;

    // When looking in parents, we only want to see what's present. We don't want
    // to post any pending wait fors as it is impossible - by design - for a child namespace
    // to observe values stored in parents after child creation. Children are clones,
    // and parents/children are distinct after the creation.
    options = options.waitMode(WaitMode.GET);

    oldVC = options.getVersionConstraint();
    switch (nsOptions.getVersionMode()) {
    case SEQUENTIAL:
      throw new RuntimeException("Panic: parent not supported/expected for version mode of SEQUENTIAL");
    case SINGLE_VERSION:
      // for write-once namespaces, we use the system time to limit retrieval in the parent
      // to values that were stored before the child creation
      if (debugParent) {
        Log.warning(oldVC);
        Log.warning(options.versionConstraint(oldVC.maxCreationTime(
            Math.min(oldVC.getMaxCreationTime(), nsProperties.getCreationTime()))).getVersionConstraint());
      }
      return options.versionConstraint(
          oldVC.maxCreationTime(Math.min(oldVC.getMaxCreationTime(), nsProperties.getCreationTime())));
    case CLIENT_SPECIFIED:
    case SYSTEM_TIME_MILLIS:
    case SYSTEM_TIME_NANOS:
      // for cases of system-specified versions, we want to ignore values in the parent
      // that were stored after the child namespace was created
      return options.versionConstraint(oldVC.max(Math.min(oldVC.getMax(), nsProperties.getMinVersion() - 1)));
    default:
      throw new RuntimeException("Panic");
    }
  }

  private ByteBuffer verifyIntegrity(DHTKey key, ByteBuffer result) {
    try {
      ValueUtil.verifyChecksum(result);
      return result;
    } catch (CorruptValueException cve) {
      Log.warningAsync(String.format("Corrupt: %s", key));
      return ValueUtil.corruptValue;
    }
  }

  private void addPendingWaitFor(DHTKey key, RetrievalOptions options, UUIDBase opUUID) {
    Set<PendingWaitFor> pendingWaitForSet;

    if (debugWaitFor) {
      Log.warningAsyncf("addPendingWaitFor %s %s %s", key, options, opUUID);
    }
    pendingWaitForSet = pendingWaitFors.get(key);
    if (pendingWaitForSet == null) {
      Set<PendingWaitFor> prev;

      pendingWaitForSet = new ConcurrentSkipListSet<>();
      prev = pendingWaitFors.putIfAbsent(key, pendingWaitForSet);
      if (prev != null) {
        pendingWaitForSet = prev;
      }
    }
    pendingWaitForSet.add(new PendingWaitFor(key, options, opUUID));
  }

  protected ByteBuffer _retrieve(DHTKey key, RetrievalOptions options) {
    return _retrieve(key, new InternalRetrievalOptions(options));
  }

  protected ByteBuffer[] _retrieve(DHTKey[] keys, InternalRetrievalOptions options) {
    int[] segmentNumbers;
    Triple<DHTKey, Integer, Integer>[] keysSegmentNumbersAndIndices;
    ByteBuffer[] sortedResults;
    ByteBuffer[] unsortedResults;

    if (debugParent) {
      Log.warningAsyncf("_retrieve %x batch size %d", ns, keys.length);
    }
    segmentNumbers = new int[keys.length];
    for (int i = 0; i < keys.length; i++) {
      segmentNumbers[i] = getSegmentNumber(keys[i], options.getVersionConstraint());
      if (debugParent) {
        Log.warningAsyncf("_retrieve %x %s segmentNumber %d", ns, KeyUtil.keyToString(keys[i]), segmentNumbers[i]);
      }
    }

    // Sort so that we group segment access. Not always useful,
    // but can become critical for large retrievals that span multiple segments
    keysSegmentNumbersAndIndices = new Triple[keys.length];
    for (int i = 0; i < keysSegmentNumbersAndIndices.length; i++) {
      keysSegmentNumbersAndIndices[i] = new Triple<>(keys[i], segmentNumbers[i], i);
    }
    Arrays.sort(keysSegmentNumbersAndIndices, KeyAndSegmentComparator.mostRecentSegmentsFirst);

    sortedResults = _retrieve(keysSegmentNumbersAndIndices, options);

    // We fix up the ordering here. It would be possible to avoid this
    // by constructing the unsorted results inside of the above _retrieve() call.
    // We currently avoid that to avoid leaking the notion of sorting/indexing changes
    // into that method.
    unsortedResults = new ByteBuffer[sortedResults.length];
    for (int i = 0; i < sortedResults.length; i++) {
      unsortedResults[keysSegmentNumbersAndIndices[i].getV3()] = sortedResults[i];
    }
    return unsortedResults;
  }

  /**
   * Sort by segment number. Break ties using original index.
   */
  private static class KeyAndSegmentComparator implements Comparator<Triple<DHTKey, Integer, Integer>> {
    private final int order;

    static final KeyAndSegmentComparator mostRecentSegmentsFirst = new KeyAndSegmentComparator(-1);
    static final KeyAndSegmentComparator leastRecentSegmentsFirst = new KeyAndSegmentComparator(1);

    private KeyAndSegmentComparator(int order) {
      this.order = order;
    }

    @Override
    public int compare(Triple<DHTKey, Integer, Integer> o1, Triple<DHTKey, Integer, Integer> o2) {
      int c;

      c = order * Integer.compare(o1.getV2(), o2.getV2());
      return c == 0 ? Integer.compare(o1.getV3(), o2.getV3()) : c;
    }
  }

  protected <T> T _retrieve(DHTKey key, InternalRetrievalOptions options, RetrieveCallback<ByteBuffer, T> callback) {
    return callback.apply(_retrieve(key, options));
  }

  protected ByteBuffer _retrieve(DHTKey key, InternalRetrievalOptions options) {
    return _retrieve(key, options, true);
  }

  protected ByteBuffer _retrieve(DHTKey key, InternalRetrievalOptions options, boolean verifySS) {
    int segmentNumber;
    VersionConstraint versionConstraint;
    int failedStore;

    if (debugParent) {
      Log.warningAsyncf("_retrieve %x %s", ns, key);
    }

    failedStore = 0;
    versionConstraint = options.getVersionConstraint();
    do {
      ByteBuffer result;

      segmentNumber = getSegmentNumber(key, versionConstraint);
      if (debugVersion) {
        Log.warningAsyncf("retrieve: %s", key);
        Log.warningAsyncf("RetrievalOptions: %s", options);
      }
      if (segmentNumber == IntCuckooConstants.noSuchValue) {
        return null;
      } else {
        readLock.lock();
        try {
          if (headSegment.getSegmentNumber() == segmentNumber) {
            // return getValueEntry(key).retrieve(options);
            if (debugSegments) {
              Log.warningAsync("Read from head segment");
            }
            result = retrieve(headSegment, key, options, verifySS);
            if (debugSegments) {
              Log.warningAsync("Done read from head segment");
            }
          } else {
            try {
              AbstractSegment segment;

              segment = getSegment(segmentNumber, readSegmentPrereadMode);
              if (debugSegments) {
                Log.warningAsync("Read from file segment");
              }
              result = retrieve(segment, key, options, verifySS);
              if (debugSegments) {
                Log.warningAsync("Done read from file segment");
                Log.warningAsyncf("result: %s", result);
              }
            } catch (IOException ioe) {
              peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
              Log.logErrorWarning(ioe);
              return null;
            }
          }
        } finally {
          readLock.unlock();
        }

        if (result != null) {
          // Double check that the result is valid
          if (debug) {
            Log.warningAsyncf("tpc %d", CCSSUtil.getStorageState(MetaDataUtil.getCCSS(result, 0)));
            Log.warningAsyncf("tpc %s %s",
                StorageProtocolUtil.storageStateValidForRead(nsOptions.getConsistencyProtocol(),
                    CCSSUtil.getStorageState(MetaDataUtil.getCCSS(result, 0))));
          }
          // FUTURE - this is a temporary workaround until the versioned storage is overhauled
          if (verifySS && !StorageProtocolUtil.storageStateValidForRead(nsOptions.getConsistencyProtocol(),
              MetaDataUtil.getStorageState(result, 0))) {
            switch (versionConstraint.getMode()) {
            case GREATEST:
              long newMaxVersion;

              newMaxVersion = MetaDataUtil.getVersion(result, 0) - 1;
              if (newMaxVersion < versionConstraint.getMin()) {
                return null;
              }
              versionConstraint = versionConstraint.max(newMaxVersion);
              break;
            case LEAST:
              long newMinVersion;

              newMinVersion = MetaDataUtil.getVersion(result, 0) + 1;
              if (newMinVersion > versionConstraint.getMax()) {
                return null;
              }
              versionConstraint = versionConstraint.min(newMinVersion);
              break;
            default:
              throw new RuntimeException("Panic");
            }
            options = options.versionConstraint(versionConstraint);
          } else {
            return result;
          }
        } else {
          return null;
        }
      }
    } while (failedStore++ < maxFailedStores);
    Log.warning("maxFailedStores exceeded");
    return null;
  }

  protected ByteBuffer[] _retrieve(Triple<DHTKey, Integer, Integer>[] keysSegmentNumbersAndIndices,
      InternalRetrievalOptions options) {
    ByteBuffer[] results;

    results = new ByteBuffer[keysSegmentNumbersAndIndices.length];
    for (int i = 0; i < keysSegmentNumbersAndIndices.length; i++) {
      if (debugParent) {
        Log.fineAsyncf("\t%s %d %d", KeyUtil.keyToString(keysSegmentNumbersAndIndices[i].getV1()),
            keysSegmentNumbersAndIndices[i].getV2(), keysSegmentNumbersAndIndices[i].getV3());
      }
      if (debugVersion) {
        Log.warningAsyncf("retrieve: %s", keysSegmentNumbersAndIndices[i].getV1());
        Log.warningAsyncf("RetrievalOptions: %s", options);
      }
      if (keysSegmentNumbersAndIndices[i].getV2() == IntCuckooConstants.noSuchValue) {
        results[i] = null;
      }
    }
    readLock.lock();
    try {
      for (int i = 0; i < keysSegmentNumbersAndIndices.length; i++) {
        if (keysSegmentNumbersAndIndices[i].getV2() != IntCuckooConstants.noSuchValue) {
          if (headSegment.getSegmentNumber() == keysSegmentNumbersAndIndices[i].getV2()) {
            if (debugSegments) {
              Log.warningAsync("Read from head segment");
            }
            results[i] = retrieve(headSegment, keysSegmentNumbersAndIndices[i].getV1(), options, true);
            if (debugSegments) {
              Log.warningAsync("Done read from head segment");
            }
          } else {
            try {
              AbstractSegment segment;

              segment = getSegment(keysSegmentNumbersAndIndices[i].getV2(), readSegmentPrereadMode);
              if (debugSegments) {
                Log.warningAsync("Read from file segment");
              }
              results[i] = retrieve(segment, keysSegmentNumbersAndIndices[i].getV1(), options, true);
              if (debugSegments) {
                Log.warningAsync("Done read from file segment");
                Log.warningAsync("result: " + results[i]);
              }
            } catch (IOException ioe) {
              peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
              Log.logErrorWarning(ioe);
              return null;
            }
          }
        }
      }
    } finally {
      readLock.unlock();
    }

    for (int i = 0; i < results.length; i++) {
      if (results[i] != null) {
        // Double check that the result is valid
        if (debug) {
          Log.warningAsyncf("tpc %d", CCSSUtil.getStorageState(MetaDataUtil.getCCSS(results[i], 0)));
          Log.warningAsyncf("tpc %s", StorageProtocolUtil.storageStateValidForRead(nsOptions.getConsistencyProtocol(),
              CCSSUtil.getStorageState(MetaDataUtil.getCCSS(results[i], 0))));
        }
        if (!StorageProtocolUtil.storageStateValidForRead(nsOptions.getConsistencyProtocol(),
            MetaDataUtil.getStorageState(results[i], 0))) {
          // If we detect a failed store, simply revert to the single key retrieval code
          // which is capable of handling this situation
          results[i] = _retrieve(keysSegmentNumbersAndIndices[i].getV1(), options);
        }
      }
    }

    return results;
  }

  /**
   * Call segment.retrieve() verifying storage state if necessary, and re-issuing a
   * modified call (with a verification request) if verification fails
   *
   * @param segment
   * @param key
   * @param options
   * @param verifySS
   * @return
   */
  private ByteBuffer retrieve(AbstractSegment segment, DHTKey key, InternalRetrievalOptions options, boolean verifySS) {
    try {
      ByteBuffer result;

      result = segment.retrieve(key, options, false);
      if (result != null && verifySS && verifyStorageState && !storageStateValid(result)) {
        result = segment.retrieve(key, options.cpSSToVerify(nsOptions.getConsistencyProtocol()), true);
      }
      return result;
    } catch (RuntimeException re) {
      Log.warningf("Due to exception %s, removing %d from fileSegmentCache", re, segment.getSegmentNumber());
      fileSegmentCache.invalidate(segment.getSegmentNumber());
      throw re;
    }
  }

  private boolean storageStateValid(ByteBuffer result) {
    return StorageProtocolUtil.storageStateValidForRead(nsOptions.getConsistencyProtocol(),
        MetaDataUtil.getStorageState(result, 0));
  }

  public void cleanupPendingWaitFors() {
    if (debugWaitFor) {
      Log.warningAsync("Cleaning pending waitfors");
    }
    for (Map.Entry<DHTKey, Set<PendingWaitFor>> entry : pendingWaitFors.entrySet()) {
      for (PendingWaitFor pendingWaitFor : entry.getValue()) {
        ActiveProxyRetrieval waiter;

        waiter = activeRetrievals.get(pendingWaitFor.getOpUUID());
        if (waiter == null) {
          if (debugWaitFor) {
            Log.fineAsyncf("No active retrieval for %s", pendingWaitFor.getKey());
          }
          entry.getValue().remove(pendingWaitFor);
        } else {
          Log.fineAsyncf("Found active retrieval for %s", pendingWaitFor.getKey());
        }
      }
      if (entry.getValue().size() == 0) {
        pendingWaitFors.remove(entry.getKey());
      }
    }
  }

  private int getSegmentNumber(DHTKey key, VersionConstraint vc) {
    metaReadLock.lock();
    try {
      int rawSegmentNumber;
      int segmentNumber;

      rawSegmentNumber = valueSegments.get(key);
      if (debugSegments) {
        Log.warning("valueSegments.get " + key + " " + rawSegmentNumber);
      }
      if (rawSegmentNumber < 0) {
        if (rawSegmentNumber != IntCuckooConstants.noSuchValue) {
          OffsetList offsetList;

          offsetList = offsetListStore.getOffsetList(-rawSegmentNumber);
          segmentNumber = offsetList.getOffset(vc, null);
          if (segmentNumber < 0) {
            if (segmentNumber == OffsetList.NO_MATCH_FOUND) {
              segmentNumber = IntCuckooConstants.noSuchValue;
            } else {
              throw new RuntimeException("Unexpected segmentNumber: " + segmentNumber);
            }
          }
        } else {
          segmentNumber = IntCuckooConstants.noSuchValue;
        }
      } else {
        segmentNumber = rawSegmentNumber;
      }
      if (debugSegments) {
        Log.warning("offsetList segmentNumber: ", rawSegmentNumber);
      }
      return segmentNumber;
    } finally {
      metaReadLock.unlock();
    }
  }

  private long[] segmentOldestVersion(int segmentNumber, DHTKey key) {
    return segmentVersion(segmentNumber, key, oldestVersionOptions);
  }

  private long[] segmentNewestVersion(int segmentNumber, DHTKey key) {
    return segmentVersion(segmentNumber, key, newestVersionOptions);
  }

  private long[] segmentVersion(int segmentNumber, DHTKey key, InternalRetrievalOptions retrievalOptions) {
    try {
      AbstractSegment segment;

      // FUTURE - reduce disk accesses
      segment = getSegmentChecked(segmentNumber);
      return segmentVersionAndStorageTime(segment, key, retrievalOptions);
    } catch (IOException ioe) {
      peerHealthMonitor.addSelfAsSuspect(PeerHealthIssue.StorageError);
      Log.logErrorWarning(ioe);
      throw new RuntimeException(ioe);
    }
  }

  private long[] segmentVersionAndStorageTime(AbstractSegment segment, DHTKey key,
      InternalRetrievalOptions retrievalOptions) {
    ByteBuffer result;
    long[] versionAndStorageTime;

    versionAndStorageTime = new long[2];
    if (debugSegments) {
      Log.warning("Read segmentVersion");
    }
    result = retrieve(segment, key, retrievalOptions, true);
    if (debugSegments) {
      Log.warning("Done read segmentVersion");
      Log.warning("result: " + result);
    }
    if (result != null) {
      versionAndStorageTime[VERSION_INDEX] = MetaDataUtil.getVersion(result, 0);
      versionAndStorageTime[STORAGE_TIME_INDEX] = MetaDataUtil.getCreationTime(result, 0);
    } else {
      versionAndStorageTime[VERSION_INDEX] = noSuchVersion;
      versionAndStorageTime[STORAGE_TIME_INDEX] = 0;
    }
    return versionAndStorageTime;
  }

  // FUTURE - getSegment() implementation needs to be updated
  private WritableSegmentBase getSegment(int segmentNumber, SegmentPrereadMode segmentPrereadMode) throws IOException {
    if (segmentNumber == headSegment.getSegmentNumber()) {
      return headSegment;
    } else {
      switch (nsOptions.getStorageType()) {
      case RAM:
        return ramSegments.get(segmentNumber);
      case FILE:
        return getFileSegment(segmentNumber, segmentPrereadMode);
      default:
        throw new RuntimeException("Panic");
      }
    }
  }

  // FUTURE - getSegment() implementation needs to be updated
  private WritableSegmentBase getSegmentChecked(int segmentNumber) throws IOException {
    if (headSegment != null && segmentNumber == headSegment.getSegmentNumber()) {
      return headSegment;
    } else {
      switch (nsOptions.getStorageType()) {
      case RAM:
        return ramSegments.get(segmentNumber);
      case FILE:
        return getFileSegment(segmentNumber, readSegmentPrereadMode);
      default:
        throw new RuntimeException("Panic");
      }
    }
  }

  private String segmentCreationTimeString(int segmentNumber) {
    if (deletedSegments.contains(segmentNumber)) {
      return "deleted";
    } else {
      if (segmentNumber < nextSegmentID.get()) {
        switch (nsOptions.getStorageType()) {
        case RAM:
          return "RAM";
        case FILE:
          return FileSegment.segmentCreationTime(nsDir, segmentNumber);
        default:
          throw new RuntimeException("Panic");
        }
      } else {
        throw new RuntimeException(
            String.format("Segment number %d is outside of current head segment %d", segmentNumber,
                nextSegmentID.get()));
      }
    }
  }

  private boolean segmentExists(int segmentNumber) {
    if (deletedSegments.contains(segmentNumber)) {
      return false;
    } else {
      if (segmentNumber < nextSegmentID.get()) {
        boolean exists;

        switch (nsOptions.getStorageType()) {
        case RAM:
          exists = true;
          break;
        case FILE:
          exists = fileSegmentExists(segmentNumber);
          break;
        default:
          throw new RuntimeException("Panic");
        }
        if (!exists) {
          deletedSegments.add(segmentNumber);
        }
        return exists;
      } else {
        return false;
      }
    }
  }

  private boolean fileSegmentExists(int segmentNumber) {
    return FileSegment.fileForSegment(nsDir, segmentNumber).exists();
  }

  // FUTURE - getFileSegment() implementation needs to be updated
  private FileSegment getFileSegment(int segmentNumber, SegmentPrereadMode segmentPrereadMode) throws IOException {
    try {
      // FUTURE - reduce usage of ReadWrite
      return fileSegmentCache.get(segmentNumber,
          new SegmentLoader(segmentNumber, FileSegmentLoadMode.ReadWrite, segmentPrereadMode));
    } catch (ExecutionException e) {
      throw new IOException(e);
    }
  }

  private FileSegment loadFileSegment(int segmentNumber, FileSegmentLoadMode fileSegmentLoadMode,
      SegmentPrereadMode segmentPrereadMode) throws IOException {
    try {
      return _loadFileSegment(segmentNumber, fileSegmentLoadMode, segmentPrereadMode);
    } catch (IOException | RuntimeException e) {
      Exception lastE;
      int attemptIndex;

      lastE = e;
      attemptIndex = 1;
      Log.logErrorWarning(e);
      Log.warning(
          String.format("Treating as non fatal. %s ns %x segmentNumber %d attemptIndex %d", e, ns, segmentNumber,
              attemptIndex));
      while (attemptIndex < fileSegmentAttempts) {
        try {
          return _loadFileSegment(segmentNumber, fileSegmentLoadMode, segmentPrereadMode);
        } catch (IOException | RuntimeException e2) {
          lastE = e2;
          Log.warning(
              String.format("Treating as non fatal. %s ns %x segmentNumber %d attemptIndex %d", e2, ns, segmentNumber,
                  attemptIndex));
        }
        ThreadUtil.sleep(fileSegmentAttemptDelayMillis);
        attemptIndex++;
      }
      if (lastE instanceof RuntimeException) {
        throw (RuntimeException) lastE;
      } else {
        throw (IOException) lastE;
      }
    }
  }

  private FileSegment _loadFileSegment(int segmentNumber, FileSegmentLoadMode fileSegmentLoadMode,
      SegmentPrereadMode segmentPrereadMode) throws IOException {
    FileSegment fileSegment;

    Log.infof("_loadFileSegment %d %s %s", segmentNumber, fileSegmentLoadMode, segmentPrereadMode);
    // FUTURE - this implementation touches disk more than we need to
    try {
      if (fileSegmentLoadMode == FileSegmentLoadMode.ReadWrite) {
        FileSegment.SyncMode syncMode;

        syncMode = nsOptions.getStorageType() == StorageType.FILE_SYNC ?
            FileSegment.SyncMode.Sync :
            FileSegment.SyncMode.NoSync;
        fileSegment = FileSegment.openForDataUpdate(nsDir, segmentNumber, nsOptions.getSegmentSize(), syncMode,
            nsOptions, segmentIndexLocation, segmentPrereadMode);
      } else {
        if (fileSegmentLoadMode == FileSegmentLoadMode.ReadIndexOnly) {
          fileSegment = FileSegment.openReadIndexOnly(nsDir, segmentNumber, nsOptions.getSegmentSize(), nsOptions);
        } else {
          fileSegment = FileSegment.openReadOnly(nsDir, segmentNumber, nsOptions.getSegmentSize(), nsOptions);
        }
      }
    } catch (IOException | OutOfMemoryError e) {
      boolean oom;

      if (e instanceof IOException) {
        oom = e.getCause() != null && (e.getCause() instanceof OutOfMemoryError);
      } else {
        oom = true;
      }
      if (oom) {
        Log.warning("OOM attempting to open mapped file. Calling gc and finalization.");
        finalization.forceFinalization(0);
        Log.warning("GC & finalization complete.");
        fileSegment = FileSegment.openReadOnly(nsDir, segmentNumber, nsOptions.getSegmentSize(), nsOptions);
      } else {
        throw e;
      }
    }
    if (debugSegments) {
      fileSegment.displayForDebug();
    }
    return fileSegment;
  }

  static NamespaceStore recoverExisting(long ns, File nsDir, NamespaceStore parent, StoragePolicy storagePolicy,
      MessageGroupBase mgBase, NodeRingMaster2 ringMaster,
      ConcurrentMap<UUIDBase, ActiveProxyRetrieval> activeRetrievals, SilverKingZooKeeperClient zk, String nsLinkBasePath,
      LinkCreationListener linkCreationListener, ReapPolicy reapPolicy, NamespaceProperties nsProperties)
      throws IOException {
    NamespaceStore nsStore;
    int numSegmentsToPreread;
    int numSegmentsToSkipPreread;
    int segmentsPrereadSkipped;

    nsStore = new NamespaceStore(ns, nsDir, NamespaceStore.DirCreationMode.DoNotCreateNSDir, nsProperties, parent,
        mgBase, ringMaster, true, activeRetrievals, reapPolicy);
    if (nsProperties.getOptions().getStorageType() != StorageType.RAM) {
      List<Integer> segmentNumbers;

      // FUTURE - presently only NamespaceFileSegmentCompactor is supported; allow others
      NamespaceFileSegmentCompactor.recoverOngoingCompaction(nsDir);
      segmentNumbers = FileUtil.numericFilesInDirAsSortedIntegerList(nsDir);
      if (segmentNumbers.size() > 0) {
        FileSegmentRecoverer fsr;
        int headSegmentNumber;
        int headSegmentNumberIndex;

        fsr = new FileSegmentRecoverer(nsDir, nsProperties);

        headSegmentNumberIndex = segmentNumbers.size() - 1;
        headSegmentNumber = segmentNumbers.get(headSegmentNumberIndex);
        segmentNumbers.remove(headSegmentNumberIndex);

        numSegmentsToPreread =
            (int) ((long) nsPrereadGB * (1024L * 1024L * 1024L) / (long) (nsProperties.getOptions().getSegmentSize()));
        numSegmentsToSkipPreread = segmentNumbers.size() - numSegmentsToPreread;
        segmentsPrereadSkipped = 0;
        Log.warningf("segmentsToPreread: %d", numSegmentsToPreread);

        for (int i : segmentNumbers) {
          FileSegment segment;
          SegmentPrereadMode segmentPrereadMode;

          segmentPrereadMode = segmentsPrereadSkipped < numSegmentsToSkipPreread ?
              SegmentPrereadMode.NoPreread :
              SegmentPrereadMode.Preread;
          segment = null;
          try {
            segment = fsr.recoverFullSegment(i, nsStore, segmentIndexLocation, segmentPrereadMode);
          } catch (Exception e) {
            Log.logErrorWarning(e, "Error recovering full segment " + i);
            Log.warningf("Attempting partial segment recovery of segment: %d %s", i, segmentPrereadMode);
            segment = fsr.readPartialSegment(i, false);
            Log.warning("Successfully read segment as partial segment: " + i);
            Log.warning("Persisting segment: " + i);
            try {
              segment.persist();
            } catch (IOException ioe) {
              throw new RuntimeException("Segment persists failed for recovered segment" + i, ioe);
            }
            Log.warning("Persisted segment: " + i);
            Log.warning("Resuming full segment recovery: " + i);
            segment = fsr.recoverFullSegment(i, nsStore, segmentIndexLocation, segmentPrereadMode);
          }
          if (segmentPrereadMode == SegmentPrereadMode.Preread && segment != null) {
            nsStore.fileSegmentCache.put(i, segment);
          }
          segmentsPrereadSkipped++;
        }
        // Check for partial recovery on last
        // FUTURE - Check for corruption
        // FUTURE - If full, then do full recovery?
        nsStore.setHeadSegment(fsr.recoverPartialSegment(headSegmentNumber, nsStore));
      } else {
        nsStore.createInitialHeadSegment();
      }
    } else {
      nsStore.initRAMSegments();
    }
    nsStore.initializeTriggers();
    return nsStore;
  }

  private void initializeTriggers() {
    if (putTrigger != null) {
      putTrigger.initialize(this);
    }
    if (retrieveTrigger != null && retrieveTrigger != putTrigger) {
      retrieveTrigger.initialize(this);
    }
  }

  // FUTURE - Consider deco
  public OpResult snapshot(long version) {
    writeLock.lock();
    try {
      if (version >= curSnapshot) {
        curSnapshot = version;
        return OpResult.SUCCEEDED;
      } else {
        return OpResult.INVALID_VERSION;
      }
    } finally {
      writeLock.unlock();
    }
  }

  ////////////////
  // convergence

  public boolean getChecksumTreeForLocal(UUIDBase uuid, ConvergencePoint targetCP, ConvergencePoint sourceCP,
      MessageGroupConnection connection, byte[] originator, RingRegion region, IPAndPort replica, int timeoutMillis) {
    ActiveRegionSync ars;

    ars = new ActiveRegionSync(this, checksumTreeServer, mgBase,
        new ChecksumTreeRequest(targetCP, sourceCP, region, replica));
    Log.warningAsyncf("getChecksumTreeForLocal req uuid %s  ars uuid %s  %s %s %s %s", uuid, ars.getUUID(), targetCP,
        sourceCP, region, replica);
    activeRegionSyncs.put(ars.getUUID(), ars);
    try {
      // Forward request to remote
      ars.startSync();
      // Wait for completion
      // Pass result back
      return ars.waitForCompletion(timeoutMillis, TimeUnit.MILLISECONDS);
    } finally {
      activeRegionSyncs.remove(ars.getUUID());
    }
  }

  public void getChecksumTreeForRemote(UUIDBase uuid, ConvergencePoint targetCP, ConvergencePoint sourceCP,
      MessageGroupConnection connection, byte[] originator, RingRegion region) {
    checksumTreeServer.getChecksumTree(uuid, targetCP, sourceCP, connection, originator, region);
  }

  public void incomingChecksumTree(UUIDBase uuidBase, ChecksumNode remoteTree, ConvergencePoint cp,
      MessageGroupConnection connection) {
    ActiveRegionSync ars;

    Log.warningAsyncf("incomingChecksumTree %s", uuidBase);
    ars = activeRegionSyncs.get(uuidBase);
    if (ars == null) {
      Log.warningf("Ignoring unexpected incoming checksum tree %x %s", ns, uuidBase.toString());
    } else {
      ars.incomingChecksumTree(cp, remoteTree, connection);
    }
  }

  public void incomingSyncRetrievalResponse(MessageGroup message) {
    message = message.ensureArrayBacked();
    ActiveRegionSync._incomingSyncRetrievalResponse(message);
  }

  public void handleSecondarySync(MessageGroup message) {
    List<StorageValueAndParameters> svpList;

    if (debug) {
      Log.warningAsyncf("incomingSyncRetrievalResponse");
    }
    svpList = new ArrayList<>();
    for (MessageGroupRetrievalResponseEntry entry : message.getRetrievalResponseValueKeyIterator()) {
      StorageValueAndParameters svp;

      svp = StorageValueAndParameters.createSVP(entry);
      if (svp != null) {
        svpList.add(svp);
      }
    }
    if (!svpList.isEmpty()) {
      put(svpList, emptyUserData, NullKeyedOpResultListener.instance);
    }
  }

  // LOCKING WILL NEED TO BE HANDLED IF BELOW IS EXPOSED
  //public Iterator<KeyValueChecksum> keyValueChecksumIterator(long version) {
  //    return new KeyValueChecksumIterator(version);
  ///}

  private class KeyValueChecksumIterator implements Iterator<KeyValueChecksum> {
    private final Iterator<DHTKeyIntEntry> valueSegmentEntries;

    private final RetrievalOptions retrievalOptions;

    private KeyValueChecksumIterator(long version) {
      valueSegmentEntries = valueSegments.iterator();
      retrievalOptions = OptionsHelper.newRetrievalOptions(RetrievalType.VALUE_AND_META_DATA, WaitMode.GET,
          VersionConstraint.greatest);
    }

    @Override
    public boolean hasNext() {
      return valueSegmentEntries.hasNext();
    }

    @Override
    public KeyValueChecksum next() {
      DHTKeyIntEntry vsEntry;
      ByteBuffer result;
      byte[] checksum;

      vsEntry = valueSegmentEntries.next();
      result = _retrieve(vsEntry, retrievalOptions);
      // FUTURE think about above in light of new retrieval grouping
      // checksum = new byte[checksumType.length()];

      checksum = MetaDataUtil.getChecksum(result, 0);
      // result.position(0);
      // result.get(checksum);
      return new KeyValueChecksum(vsEntry.getKey(), checksum);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // FUTURE - reminder: convergence implementation is currently only valid for SINGLE_VERSION

  /**
   * readLock() must be held while this is in use and readUnlock() must be called when complete
   */
  public Iterator<KeyAndVersionChecksum> keyAndVersionChecksumIterator(long minVersion, long maxVersion) {
    if (retrieveTrigger == null || !retrieveTrigger.subsumesStorage()) {
      if (debug) {
        Log.warningAsync("KeyAndVersionChecksumIterator");
      }
      return new KeyAndVersionChecksumIterator(minVersion, maxVersion);
    } else {
      if (debug) {
        Log.warningAsync("KeyAndVersionChecksumIteratorForTrigger");
      }
      return new KeyAndVersionChecksumIteratorForTrigger();
    }
  }

  private class KeyAndVersionChecksumIterator implements Iterator<KeyAndVersionChecksum> {
    private final Iterator<DHTKeyIntEntry> valueSegmentEntries;
    private KeyAndVersionChecksum next;

    //private final RetrievalOptions retrievalOptions;

    private KeyAndVersionChecksumIterator(long minVersion, long maxVersion) {
      valueSegmentEntries = valueSegments.iterator();
      //retrievalOptions = OptionsHelper.newRetrievalOptions(RetrievalType.VALUE_AND_META_DATA, WaitMode.GET,
      //        new VersionConstraint(minVersion, maxVersion, Mode.GREATEST));
      //        //VersionConstraint.greatest);
      moveToNext();
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    private void moveToNext() {
      KeyAndVersionChecksum _next;

      _next = null;
      while (_next == null && valueSegmentEntries.hasNext()) {
        DHTKeyIntEntry vsEntry;
        long checksum;
        boolean validChecksum;

        vsEntry = valueSegmentEntries.next();
        if (nsProperties.getOptions().isWriteOnce()) {
          checksum = 0;
          validChecksum = true;
        } else {
          MultiVersionChecksum _checksum;

          try {
            _checksum = getVersionChecksum(vsEntry.getKey());
          } catch (RuntimeException re) {
            re.printStackTrace();
            _checksum = null;
          }
          if (_checksum != null) {
            checksum = _checksum.getLongChecksum();
            validChecksum = true;
          } else {
            checksum = 0;
            validChecksum = false;
          }
        }
        if (validChecksum) {
          _next = new KeyAndVersionChecksum(vsEntry.getKey(), checksum, vsEntry.getValue());
        }
      }
      next = _next;
    }

    @Override
    public KeyAndVersionChecksum next() {
      KeyAndVersionChecksum _next;

      _next = next;
      moveToNext();
      return _next;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  // simplified - doesn't support multi version as multi-version convergence is not yet fully implemented
  private class KeyAndVersionChecksumIteratorForTrigger implements Iterator<KeyAndVersionChecksum> {
    private final Iterator<DHTKey> keyIterator;

    private KeyAndVersionChecksumIteratorForTrigger() {
      keyIterator = retrieveTrigger.keyIterator();
    }

    @Override
    public boolean hasNext() {
      return keyIterator.hasNext();
    }

    @Override
    public KeyAndVersionChecksum next() {
      DHTKey key;

      key = keyIterator.next();
      if (key != null) {
        return new KeyAndVersionChecksum(key, 0, 0);
      } else {
        return null;
      }
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private MultiVersionChecksum getVersionChecksum(DHTKey key) {
    if (retrieveTrigger != null) {
      throw new RuntimeException("panic"); // Not for use with triggers
    } else {
      int segmentNumber;

      segmentNumber = valueSegments.get(key);
      if (segmentNumber >= 0) {
        MultiVersionChecksum checksum;
        WritableSegmentBase segment;
        long latestVersion;

        try {
          segment = getSegment(segmentNumber, SegmentPrereadMode.Preread);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
        int offset;

        offset = segment.getRawOffset(key);
        if (offset < 0) {
          throw new RuntimeException("Unexpected offset < 0: " + key + " " + offset + " " + segmentNumber);
        }
        latestVersion = segment.getVersion(offset);
        checksum = new MultiVersionChecksum();
        checksum.addKey(key);
        checksum.addVersionAndStorageTime(latestVersion, 0);
        return checksum;
        //return MultiVersionChecksum.fromKey(key);
      } else {
        OffsetList offsetList;

        offsetList = offsetListStore.getOffsetList(-segmentNumber);
        //return offsetList.getMultiVersionChecksum();
        // FIXME - TEMPORARY - ONLY CONSIDER THE MOST RECENT VALUE
        // FOR CONVERGENCE
        MultiVersionChecksum checksum;
        long latestVersion;

        latestVersion = offsetList.getLatestVersion();
        if (latestVersion >= 0) {
          checksum = new MultiVersionChecksum();
          checksum.addKey(key);
          checksum.addVersionAndStorageTime(latestVersion, 0);
          return checksum;
        } else {
          return null;
        }
      }
    }
  }

  public void readLockAll() {
    readLock.lock();
    metaReadLock.lock();
  }

  public void readUnlockAll() {
    metaReadLock.unlock();
    readLock.unlock();
  }

  public void writeLockAll() {
    writeLock.lock();
    metaWriteLock.lock();
  }

  public void writeUnlockAll() {
    metaWriteLock.unlock();
    writeLock.unlock();
  }

  ////////////////////
  // Retention

  private final ReapImplState reapImplState = new ReapImplState();
  private final CompactAndDeleteImplState cdImplState = new CompactAndDeleteImplState();

  void initializeReapImplState() {
    if (!reapImplState.isClear()) {
      throw new RuntimeException("Can't initialize state. Not clear.");
    } else {
      int nextSegmentNumber;
      ValueRetentionState vrState;

      nextSegmentNumber = headSegment.getSegmentNumber();
      vrState = vrp.createInitialState();
      reapImplState.initialize(nextSegmentNumber, vrState);
    }
  }

  private ReapPhase reapPhase = ReapPhase.reap;

  public void setReapPhase(ReapPhase reapPhase) {
    if (this.reapPhase == reapPhase) {
      throw new RuntimeException("Phase already " + reapPhase);
    } else {
      this.reapPhase = reapPhase;
    }
    if (reapPolicy.verboseReapPhase()) {
      Log.warningAsyncf("reapPhase: %s %s", this.reapPhase, Long.toHexString(ns));
    }
  }

  private static class CompactAndDeleteImplState {
    private List<Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>>> reapResults;
    private Set<Integer> uncompactedSegments;

    void initialize(List<Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>>> reapResults,
        Set<Integer> uncompactedSegments) {
      if (!isClear()) {
        throw new RuntimeException("Can't initialize state. Not clear.");
      } else {
        this.reapResults = new LinkedList<>();
        this.reapResults.addAll(reapResults);
        this.uncompactedSegments = uncompactedSegments;
      }
    }

    void clear() {
      reapResults = null;
    }

    boolean isClear() {
      return reapResults == null;
    }

    Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> nextReapResult() {
      if (reapResults == null) {
        throw new RuntimeException("Unexpected nextReapResult() called on clear result");
      } else {
        if (reapResults.isEmpty()) {
          return null;
        } else {
          return reapResults.remove(0);
        }
      }
    }

    boolean hasReapResults() {
      if (reapResults == null) {
        throw new RuntimeException("Unexpected hasReapResults() called on clear result");
      } else {
        return !reapResults.isEmpty();
      }
    }

    public Set<Integer> getUncompactedSegments() {
      return uncompactedSegments;
    }
  }

  private static class ReapImplState {
    private int nextSegmentNumber;
    private ValueRetentionState vrState;
    private SortedMap<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> curReapResults;
    private Set<DHTKey> invalidatedKeys;
    private int lastSegmentRetainedBytes;
    private Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> lastSegmentReapResults;
    private Set<Integer>  uncompactedSegments;

    ReapImplState() {
      clear();
    }

    void initialize(int nextSegmentNumber, ValueRetentionState vrState) {
      if (!isClear()) {
        throw new RuntimeException("Can't initialize state. Not clear.");
      } else {
        this.nextSegmentNumber = nextSegmentNumber;
        this.vrState = vrState;
      }
    }

    void clear() {
      nextSegmentNumber = -1;
      vrState = null;
      curReapResults = new TreeMap<>();
      invalidatedKeys = new HashSet<>();
      lastSegmentRetainedBytes = 0;
      lastSegmentReapResults = null;
      uncompactedSegments = new HashSet<>();
    }

    boolean isClear() {
      return nextSegmentNumber < 0;
    }

    void decrementSegment() {
      --nextSegmentNumber;
    }

    int getNextSegmentNumber() {
      return nextSegmentNumber;
    }

    ValueRetentionState getVRState() {
      return vrState;
    }

    Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> getLastSegmentReapResults() {
      return lastSegmentReapResults;
    }

    int getLastSegmentRetainedBytes() {
      return lastSegmentRetainedBytes;
    }

    void setLastSegmentRetainedBytes(int lastSegmentRetainedBytes) {
      this.lastSegmentRetainedBytes = lastSegmentRetainedBytes;
    }

    private void setlastSegmentReapResults(
        Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> lastSegmentReapResults) {
      this.lastSegmentReapResults = lastSegmentReapResults;
    }

    void addReapResult(int segment, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>> result) {
      uncompactedSegments.remove(segment);
      if (curReapResults.get(segment) != null) {
        Log.warningf("Unexpected duplicate reap results for %d", segment);
      } else {
      curReapResults.put(segment, result);
    }
    }

    void addUncompactedSegment(int segment) {
      uncompactedSegments.add(segment);
    }

    Set<Integer> getUncompactedSegments() {
      return ImmutableSet.copyOf(uncompactedSegments);
    }

    List<Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>>> curReapResultsToList() {
      if (curReapResults == null) {
        return new ArrayList<>();
      } else {
        List<Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>>> resultsList;

        resultsList = new ArrayList<>(curReapResults.size());
        for (Map.Entry<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> entry :
            curReapResults.entrySet()) {
          resultsList.add(MapUtil.mapEntryToPair(entry));
        }
        return resultsList;
      }
    }
  }

  // ===== the methods below, startupReap(), liveReap(), forceReap(), are the top-level entry point to reap
  // We need to ensure that forceReap() and liveReap() do not occur concurrently

  public void startupReap() {
    if (reapPolicy.reapAllowed(reapPolicyState, this, reapPhase, true)) {
      _reap(headSegment.getSegmentNumber(), 0, vrp.createInitialState(), true, defaultCompactionThreshold);
      transitionToCompactAndDeletePhase();
      startupCompactAndDelete(defaultCompactionThreshold);
      transitionToReapPhase();
    } else {
      if (reapPolicy.verboseReap()) {
        Log.warningAsync("Startup reap is not allowed.");
      }
    }
  }

  public void liveReap() {
    Stopwatch sw;

    sw = new SimpleStopwatch();
    reapLock.lock();
    try {
      if (reapPolicy.reapAllowed(reapPolicyState, this, reapPhase, false)) {
        if (verboseReapLogInfo) {
          Log.warningf("liveReap() ns %x", ns);
        }
        if (reapPhase == ReapPhase.reap) {
          _liveReap(defaultCompactionThreshold);
        }
        // _leaveReap() may change the phase to compactAndDelete.
        // We catch this condition in a separate if statement to
        // allow execution to fall through immediately to that phase
        // when the change occurs.
        if (reapPhase == ReapPhase.compactAndDelete) {
          liveCompactAndDelete(defaultCompactionThreshold);
        }
      }
    } finally {
      reapLock.unlock();
      sw.stop();
      if (TracerFactory.isInitialized()) {
        TracerFactory.getTracer().onLocalReap(sw.getElapsedMillis());
      }
    }
  }

  public int[] forceReap(int startSegment, int endSegment, ValueRetentionPolicyImpl<?> vrp, ValueRetentionState state)
      throws IOException {
    Stopwatch sw;

    sw = new SimpleStopwatch();
    reapLock.lock();
    try {
      int segmentsReaped;
      Pair<Integer, Integer> segmentTrashedDeleted;

      // Pre-clean
      if (!reapImplState.isClear() || !cdImplState.isClear()) {
        Log.warningf(
            "Uncleaned state in forceReap(); Will do a liveReap(): reapImplStateClean=[%b], cdImplStateClean=[%b]",
            reapImplState.isClear(), cdImplState.isClear());
        liveReap();
      }
      if (reapPhase != ReapPhase.reap || !reapImplState.isClear() || !cdImplState.isClear()) {
        throw new IOException(String.format(
            "Unexpected state in forceReap(): reapPhase=[%s], reapImplStateClean=[%b], cdImplStateClean=[%b]",
            reapPhase, reapImplState.isClear(), cdImplState.isClear()));
      }

      // PRE-CONDITION: reapPhase == ReapPhase.reap && reapImplState.isClear() && cdImplState.isClear()
      // readLock.lock(); metaReadLock.lock();
      _reap(startSegment, endSegment, vrp, state, true, 0);
      segmentsReaped = reapImplState.curReapResults.size();
      transitionToCompactAndDeletePhase();
      // writeLock.lock(); metaWriteLock.lock();
      segmentTrashedDeleted = forceCompactAndDelete(0);
      return new int[] { segmentsReaped, segmentTrashedDeleted.getV1(), segmentTrashedDeleted.getV2() };
    } finally {
      reapLock.unlock();
      sw.stop();
      if (TracerFactory.isInitialized()) {
        TracerFactory.getTracer().onForceReap(sw.getElapsedMillis());
      }
    }
  }

  // ===== Helper functions used by the above entry-point methods

  /**
   * Empty trash and compaction directories. Run finalization if *either*
   * maxUnfinalizedDeletedBytes has been exceeded or minFinalizationIntervalMillis
   * has been exceeded.
   */
  private void updateAndCheckForceFinalization(int segmentsDeleted) {
    deletionsSinceFinalization += segmentsDeleted;
    long currUnfinalizedBytesEstimate = (long) deletionsSinceFinalization * (long) nsOptions.getSegmentSize();
    if (currUnfinalizedBytesEstimate > maxUnfinalizedDeletedBytes) {
      // Doesn't presently consider other sources of finalization
      deletionsSinceFinalization = 0;
      finalization.forceFinalization(0);
    } else {
      // If deletionsSinceFinalization doesn't trigger finalization, then consider minFinalizationIntervalMillis
      if (finalization.forceFinalization(minFinalizationIntervalMillis)) {
        deletionsSinceFinalization = 0;
      }
    }
  }

  private void transitionToCompactAndDeletePhase() {
    if (reapPhase != ReapPhase.reap) {
      throw new RuntimeException("Unexpected phase");
    }
    cdImplState.initialize(reapImplState.curReapResultsToList(), reapImplState.getUncompactedSegments());
    reapImplState.clear();
    setReapPhase(ReapPhase.compactAndDelete);
  }

  private void transitionToReapPhase() {
    if (reapPhase != ReapPhase.compactAndDelete) {
      throw new RuntimeException("Unexpected phase");
    }
    cdImplState.clear();
    setReapPhase(ReapPhase.reap);
  }

  public void _liveReap(double compactionThreshold) {
    int currentBatchSize;

    currentBatchSize = 0;
    if (reapImplState.isClear()) {
      initializeReapImplState();
    }
    while (reapPolicy.reapAllowed(reapPolicyState, this, reapPhase,
        false) && reapImplState.getNextSegmentNumber() >= 0 && currentBatchSize < reapPolicy.getBatchLimit(null)) {
      int segmentsReaped;

      segmentsReaped = _reap(reapImplState.getNextSegmentNumber(), reapImplState.getNextSegmentNumber(),
          reapImplState.getVRState(), false, compactionThreshold);
      currentBatchSize += segmentsReaped;
      reapImplState.decrementSegment();
      if (reapImplState.getNextSegmentNumber() >= 0 && segmentsReaped > 0) {
        ThreadUtil.sleep(reapPolicy.getIdleReapPauseMillis());
      }
    }
    if (reapImplState.getNextSegmentNumber() < 0) {
      transitionToCompactAndDeletePhase();
    } else {
      Log.info("_liveReap() interrupted");
    }
  }

  private void startupCompactAndDelete(double compactionThreshold) {
    Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> nextReapResult;
    Set<Integer> deletedSegments;
    HashedSetMap<DHTKey, CompactorModifiedEntry> modifiedEntries;

    Log.warning("startupCompactAndDelete");
    deletedSegments = new HashSet<>();
    modifiedEntries = new HashedSetMap<>();
    nextReapResult = cdImplState.nextReapResult();
    while (nextReapResult != null) {
      Pair<Set<Integer>, HashedSetMap<DHTKey, CompactorModifiedEntry>> segmentCompactionResults;

      segmentCompactionResults = compactAndDelete(nextReapResult.getV1(), nextReapResult.getV2(), reapPolicy.verboseReap(),
          compactionThreshold);
      deletedSegments.addAll(segmentCompactionResults.getV1());
      modifiedEntries.addAll(segmentCompactionResults.getV2());
      nextReapResult = cdImplState.nextReapResult();
    }
    flushCompaction(deletedSegments, modifiedEntries);
  }

  private void liveCompactAndDelete(double compactionThreshold) {
    int currentBatchSize;
    Set<Integer> deletedSegments;
    HashedSetMap<DHTKey, CompactorModifiedEntry> modifiedEntries;
    boolean compactionCalled;

    Log.infof("liveCompactAndDelete");
    currentBatchSize = 0;
    compactionCalled = false;
    if (cdImplState.isClear()) {
      throw new RuntimeException("Unexpected cdImplState.isClear()");
    }
    deletedSegments = new HashSet<>();
    modifiedEntries = new HashedSetMap<>();
    while (reapPolicy.reapAllowed(reapPolicyState, this, reapPhase,
        false) && cdImplState.hasReapResults() && currentBatchSize < reapPolicy.getBatchLimit(null)) {
      Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> nextReapResult;

      nextReapResult = cdImplState.nextReapResult();
      if (nextReapResult != null && nextReapResult.getV1() < getHeadSegmentNumber()) {
        Pair<Set<Integer>, HashedSetMap<DHTKey, CompactorModifiedEntry>> segmentCompactionResults;

        compactionCalled = true;
        segmentCompactionResults = compactAndDelete(nextReapResult.getV1(), nextReapResult.getV2(), reapPolicy.verboseReap(),
            compactionThreshold);
        ++currentBatchSize;
        deletedSegments.addAll(segmentCompactionResults.getV1());
        modifiedEntries.addAll(segmentCompactionResults.getV2());
        ThreadUtil.sleep(reapPolicy.getIdleReapPauseMillis());
      }
    }
    if (compactionCalled) {
      flushCompaction(deletedSegments, modifiedEntries);
    }
    if (!cdImplState.hasReapResults()) {
      reapPolicyState.fullReapComplete(this);
      transitionToReapPhase();
    } else {
      Log.info("liveCompactAndDelete() interrupted");
      }
    Log.infof("out liveCompactAndDelete");
  }

  private Pair<Integer, Integer> forceCompactAndDelete(double compactionThreshold) throws IOException {
    Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> nextReapResult;
    Set<Integer> deletedSegments;
    HashedSetMap<DHTKey, CompactorModifiedEntry> modifiedEntries;
    int segmentsDeleted;

    segmentsDeleted = 0;
    if (cdImplState.isClear()) {
      throw new IOException("Unexpected cdImplState.isClear()");
    }
    deletedSegments = new HashSet<>();
    modifiedEntries = new HashedSetMap<>();
    writeLockAll();
    try {
    while (cdImplState.hasReapResults()) {
      nextReapResult = cdImplState.nextReapResult();
      if (nextReapResult != null) {
          Pair<Set<Integer>, HashedSetMap<DHTKey, CompactorModifiedEntry>> segmentCompactionResults;
        int curSegment;

        curSegment = nextReapResult.getV1();
          segmentCompactionResults = compactAndDelete(curSegment, nextReapResult.getV2(), true, compactionThreshold);
          deletedSegments.addAll(segmentCompactionResults.getV1());
          modifiedEntries.addAll(segmentCompactionResults.getV2());
          if (segmentCompactionResults.getV1().size() > 0) {
            segmentsDeleted += segmentCompactionResults.getV1().size();
        } else {
          Log.warningf(
                "Fail to compact/delete Segment [%d] during forceCompactAndDelete() for key purge; Check the error " + "message above",
              curSegment);
        }
      }
    }
      flushCompaction(deletedSegments, modifiedEntries);
    } finally {
      writeUnlockAll();
    }
    if (!cdImplState.hasReapResults()) {
      transitionToReapPhase();
    } else {
      Log.warning("forceCompactAndDelete() interrupted");
      throw new IOException("forceCompactAndDelete() interrupted");
    }

    return Pair.of(0, segmentsDeleted); // FUTURE - trash no longer used; remove pair
  }

  private int _reap(int startSegment, int endSegment, ValueRetentionState state, boolean verboseReap,
      double compactionThreshold) {
    return _reap(startSegment, endSegment, vrp, state, verboseReap, compactionThreshold);
  }

  private int _reap(int startSegment, int endSegment, ValueRetentionPolicyImpl<?> vrp, ValueRetentionState state,
      boolean verboseReap, double compactionThreshold) {
    int segmentsReaped;

    segmentsReaped = 0;
    if (verboseReap || verboseReapLogInfo) {
      Log.infoAsyncf("_reap ns %x %s [%d, %d]", ns, vrp, startSegment, endSegment);
    }
    if (vrp != null) {
      readLockAll();
      try {
        long curTimeNanos;

        curTimeNanos = systemTimeSource.absTimeNanos();
        switch (vrp.getImplementationType()) {
        case SingleReverseSegmentWalk:
          segmentsReaped += singleReverseSegmentWalk(vrp, state, curTimeNanos, startSegment, endSegment, verboseReap,
              compactionThreshold);
          break;
        case RetainAll:
          break;
        default:
          throw new RuntimeException(
              "Unsupported ValueRetentionPolicy ImplementationType: " + vrp.getImplementationType());
        }
      } finally {
        readUnlockAll();
      }
    }
    return segmentsReaped;
  }

  private <T extends ValueRetentionState> int singleReverseSegmentWalk(ValueRetentionPolicyImpl<?> vrp,
      T valueRetentionState, long curTimeNanos, int startSegment, int endSegment, boolean verboseReap,
      double compactionThreshold) {
    int segmentsReaped;

    segmentsReaped = 0;
    Log.finef("nsStore.singleReverseSegmentWalk %d %d", startSegment, endSegment);
    if (startSegment < endSegment) {
      throw new RuntimeException("singleReverseSegmentWalk startSegment < endSegment; order should be reverse");
    }
    for (int i = startSegment; i >= endSegment; i--) {
      Log.finef("nsStore.singleReverseSegmentWalk %d", i);
      if (segmentExists(i)) {
        Quintuple<CompactionCheckResult, Set<Integer>, Set<Integer>, Set<DHTKey>, Boolean> result;
        WritableSegmentBase segment;
        String ctime = segmentCreationTimeString(i);

        boolean isKeyLevelReap = false;

        ++segmentsReaped;
        try {
          Stopwatch sw;
          KeyLevelValueRetentionPolicyImpl segment_klvrp;
          ValueRetentionState segment_vrs;
          boolean crossSegmentCompactionEnabled;

          // cross segment compaction can only be efficiently implemented when lengths are indexed
          crossSegmentCompactionEnabled = FileSegmentStorageFormat.parse(nsOptions.getStorageFormat()).lengthsAreIndexed();
          
          sw = new SimpleStopwatch();

          // Determine the ValueRetentionPolicy and ValueRetentionState for key-level retention within a segment.
          // In trivial cases, the policy on the NamespaceStore permits retaining (or reaping) entire segments at a
          // time. Here, we set the key-level policy to be a PermanentRetentionPolcyImpl or NeverRetentionPolicyImpl
          // (respectively), and use ValueRetentionState.EMPTY.
          // In more complex cases, the policy on the NamespaceStore cannot make segment-level decisions, and we need
          // a key-level retention policy and state and to decide which individual keys within a segment to retain/reap.
          if (vrp instanceof SegmentLevelValueRetentionPolicyImpl) {
            SegmentLevelValueRetentionPolicyImpl<T> slvrp;
            boolean segmentRetained;

            slvrp = (SegmentLevelValueRetentionPolicyImpl<T>) vrp;
            segmentRetained = slvrp.retains(i, nsOptions.getSegmentSize(), valueRetentionState, curTimeNanos);
            if (segmentRetained) {
              // retain all keys in this segment
              segment_klvrp = new PermanentRetentionPolicyImpl();
              segment_vrs = new EmptyValueRetentionState();
            } else {
              // delete all keys in this segment
              if (verboseReap) {
                Log.infof("All keys in Segment %d created at %s will be reaped", i, ctime);
              }
              segment_klvrp = new NeverRetentionPolicyImpl();
              segment_vrs = new EmptyValueRetentionState();
              if (TracerFactory.isInitialized()) {
                TracerFactory.getTracer().onSegmentReap(i, ctime);
              }
            }
          } else {
            // key-level level vrp; scan at key level
            if (verboseReap) {
              Log.infof("Keys in Segment %d created at %s will be reaped according to Key level policy", i, ctime);
            }
            isKeyLevelReap = true;
            segment_klvrp = (KeyLevelValueRetentionPolicyImpl<T>) vrp;
            segment_vrs = valueRetentionState;
          }

          if (i == headSegment.getSegmentNumber()) {
            segment = headSegment;
          } else {
            if (nsOptions.getStorageType() == StorageType.RAM) {
              segment = getSegment(i, SegmentPrereadMode.Preread);
            } else {
              segment = fileSegmentCache.getIfPresent(i);
              if (segment == null) {
                FileSegmentStorageFormat fileSegmentStorageFormat;
                FileSegmentLoadMode fileSegmentLoadMode;

                fileSegmentStorageFormat = FileSegmentStorageFormat.parse(nsOptions.getStorageFormat());
                fileSegmentLoadMode =
                    ((segment_klvrp.considersStoredLength() && !fileSegmentStorageFormat.lengthsAreIndexed()) || (segment_klvrp.considersInvalidations() && !fileSegmentStorageFormat.invalidationsAreIndexed())) || forceDataSegmentLoadOnReap ?
                    FileSegmentLoadMode.ReadOnly :
                    FileSegmentLoadMode.ReadIndexOnly;

                // This segment will never be cached
                // We don't use getSegment as we do not wish to pollute the cache
                // Ideally, can reap without reading the data segment. If, however, the retention policy considers
                // stored length and stored lengths are not indexed, we must read the data segment
                // Likewise, if invalidations are not indexed, we must read the data segment
                segment = loadFileSegment(i, fileSegmentLoadMode, SegmentPrereadMode.Preread);
              }
            }
          }
          Log.fine("segment.singleReverseSegmentWalk %d", i);

          result = segment.singleReverseSegmentWalk(segment_klvrp, segment_vrs, curTimeNanos, ringMaster,
              reapImplState.invalidatedKeys, crossSegmentCompactionEnabled);

          if (i == headSegment.getSegmentNumber()) {
            if (verboseReap || verboseReapLogInfo) {
              Log.warningAsyncf("Retaining head segment");
            }
          } else {
            CompactionCheckResult ccr;
            boolean subsequentCanCompactIntoThisSegment;

            // FUTURE - currently not handling the case where we merge over a deleted segment
            //   i.e. if we have partially full segments 0 and 2, and empty segment 1
            //   in principle, we want to delete 1, and merge 2 into 0.
            //   Presently, we will perform that merge in the next pass.

            ccr = result.getV1();
            if (isKeyLevelReap && ccr.getInvalidEntries() > 0 && TracerFactory.isInitialized()) {
              TracerFactory.getTracer().onKeysReap(i, ccr.getInvalidEntries(), ctime);
            }

            subsequentCanCompactIntoThisSegment =
                isKeyLevelReap // don't compact across segments for segment-level reap
                && crossSegmentCompactionEnabled
                && ccr.getRetainedBytes() > 0 // present segment isn't deleted
                && reapImplState.getLastSegmentRetainedBytes() > 0 // subsequent segment has retained bytes
                && (reapImplState.getLastSegmentRetainedBytes() + ccr.getRetainedBytes() + SegmentFormat.headerSize < nsOptions.getSegmentSize()); // and it can fit
            Log.warningf("CompactionCheckResult %d  %s", i, ccr);
            Log.warningf("Segment %d discarded %d keys out of %d total", i, ccr.getInvalidEntries(),
                ccr.getTotalEntries());
            Log.warningf("Segment %d retained bytes %d  last segment retained bytes %d", i, ccr.getRetainedBytes(),
                reapImplState.getLastSegmentRetainedBytes());
            Log.warningf("subsequentCanCompactIntoThisSegment %d %s", i, subsequentCanCompactIntoThisSegment);
            if (subsequentCanCompactIntoThisSegment) {
              Pair<Integer, Triple<CompactionCheckResult, Set<Integer>, Set<Integer>>> lastSegmentReapResults;

              lastSegmentReapResults = reapImplState.getLastSegmentReapResults();
              if (lastSegmentReapResults != null) {
                reapImplState.addReapResult(lastSegmentReapResults.getV1(), lastSegmentReapResults.getV2());
            }
          }

            if (ccr.getValidEntries() == 0 // segment should be deleted
                || (ccr.getInvalidEntries() > 0 && ccr.getInvalidFraction() >= compactionThreshold) // segment should
                // be compacted
                || result.getV5() // segment must be compacted to ensure consistency
                || subsequentCanCompactIntoThisSegment) {
              // Inform the reap implementation
              reapImplState.addReapResult(i, result.getTripleAt1());
              // Add all invalidated keys in this segment to invalidatedAndCompactedKeys
              reapImplState.invalidatedKeys.addAll(result.getV4());
              reapImplState.setLastSegmentRetainedBytes(
                  subsequentCanCompactIntoThisSegment ? 0 : ccr.getRetainedBytes());
              reapImplState.setlastSegmentReapResults(null);
            } else {
              // save state in case we can compact this into a lower segment
              reapImplState.setLastSegmentRetainedBytes(ccr.getRetainedBytes());
              reapImplState.setlastSegmentReapResults(new Pair<>(i, result.getTripleAt1()));
              reapImplState.addUncompactedSegment(i);
              // For now, this segment is uncompacted.
              // If we actually add this saved segment to compaction,
              // addReapResult() will remove this from uncompacted segments.
            }
          }
          sw.stop();
          if (verboseReap || verboseReapLogInfo) {
            Log.warningAsyncf("%s %d %d", result.getV1(), result.getV2().size(), result.getV3().size());
            Log.warningAsyncf("singleReverseSegmentWalk: processing segment %d took %f", i, sw.getElapsedSeconds());
          }
        } catch (Exception e) {
          Log.logErrorWarning(e, "Skipping segment " + i + " due to Exception..");
          e.printStackTrace();
        }
      }
    }
    return segmentsReaped;
  }

  private int deleteSegments(Set<Integer> segmentsToDelete1, Set<Integer> segmentsToDelete2) {
    Set<Integer> segmentsToDelete;

    Log.infof("deleteSegments");
    Log.infof("segmentsToDelete1 %s", CollectionUtil.toString(segmentsToDelete1));
    Log.infof("segmentsToDelete2 %s", CollectionUtil.toString(segmentsToDelete2));
    segmentsToDelete = new HashSet<>(segmentsToDelete1.size() + segmentsToDelete2.size());
    segmentsToDelete.addAll(segmentsToDelete1);
    segmentsToDelete.addAll(segmentsToDelete2);
    return deleteSegments(segmentsToDelete);
  }

  private int deleteSegments(Set<Integer> segmentsToDelete) {
    List<Integer> segmentsToDeleteList;

    // Sort the list of segments to delete to ensure that we delete in monotonically increasing order
    segmentsToDeleteList = new ArrayList<>(segmentsToDelete.size());
    segmentsToDeleteList.addAll(segmentsToDelete);
    Collections.sort(segmentsToDeleteList);
    for (int segmentToDelete : segmentsToDeleteList) {
      deleteSegment(segmentToDelete);
    }
    return segmentsToDelete.size();
  }

  private void deleteSegment(int segmentNumber) {
    FileSegment segment;

    segment = fileSegmentCache.getIfPresent(segmentNumber);
    if (segment != null) {
      Log.infof("Invalidating cache segment: ", segmentNumber);
      fileSegmentCache.invalidate(segmentNumber);
      segment.close();
      segment = null;
    }
    if (reapPolicy.verboseSegmentDeletionAndCompaction()) {
      Log.warning("Deleting segment: ", segmentNumber);
    }
    FileCompactionUtil.delete(nsDir, segmentNumber);
    if (reapPolicy.verboseSegmentDeletionAndCompaction()) {
      Log.warning("Done deleting segment: ", segmentNumber);
    }
  }

  private void flushCompaction(Set<Integer> deletedSegments,
      HashedSetMap<DHTKey, CompactorModifiedEntry> modifiedEntries) {
    int numSegmentsDeleted;

    if (reapPolicy.verboseSegmentDeletionAndCompaction()) {
      Log.warningf("Flush compaction %x", ns);
    }
    writeLockAll();
    try {
      numSegmentsDeleted = deleteSegments(deletedSegments, fileSegmentCompactor.drainCurrentCompactionSourceSegments());
      fileSegmentCompactor.flushCompaction(false);
      updateOffsetLists(deletedSegments, modifiedEntries);
    } finally {
      writeUnlockAll();
    }
    updateAndCheckForceFinalization(numSegmentsDeleted);
  }

  private Pair<Set<Integer>, HashedSetMap<DHTKey, CompactorModifiedEntry>> compactAndDelete(int curSegment,
      Triple<CompactionCheckResult, Set<Integer>, Set<Integer>> result, boolean verboseReap,
      double compactionThreshold) {

    if (verboseReap || verboseReapLogInfo) {
      Log.warningf("compactAndDelete %d headSegment %d", curSegment, headSegment.getSegmentNumber());
    }
    // previous implementation locked here; present implementation avoids acquiring the lock
    // until it's absolutely necessary
    Set<Integer> segmentsToDelete;
    HashedSetMap<DHTKey, CompactorModifiedEntry> modifiedEntries;

    segmentsToDelete = new HashSet<>();
    modifiedEntries = new HashedSetMap<>();
      if (result != null) {
        CompactionCheckResult ccr;

        ccr = result.getV1();
        try {
          Stopwatch sw;

          sw = new SimpleStopwatch();
          if (verboseReap || verboseReapLogInfo) {
            Log.warningAsyncf("Segment %3d CompactionCheckResult:\t%s", curSegment, ccr.toString());
          }

        Log.finef("ccr %s", ccr);
          if (ccr.getValidEntries() == 0) {
          segmentsToDelete.add(curSegment);
        } else { // note that we used to check the compaction threshold here, but that check is moved into the reap
            try {
            HashedSetMap<DHTKey, CompactorModifiedEntry> compactionResult;

            compactionResult = fileSegmentCompactor.compact(curSegment,
                new RetainedOffsetMapCheck(result.getV2(), result.getV3()),
                reapPolicy.verboseSegmentDeletionAndCompaction(), ccr.hasInvalidEntries(),
                cdImplState.getUncompactedSegments());
            if (compactionResult != null) {
              modifiedEntries.addAll(compactionResult);
            }
            } catch (IOException ioe) {
              Log.logErrorWarning(ioe, "IOException compacting segment: " + curSegment);
            }
          }
          sw.stop();
          if (verboseReap || verboseReapLogInfo) {
            Log.warningAsyncf("compactAndDelete: processing segment %d took %f", curSegment, sw.getElapsedSeconds());
          }
        } catch (Exception e) {
          Log.logErrorWarning(e, "Skipping segment " + curSegment + " due to Exception.");
          e.printStackTrace();
        }
      }
    return new Pair<>(segmentsToDelete, modifiedEntries);
  }

  private void updateOffsetLists(Set<Integer> deletedSegments,
      HashedSetMap<DHTKey, CompactorModifiedEntry> modifiedEntries) {
    RAMOffsetListStore ols;

    ols = (RAMOffsetListStore) offsetListStore;

    // First, remove references to deleted segments
    if (deletedSegments.size() > 0) {
      Set<DHTKey> singleKeysInDeletedSegments;

      // Remove deleted segments from offset lists
      for (int i = 1; i <= ols.getNumLists(); i++) { // offset list indexing is 1-based
        RAMOffsetList ol;

        ol = (RAMOffsetList) ols.getOffsetList(i);
        ol.removeEntriesByValue(deletedSegments);
      }
      // Remove deleted segments from single-key (non-offset-list) mappings
      // (Iteration and deletion must be performed independently)
      singleKeysInDeletedSegments = new HashSet<>();
      for (DHTKeyIntEntry keyAndSegment : valueSegments) {
        if (keyAndSegment.getValue() >= 0 && deletedSegments.contains(keyAndSegment.getValue())) {
          singleKeysInDeletedSegments.add(keyAndSegment.getKey());
        }
      }
      for (DHTKey singleKeyToRemove : singleKeysInDeletedSegments) {
        valueSegments.remove(singleKeyToRemove);
      }
    }

    // Now, update references to entries deleted or moved during compaction
    for (DHTKey key : modifiedEntries.getKeys()) {
      int rawSegmentNumber;

      rawSegmentNumber = valueSegments.get(key);
      if (rawSegmentNumber < 0) {
        if (rawSegmentNumber != IntCuckooConstants.noSuchValue) {
          RAMOffsetList ol;

          if (Log.levelMet(Level.FINE)) {
            Log.finef("Updating reference list for %s", KeyUtil.keyToString(key));
          }
          ol = (RAMOffsetList) ols.getOffsetList(-rawSegmentNumber);
          ol.updateEntriesByMatch(modifiedEntries.getSet(key));
        } else {
          // No action required
        }
      } else {
        Set<CompactorModifiedEntry> entriesForKey;
        CompactorModifiedEntry currentEntry;

        entriesForKey = modifiedEntries.getSet(key);
        if (entriesForKey.size() != 1) {
          Log.warningAsyncf("Unexpected modifiedEntries.getSet(key).size() != 1");
        }
        currentEntry = entriesForKey.iterator().next();
        if (debugSegments) {
          Log.warningf("valueSegments.remove(%s)", KeyUtil.keyToString(key));
        }
        valueSegments.remove(key); // key will be added back in for modified entries below
        if (currentEntry.getNewSegmentNumber() != CompactorModifiedEntry.REMOVED) {
          if (nsOptions.getRevisionMode() != RevisionMode.UNRESTRICTED_REVISIONS) { // FUTURE - REMOVE AFTER
            // VERIFICATION
            if (currentEntry.getCreationTime() != 0) {
              Log.warningf("Unexpected currentEntry.getCreationTime() != 0");
            }
          }
          // add the key that was removed above
          if (debugSegments || Log.levelMet(Level.FINE)) {
            Log.warningf("valueSegments.put(%s, %d)", KeyUtil.keyToString(key), currentEntry.getNewSegmentNumber());
          }
          valueSegments.put(key, currentEntry.getNewSegmentNumber());
        }
      }
    }
  }

  ////////////////////////////////////
  // ManagedNamespaceStore implementation for internal runtime admin

  @Override
  public PurgeResult syncPurgeKey(DHTKey keyToPurge, long purgeBeforeCreationTimeNanosInclusive) throws IOException {

    // The locking order matters, since this method races with liveReap who acquires/release the following locks in
    // order:
    // reapLock.lock();
    //      readLock.lock(); metaReadLock.lock(); metaReadLock.unlock(); readLock.unlock();
    //      writeLock.lock(); metaWriteLock.lock(); metaWriteLock.unlock(); writeLock.unlock();
    // reapLock.unlock();
    reapLock.lock();
    writeLockAll();
    try {
      Integer startSegment;
      int endSegment;

      endSegment = 0;

      Log.warningf("Starting to purge DHTKey [%s] before nanos [%d] (inclusive)", keyToPurge.toString(),
          purgeBeforeCreationTimeNanosInclusive);
      newHeadSegment(true); // already hold writeLock
      Log.warningf("HeadSegment has been moved to [%d] for DHTKey: %s", getHeadSegmentNumber(), keyToPurge.toString());
      // metaReadLock.lock(); metaReadLock.unlock();
      startSegment = newestSegment(keyToPurge);
      if (startSegment == null) {
        Log.warningf("This node has no data for DHTKey: %s", keyToPurge.toString());
        return PurgeResult.empty();
      } else {
        InternalPurgeKeyRetentionPolicyImpl purgeRetentionPolicy;
        InternalPurgeKeyRetentionState purgeRetentionState;
        int[] segmentsReapedTrashedDeleted;
        int segmentsReaped, segmentsTrashed, segmentsDeleted;

        Log.warningf("Starting to reap segment [%d] to [%d] for DHTKey: %s", startSegment, endSegment,
            keyToPurge.toString());
        purgeRetentionState = new InternalPurgeKeyRetentionState();
        purgeRetentionPolicy = new InternalPurgeKeyRetentionPolicyImpl(keyToPurge,
            purgeBeforeCreationTimeNanosInclusive);
        segmentsReapedTrashedDeleted = forceReap(startSegment, endSegment, purgeRetentionPolicy, purgeRetentionState);
        segmentsReaped = segmentsReapedTrashedDeleted[0];
        segmentsTrashed = segmentsReapedTrashedDeleted[1];
        segmentsDeleted = segmentsReapedTrashedDeleted[2];

        if (segmentsReaped <= segmentsTrashed && segmentsTrashed <= segmentsDeleted) {
          return new PurgeResult(purgeRetentionState.getCount(), purgeRetentionState.getLatestCreationTimeNanos(),
              purgeRetentionState.getLatestVersion(), segmentsReaped, segmentsTrashed, segmentsDeleted);
        } else {
          throw new IOException(
              String.format("Inconsistent: segmentsReaped=[%d], segmentsTrashed=[%d], segmentsDeleted=[%d]",
                  segmentsReaped, segmentsTrashed, segmentsDeleted));
        }
      }
    } finally {
      writeUnlockAll();
      reapLock.unlock();
    }
  }

  @Override
  public PurgeResult syncPurgeKey(DHTKey keyToPurge) throws IOException {
    long curTimeNanos;

    curTimeNanos = systemTimeSource.absTimeNanos();
    return syncPurgeKey(keyToPurge, curTimeNanos);
  }

  @Override
  public List<Integer> listKeySegments(DHTKey key) throws IOException {
    return getKeySegments(key);
  }

  @Override
  public List<Integer> listKeySegments(DHTKey key, long beforeCreationTimeNanosInclusive) throws IOException {
    if (nsOptions.getVersionMode() == NamespaceVersionMode.CLIENT_SPECIFIED || nsOptions.getRevisionMode() == RevisionMode.UNRESTRICTED_REVISIONS) {
      throw new IOException(
          "Currently listKeySegments(DHTKey key, long beforeCreationTimeNanos) doesn't support NamespaceVersionMode" + ".CLIENT_SPECIFIED nor RevisionMode.UNRESTRICTED_REVISIONS");
    }

    readLockAll();
    try {
      List<Integer> allSegments, filteredSegments;

      allSegments = getKeySegments(key);
      filteredSegments = new ArrayList<>(allSegments.size());
      for (Integer curSegment : allSegments) {
        if (segmentExists(curSegment)) {
          long[] least;

          least = segmentOldestVersion(curSegment, key);
          if (least[VERSION_INDEX] != noSuchVersion) { // double check; getKeySegments(key) shall already make this true
            long leastCreationTime;
            leastCreationTime = least[STORAGE_TIME_INDEX];
            if (leastCreationTime <= beforeCreationTimeNanosInclusive) {
              filteredSegments.add(curSegment);
            }
          }
        }
      }
      return filteredSegments;
    } finally {
      readUnlockAll();
    }
  }

  ////////////////////////////////////
  // SSNamespaceStore implementation

  private static final int numPendingPutWorkers = Math.min(8, Runtime.getRuntime().availableProcessors());
  private static final int maxWorkBatchSize = 128;
  private static final BlockingQueue<PendingPut> pendingPuts = new ArrayBlockingQueue<>(
      numPendingPutWorkers * maxWorkBatchSize);

  private static final ArrayList<PendingPutWorker> pendingPutWorkers = mkWorkers();

  private static ArrayList<PendingPutWorker> mkWorkers() {
    ArrayList<PendingPutWorker> ppwArray = new ArrayList<>();
    if (enablePendingPuts) {
      Log.warningf("numPendingPutWorkers: %d", numPendingPutWorkers);
      for (int i = 0; i < numPendingPutWorkers; i++) {
        ppwArray.add(new PendingPutWorker(i, pendingPuts));
      }
      }
    return ppwArray;
  }

  public static void shutdownPendingPutWorkers() {
    for (PendingPutWorker ppw : pendingPutWorkers) {
      ppw.stop();
    }
  }

  static class PendingPut {
    final NamespaceStore nsStore;
    final List<StorageValueAndParameters> values;
    final KeyedOpResultListener resultListener;

    PendingPut(NamespaceStore nsStore, List<StorageValueAndParameters> values, KeyedOpResultListener resultListener) {
      this.nsStore = nsStore;
      this.values = values;
      this.resultListener = resultListener;
    }
  }

  private List<StorageValueAndParameters> fixupCompression(List<StorageValueAndParameters> values) {
    List<StorageValueAndParameters> _values;

    _values = new ArrayList<>(values.size());
    for (StorageValueAndParameters svp : values) {
      StorageValueAndParameters _svp;
      StorageValueAndParameters __svp;
      int compressedLength;

      if (svp.compressedSizeSet()) {
        compressedLength = svp.getCompressedSize();
      } else {
        compressedLength = svp.getValue().remaining();
      }
      // The client disallows any compression usage when the compressed size equals or exceeds the
      // uncompressed size. As ccss is per message, and this compression check is per value,
      // we catch this case and modify the ccss for values where this applies.
      if (compressedLength == svp.getUncompressedSize()) {
        _svp = svp.ccss(CCSSUtil.updateCompression(svp.getCCSS(), Compression.NONE));
      } else {
        _svp = svp;
      }
      if (svpMapper != null && compressedLength > svpMapThreshold) {
        try {
          __svp = svpMapper.convertToMappedSVP(_svp);
        } catch (Exception e) { // only IO thrown, but catch & tolerate anything
          Log.logErrorWarning(e);
          __svp = _svp;
        }
      } else {
        __svp = _svp;
      }
      _values.add(__svp);
    }
    return _values;
  }

  private void addPendingPut(List<StorageValueAndParameters> values, KeyedOpResultListener resultListener) {
    try {
      //Log.warningf("Adding pending put %d", values.size());
      pendingPuts.put(new PendingPut(this, fixupCompression(values), resultListener));
    } catch (InterruptedException e) {
      Log.logErrorWarning(e);
    }
  }

  static class PendingPutWorker implements Runnable {
    private boolean running;
    private final BlockingQueue<PendingPut> q;
    private final PendingPut[] work;
    public final Thread runner;

    PendingPutWorker(int index, BlockingQueue<PendingPut> q) {

      work = new PendingPut[maxWorkBatchSize];
      this.q = q;
      runner = ThreadUtil.newDaemonThread(this, "PendingPutWorker." + index);
      running = true;
      runner.start();
    }

    private int takeMultiple() throws InterruptedException {
      int numTaken;

      numTaken = 0;
      while (numTaken < maxWorkBatchSize) {
        if (numTaken == 0) {
          work[numTaken] = q.take();
          ++numTaken;
        } else {
          PendingPut p;

          p = q.poll();
          if (p != null) {
            work[numTaken] = p;
            ++numTaken;
          } else {
            break;
          }
        }
      }
      return numTaken;
    }

    private void process() {
      int numTaken;

      try {
        //numTaken = q.takeMultiple(work);
        numTaken = takeMultiple();
        if (Log.levelMet(Level.INFO)) {
          Log.warningf("q.size() %d\tnumTaken %d", q.size(), numTaken);
        }
      } catch (InterruptedException e) {
        numTaken = 0;
      }

      if (numTaken > 0) {
        mergePendingPuts(work, numTaken); // merge puts

        // send results (actual send)
        for (int i = 0; i < numTaken; i++) {
          if (work[i].resultListener instanceof PutCommunicator) {
            PutCommunicator putCommunicator;

            putCommunicator = (PutCommunicator) work[i].resultListener;
            putCommunicator.getPutOperationContainer().sendInitialResults(putCommunicator);
          } else {
            Log.warningf("not PutCommunicator %s", work[i].resultListener.getClass().getName());
          }
        }

        ArrayUtil.clear(work); // allow gc of completed work
      }
    }

    private int numValues(PendingPut[] pendingPuts, int numPendingPuts) {
      int numValues;

      numValues = 0;
      for (int i = 0; i < numPendingPuts; i++) {
        numValues += pendingPuts[i].values.size();
      }
      return numValues;
    }

    private void mergePendingPuts(PendingPut[] pendingPuts, int numPendingPuts) {
      int maxValues;
      int sIndex;

      maxValues = numValues(pendingPuts, numPendingPuts);
      sIndex = 0;
      while (sIndex < numPendingPuts) {
        KeyedOpResultMultiplexor resultListenerMultiplexor;
        List<StorageValueAndParameters> values;
        NamespaceStore nsStore;

        nsStore = pendingPuts[sIndex].nsStore;
        values = new ArrayList<>(maxValues);
        resultListenerMultiplexor = new KeyedOpResultMultiplexor();
        while (sIndex < numPendingPuts && pendingPuts[sIndex].nsStore == nsStore) {
          for (StorageValueAndParameters svp : pendingPuts[sIndex].values) {
            values.add(svp);
            resultListenerMultiplexor.addListener(svp.getKey(), pendingPuts[sIndex].resultListener);
          }
          sIndex++;
        }
        if (values.size() <= 0) {
          throw new RuntimeException("Unexpected values.size() <= 0: " + values.size());
        }
        if (nsStore.putTrigger != null && nsStore.putTrigger.supportsMerge()) {
          Set<Waiter> triggeredWaitFors;
          Map<DHTKey, OpResult> mergeResults;

          mergeResults = nsStore.putTrigger.mergePut(values);
          nsStore.writeLock.lock();
          try {
            triggeredWaitFors = nsStore.notifyAndCheckWaiters(mergeResults, resultListenerMultiplexor);
          } finally {
            nsStore.writeLock.unlock();
          }
          nsStore.handleTriggeredWaitFors(triggeredWaitFors);

          for (Map.Entry<DHTKey, OpResult> resultEntry : mergeResults.entrySet()) {
            resultListenerMultiplexor.sendResult(resultEntry.getKey(), resultEntry.getValue());
          }
        } else {
          nsStore.put(values, null, resultListenerMultiplexor); // Note: pending puts don't support userdata
        }
        maxValues -= values.size();
      }
    }

    public void run() {
      while (running) {
        try {
          process();
        } catch (Exception e) {
          Log.logErrorWarning(e);
          ThreadUtil.pauseAfterException();
        }
      }
    }

    public void stop() {
      running = false;
      runner.interrupt();
    }
  }

  @Override
  public long getNamespaceHash() {
    return ns;
  }

  @Override
  public boolean isNamespace(String ns) {
    return NamespaceUtil.nameToContext(ns) == this.ns;
  }

  @Override
  public OpResult put(DHTKey key, ByteBuffer value, SSStorageParametersAndRequirements storageParams, byte[] userData,
      NamespaceVersionMode nsVersionMode) {
    return _put(key, value, StorageParametersAndRequirements.fromSSStorageParametersAndRequirements(storageParams),
        userData, nsVersionMode);
  }

  @Override
  public ByteBuffer retrieve(DHTKey key, SSRetrievalOptions options) {
    ByteBuffer result;

    result = _retrieve(key, InternalRetrievalOptions.fromSSRetrievalOptions(options));
    if (result != null && !options.getReturnInvalidations()) {
      if (MetaDataUtil.isInvalidation(result, 0)) {
        result = null;
      }
    }
    return result;
  }

  @Override
  public ByteBuffer[] retrieve(DHTKey[] keys, SSRetrievalOptions options) {
    ByteBuffer[] results;

    results = _retrieve(keys, InternalRetrievalOptions.fromSSRetrievalOptions(options));
    if (options.getReturnInvalidations()) {
      return results;
    } else {
      ByteBuffer[] nulledResults;
      ByteBuffer result;

      nulledResults = new ByteBuffer[results.length];
      for (int i = 0; i < results.length; i++) {
        result = results[i];
        if (result != null && MetaDataUtil.isInvalidation(result, 0)) {
          result = null;
        }
        nulledResults[i] = result;
      }
      return nulledResults;
    }
  }

  @Override
  public File getNamespaceSSDir() {
    synchronized (ssDir) {
      if (!ssTempDir.exists() && !ssTempDir.mkdir()) {
        throw new RuntimeException("Unable to create: " + ssTempDir);
      }
      if (!ssDir.exists() && !ssDir.mkdir()) {
        throw new RuntimeException("Unable to create: " + ssDir);
      } else {
        return ssDir;
      }
    }
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return rwLock;
  }

  @Override
  public int getHeadSegmentNumber() {
    return headSegment.getSegmentNumber();
  }

  private class SegmentLoader implements Callable<FileSegment> {
    private final int segmentNumber;
    private final FileSegmentLoadMode fileSegmentLoadMode;
    private final SegmentPrereadMode segmentPrereadMode;

    SegmentLoader(int segmentNumber, FileSegmentLoadMode fileSegmentLoadMode, SegmentPrereadMode segmentPrereadMode) {
      this.segmentNumber = segmentNumber;
      this.fileSegmentLoadMode = fileSegmentLoadMode;
      this.segmentPrereadMode = segmentPrereadMode;
    }

    @Override
    public FileSegment call() throws Exception {
      return loadFileSegment(segmentNumber, fileSegmentLoadMode, segmentPrereadMode);
    }
  }
}
