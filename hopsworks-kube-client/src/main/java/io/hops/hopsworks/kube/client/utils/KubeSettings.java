/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.kube.client.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class KubeSettings {
  private LoadingCache<KubeSettingKeys, String> configurations;
  @EJB
  private VariablesFacade variablesFacade;
  
  @PostConstruct
  public void init() {
    configurations = CacheBuilder.newBuilder()
      .maximumSize(100)
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .build(new CacheLoader<KubeSettingKeys, String>() {
        @Override
        public String load(KubeSettingKeys s) {
          return getVariable(s.getKey(), s.getDefaultValue());
        }
      });
  }
  
  public enum KubeSettingKeys {
    NAMESPACE("hopsworks_kube_namespace", "hopsworks"),
    ALERT_MANAGER_CONFIG_MAP("alert_manager_config_map", "hopsworks-release-alertmanager"),
    HOPSWORKS_SECRET("master_encryption_password_secret", "hopsworks-secrets"),
    MASTER_ENCRYPTION_PASSWORD("master_encryption_password_value", "encryption_master_password");
    private final String key;
    private final String defaultValue;
    
    KubeSettingKeys(String key, String defaultValue) {
      this.key = key;
      this.defaultValue = defaultValue;
    }
    
    public String getKey() {
      return key;
    }
    
    public String getDefaultValue() {
      return defaultValue;
    }
  }
  
  private String getVariable(String name, String defaultValue) {
    Optional<String> value = variablesFacade.getVariableValue(name);
    return value.orElse(defaultValue);
  }
  
  public String get(KubeSettingKeys confKey) {
    try {
      return configurations.get(confKey);
    } catch (ExecutionException e) {
      return confKey.getDefaultValue();
    }
  }
}
