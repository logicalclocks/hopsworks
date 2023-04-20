/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.upload;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ResumableInfoStorage {
  private static final Logger LOGGER = Logger.getLogger(ResumableInfoStorage.class.getName());
  private static final String MAP_NAME = "uploadInfo";

  @Inject
  private HazelcastInstance hazelcastInstance;
  @Resource
  private TimerService timerService;
  @EJB
  private ResumableInfoProcessor resumableInfoProcessor;

  private ConcurrentHashMap<Integer, UploadInfo> flowInfoMap;

  @PostConstruct
  protected void init() {
    if (hazelcastInstance != null) {
      if (hazelcastInstance.getConfig().getMapConfigOrNull(MAP_NAME) == null) {
        MapConfig mapConfig = new MapConfig(MAP_NAME);
        mapConfig.setMaxIdleSeconds(1800); //mark entries for removal after being untouched for 30 min (1800 seconds)
        hazelcastInstance.getConfig().addMapConfig(mapConfig);
      }
    } else {
      flowInfoMap = new ConcurrentHashMap<>();
      //Only if no hazelcast ==> not clustered
      timerService.createIntervalTimer(0L, TimeUnit.MILLISECONDS.convert(1L, TimeUnit.HOURS),
        new TimerConfig("Clean expired upload info.", false));
    }
  }

  @Timeout
  public void cleanExpiredUploadInfo() {
    try {
      long now = new Date().getTime();
      flowInfoMap.entrySet().removeIf(e -> {
        long diff = now - e.getValue().getLastWrite().getTime();
        LOGGER.log(Level.INFO, "Removing Expired Upload Info: {0}, last write : {1} minutes ago",
          new Object[]{e.getValue(), TimeUnit.MILLISECONDS.toMinutes(diff)});
        return TimeUnit.MILLISECONDS.toMinutes(diff) > 30;
      });
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to Clean expired upload info map. Error: {0}", e.getMessage());
    }
  }

  /**
   * Put uploadInfo.
   * <p/>
   *
   * @param flowInfo
   * @return
   */
  public void put(FlowInfo flowInfo) {
    if (hazelcastInstance != null) {
      resumableInfoProcessor.put(flowInfo, hazelcastInstance, MAP_NAME);
    } else {
      flowInfoMap.putIfAbsent(flowInfo.hashCode(), new UploadInfo(flowInfo.getTotalSize()));
    }
  }

  /**
   * Add chunk number and check if finished.
   *
   * @param info
   * @param rcn
   * @param contentLength
   * @return
   */
  public boolean addChunkAndCheckIfFinished(FlowInfo info, int rcn, long contentLength) {
    boolean finished;
    if (hazelcastInstance != null) {
      finished = resumableInfoProcessor.addChunkAndCheckIfFinished(info, rcn, contentLength, hazelcastInstance,
        MAP_NAME);
    } else {
      UploadInfo uploadInfo = flowInfoMap.get(info.hashCode());
      if (uploadInfo == null) {
        return false;
      }
      finished = uploadInfo.addChunkAndCheckIfFinished(rcn, contentLength);
      if (finished) {
        flowInfoMap.remove(info.hashCode());
      }
    }
    return finished;
  }

  /**
   * Check if chunk identified by rcn is uploaded.
   *
   * @param identifier
   * @param rcn
   * @return
   */
  public boolean isUploaded(Integer identifier, Integer rcn) {
    if (hazelcastInstance != null) {
      return resumableInfoProcessor.isUploaded(identifier, rcn, hazelcastInstance, MAP_NAME);
    } else {
      UploadInfo uploadInfo = flowInfoMap.get(identifier);
      return uploadInfo != null && uploadInfo.getUploadedChunks().contains(rcn);
    }
  }
}
