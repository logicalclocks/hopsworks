/*
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
 */

package io.hops.hopsworks.ca.controllers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.hops.hopsworks.ca.dao.ConfEntry;

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
// This class reads the configuration from the database
// for the ca
public class CAConf {
  private LoadingCache<CAConfKeys, String> caConfiguration;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @PostConstruct
  public void init() {
    caConfiguration = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<CAConfKeys, String>() {

          @Override
          public String load(CAConfKeys s) throws Exception {
            ConfEntry confEntry = em.find(ConfEntry.class, s.getKey());
            if (confEntry != null) {
              return confEntry.getValue();
            } else {
              return s.getDefaultValue();
            }
          }
        });
  }

  public enum CAConfKeys {
    SERVICE_KEY_ROTATION_ENABLED("service_key_rotation_enabled", "false"),
    SERVICE_KEY_ROTATION_INTERVAL("service_key_rotation_interval", "3d"),
    APPLICATION_CERTIFICATE_VALIDITY_PERIOD("application_certificate_validity_period", "3d"),
    HOPSWORKS_INSTALL_DIR("hopsworks_dir", "/srv/hops/domains"),
    CERTS_DIR("certs_dir", "/srv/hops/certs-dir"),
    HOPSWORKS_SSL_MASTER_PASSWORD("hopsworks_master_password", "adminpw"),
    KUBE_CA_PASSWORD("kube_ca_password", "adminpw"),
    HOPSWORKS_REST_LOGLEVEL("hopsworks_rest_log_level", "PROD"),
    JWT_ISSUER("jwt_issuer", "hopsworks@logicalclocks.com"),
    SUDOERS_DIR("sudoers_dir", "/srv/hops/sbin");

    private String key;
    private String defaultValue;

    CAConfKeys(String key, String defaultValue) {
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

  private String get(CAConfKeys confKey) {
    try {
      return caConfiguration.get(confKey);
    } catch (ExecutionException e) {
      return confKey.getDefaultValue();
    }
  }

  public String getString(CAConfKeys confKey) {
    return get(confKey);
  }

  public Boolean getBoolean(CAConfKeys confKey) {
    return Boolean.valueOf(get(confKey));
  }

  public Integer getInt(CAConfKeys confKey) {
    return Integer.valueOf(get(confKey));
  }

  public Long getLong(CAConfKeys confKeys) {
    return Long.valueOf(get(confKeys));
  }
}
