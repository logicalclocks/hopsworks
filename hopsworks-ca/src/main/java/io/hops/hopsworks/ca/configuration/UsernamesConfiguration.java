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
package io.hops.hopsworks.ca.configuration;

import io.hops.hadoop.shaded.com.google.gson.Gson;
import io.hops.hadoop.shaded.com.google.gson.JsonSyntaxException;
import io.hops.hopsworks.persistence.entity.util.Variables;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.Map;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class UsernamesConfiguration {

  private static final String UNIX_USERNAMES_VARIABLE = "unix_usernames_conf";
  private Map<String, String> unixUsernames = new HashMap<>();
  private Map<String, String> inverseUnixUsernames = new HashMap<>();
  private final Gson gson = new Gson();

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @PostConstruct
  public void init() {
    populateDefaultValues();
    Variables v = em.find(Variables.class, UNIX_USERNAMES_VARIABLE);
    if (v != null) {
      String rawValue = v.getValue();
      try {
        Map<String, String> confUnixUsernames = gson.fromJson(rawValue, Map.class);
        for (Map.Entry<String, String> k : confUnixUsernames.entrySet()) {
          unixUsernames.put(k.getKey(), k.getValue());
          inverseUnixUsernames.put(k.getValue(), k.getKey());
        }
      } catch (JsonSyntaxException ex) {
        // no configuration specified
      }
    }
  }

  private void populateDefaultValues() {
    for (Username u : Username.values()) {
      unixUsernames.put(u.name().toLowerCase(), u._default);
      inverseUnixUsernames.put(u._default, u.name().toLowerCase());
    }
  }

  public enum Username {
    GLASSFISH("glassfish"),
    GLASSFISHINTERNAL("glassfishinternal"),
    HDFS("hdfs"),
    HIVE("hive"),
    LIVY("livy"),
    FLINK("flink"),
    CONSUL("consul"),
    HOPSMON("hopsmon"),
    ZOOKEEPER("zookeeper"),
    RMYARN("rmyarn"),
    ONLINEFS("onlinefs"),
    ELASTIC("elastic"),
    FLYINGDUCK("flyingduck"),
    KAGENT("kagent");

    private final String _default;

    Username(String _default) {
      this._default = _default;
    }
  }

  public String getUsername(Username username) {
    String configuredUsername = unixUsernames.get(username.name().toLowerCase());
    if (configuredUsername != null) {
      return configuredUsername;
    }
    return username._default;
  }

  public String getNormalizedUsername(String username) {
    return inverseUnixUsernames.get(username.toLowerCase());
  }
}
