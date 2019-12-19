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
package io.hops.hopsworks.common.provenance.core.elastic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.hops.hopsworks.exceptions.ElasticException;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class ElasticCache {
  
  private static final Logger LOG = Logger.getLogger(ElasticCache.class.getName());
  
  @EJB
  private ProvElasticController client;
  
  private Cache<String, Map<String, String>> indexMappings;
  
  @PostConstruct
  private void initClient() {
    indexMappings = Caffeine.newBuilder()
      .expireAfterWrite(1, TimeUnit.HOURS)
      .maximumSize(50)
      .build();
  }
  
  public void cacheMapping(String index, Map<String, String> mapping) {
    indexMappings.put(index, mapping);
  }
  
  public Map<String, String> getMapping(String index) {
    return indexMappings.getIfPresent(index);
  }
  
  public void clearMapping(String index) {
    indexMappings.invalidate(index);
  }
  
  public Map<String, String> mngIndexGetMapping(String index, boolean forceFetch) throws ElasticException {
    if(forceFetch) {
      clearMapping(index);
    }
    Map<String, String> mapping = getMapping(index);
    if(mapping == null) {
      Map<String, Map<String, String>> result = client.mngIndexGetMappings(index);
      mapping = result.get(index);
      if(mapping != null) {
        cacheMapping(index, mapping);
      }
    }
    return mapping;
  }
}
