/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.jobs.flink;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ejb.ConcurrencyManagementType.BEAN;

@ConcurrencyManagement(BEAN)
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FlinkMasterAddrCache {
  
  private static final Logger LOGGER = Logger.getLogger(FlinkMasterAddrCache.class.getName());
  
  @EJB
  private FlinkController flinkController;
  
  //Key is applicationId and value is FlinkMasterAddr (IP:port)
  private LoadingCache<String, String> cache;
  
  @PostConstruct
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void init() {
    CacheLoader<String, String> loader = new CacheLoader<String, String>() {
      @Override
      public String load(String appId) throws JobException {
        LOGGER.log(Level.INFO, "Fetching FlinkMaster Addr into cache for appId:" + appId);
        String addr = flinkController.getFlinkMasterAddr(appId);
        if (Strings.isNullOrEmpty(addr)) {
          throw new JobException(RESTCodes.JobErrorCode.APPID_NOT_FOUND,Level.WARNING);
        }
        return addr;
      }
    };
    
    cache = CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(1, TimeUnit.HOURS).build(loader);
  }
  
  public String get(String appId) {
    try {
      return cache.get(appId);
    } catch (Exception ex){
      LOGGER.log(Level.WARNING, "Could not get Flink Master Address from Cache.", ex);
      return null;
    }
  }
}