/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.upload;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.partition.Partition;
import fish.payara.cluster.Clustered;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Clustered
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ResumableInfoProcessor implements Serializable {
  private static final long serialVersionUID = 2660859257315228512L;
  private static final Logger LOGGER = Logger.getLogger(ResumableInfoProcessor.class.getName());
  
  public void put(FlowInfo flowInfo, HazelcastInstance hazelcastInstance, String mapName) {
    IMap<Integer, UploadInfo> uploadInfoMap = hazelcastInstance.getMap(mapName);
    uploadInfoMap.putIfAbsent(flowInfo.hashCode(), new UploadInfo(flowInfo.getTotalSize()));
    LOGGER.log(Level.FINE, "Put chunk. id: {0}, {1}, uploadInfo: {2}", new Object[]{flowInfo.hashCode(), flowInfo,
      uploadInfoMap.get(flowInfo.hashCode())});
  }
  
  public boolean addChunkAndCheckIfFinished(FlowInfo info, int rcn, long contentLength,
    HazelcastInstance hazelcastInstance, String mapName) {
    boolean finished;
    IMap<Integer, UploadInfo> uploadInfoMap = hazelcastInstance.getMap(mapName);
    UploadInfo uploadInfo = uploadInfoMap.get(info.hashCode());
    if (uploadInfo == null) {
      LOGGER.log(Level.WARNING, "Failed to find upload info with id: {1}, rcn: {2}, {3}",
        new Object[]{info.hashCode(), rcn, info});
      return false;
    }
    finished = uploadInfo.addChunkAndCheckIfFinished(rcn, contentLength);
    Partition partition = hazelcastInstance.getPartitionService().getPartition(info.hashCode());
    LOGGER.log(Level.FINE, "Add chunk and check if finished. finished: {0}, id: {1}, rcn: {2}, uploadInfo: {3}, " +
      "{4}, partitionId: {5}, partition owner: {6}", new Object[]{finished, info.hashCode(), rcn, uploadInfo, info,
      partition.getPartitionId(), partition.getOwner()});
    if (finished) {
      uploadInfoMap.remove(info.hashCode());
    } else {
      uploadInfoMap.replace(info.hashCode(), uploadInfo);
    }
    return finished;
  }
  
  @Lock(LockType.READ)
  public boolean isUploaded(Integer identifier, Integer rcn, HazelcastInstance hazelcastInstance, String mapName) {
    IMap<Integer, UploadInfo> uploadInfo = hazelcastInstance.getMap(mapName);
    UploadInfo uInfo = uploadInfo.get(identifier);
    return uInfo != null && uInfo.getUploadedChunks().contains(rcn);
  }
}
