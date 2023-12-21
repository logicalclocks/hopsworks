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

package io.hops.hopsworks.api.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.hops.hopsworks.persistence.entity.util.Variables;
import io.hops.hopsworks.restutils.RESTLogLevel;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Configuration {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  private LoadingCache<AuthConfigurationKeys, String> configuration;

  public enum AuthConfigurationKeys {

    HOPSWORKS_REST_LOG_LEVEL("hopsworks_rest_log_level", RESTLogLevel.PROD.name());

    private String key;
    private String defaultValue;

    AuthConfigurationKeys(String key, String defaultValue) {
      this.key = key;
      this.defaultValue = defaultValue;
    }
  }

  @PostConstruct
  public void init() {
    configuration = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<AuthConfigurationKeys, String>() {
          @Override
          public String load(AuthConfigurationKeys k) throws Exception {
            Variables var = em.find(Variables.class, k.key);
            return var != null ? var.getValue() : k.defaultValue;
          }
        });
  }

  private String get(AuthConfigurationKeys key) {
    try {
      return configuration.get(key);
    } catch (ExecutionException ex) {
      return key.defaultValue;
    }
  }

  public RESTLogLevel getLogLevel(AuthConfigurationKeys key) {
    return RESTLogLevel.valueOf(get(key));
  }
}
