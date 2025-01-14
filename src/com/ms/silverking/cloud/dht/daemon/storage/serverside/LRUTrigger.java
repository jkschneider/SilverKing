package com.ms.silverking.cloud.dht.daemon.storage.serverside;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ms.silverking.cloud.dht.NamespaceVersionMode;
import com.ms.silverking.cloud.dht.common.DHTKey;
import com.ms.silverking.cloud.dht.common.OpResult;
import com.ms.silverking.cloud.dht.daemon.storage.NamespaceStore;
import com.ms.silverking.cloud.dht.daemon.storage.StorageValueAndParameters;
import com.ms.silverking.cloud.dht.daemon.storage.retention.LRURetentionPolicyImpl;
import com.ms.silverking.cloud.dht.serverside.PutTrigger;
import com.ms.silverking.cloud.dht.serverside.RetrieveTrigger;
import com.ms.silverking.cloud.dht.serverside.SSNamespaceStore;
import com.ms.silverking.cloud.dht.serverside.SSRetrievalOptions;
import com.ms.silverking.cloud.dht.serverside.SSStorageParametersAndRequirements;
import com.ms.silverking.log.Log;

public class LRUTrigger implements PutTrigger, RetrieveTrigger.Direct {
  private LRURetentionPolicyImpl impl;
  private SSNamespaceStore nsStore;

  static {
    Log.fine("LRUTrigger loaded");
  }

  public LRUTrigger() {
  }

  @Override
  public void initialize(SSNamespaceStore nsStore) {
    this.nsStore = nsStore;

    if (nsStore instanceof NamespaceStore) {
      this.impl = (LRURetentionPolicyImpl) ((NamespaceStore) this.nsStore).getValueRetentionPolicyImpl();
    }
  }

  @Override
  public ByteBuffer retrieve(SSNamespaceStore nsStore, DHTKey key, SSRetrievalOptions options) {
    impl.markRead(key);
    return nsStore.retrieve(key, options);
  }

  @Override
  public ByteBuffer[] retrieve(SSNamespaceStore nsStore, DHTKey[] keys, SSRetrievalOptions options) {
    for (DHTKey key : keys) {
      impl.markRead(key);
    }
    return nsStore.retrieve(keys, options);
  }

  @Override
  public Iterator<DHTKey> keyIterator() {
    throw new RuntimeException("Panic");
  }

  @Override
  public long getTotalKeys() {
    throw new RuntimeException("Panic");
  }

  @Override
  public OpResult put(SSNamespaceStore nsStore, DHTKey key, ByteBuffer value,
      SSStorageParametersAndRequirements storageParams, byte[] userData, NamespaceVersionMode nsVersionMode) {
    impl.markWrite(key, storageParams.getVersion(), storageParams.getCompressedSize());
    return nsStore.put(key, value, storageParams, userData, nsVersionMode);
  }

  @Override
  public OpResult putUpdate(SSNamespaceStore nsStore, DHTKey key, long version, byte storageState) {
    return nsStore.putUpdate(key, version, storageState);
  }

  @Override
  public Map<DHTKey, OpResult> mergePut(List<StorageValueAndParameters> values) {
    throw new RuntimeException("Panic");
  }

  @Override
  public boolean supportsMerge() {
    return false;
  }

  @Override
  public boolean subsumesStorage() {
    return false;
  }
}
