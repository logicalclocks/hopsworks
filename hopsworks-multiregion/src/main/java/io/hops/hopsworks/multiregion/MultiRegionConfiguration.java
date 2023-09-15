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

package io.hops.hopsworks.multiregion;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.hops.hopsworks.persistence.entity.util.Variables;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class MultiRegionConfiguration {

  private LoadingCache<MultiRegionConfKeys, String> multiRegionConfiguration;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  /*
   * ------------------ MultiRegion WatchDog ------------------
   */

  public enum MultiRegionConfKeys {
    MULTIREGION_WATCHDOG_ENABLED("multiregion_watchdog_enabled", "false"),
    MULTIREGION_WATCHDOG_INTERVAL("multiregion_watchdog_interval", "5s"),
    MULTIREGION_WATCHDOG_URL("multiregion_watchdog_url", ""),
    MULTIREGION_WATCHDOG_REGION("multiregion_watchdog_region", ""),
    HOPSWORKS_DOMAIN_DIR("hopsworks_dir", "/srv/hops/domains/domain1"),
    HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD("hopsworks_master_password", "adminpw");

    private String key;
    private String defaultValue;

    MultiRegionConfKeys(String key, String defaultValue) {
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

  @PostConstruct
  public void init() {
    multiRegionConfiguration = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<MultiRegionConfKeys, String>() {

          @Override
          public String load(MultiRegionConfKeys s) throws Exception {
            Variables confEntry = em.find(Variables.class, s.getKey());
            if (confEntry != null) {
              return confEntry.getValue();
            } else {
              return s.getDefaultValue();
            }
          }
        });
  }

  private String get(MultiRegionConfKeys confKey) {
    try {
      return multiRegionConfiguration.get(confKey);
    } catch (ExecutionException e) {
      return confKey.getDefaultValue();
    }
  }

  public String getString(MultiRegionConfKeys confKey) {
    return get(confKey);
  }

  public Boolean getBoolean(MultiRegionConfKeys confKey) {
    return Boolean.valueOf(get(confKey));
  }

  public Long getInterval(MultiRegionConfKeys confKeys) {
    Long intervalValue = getConfTimeValue(get(confKeys));
    TimeUnit intervalTimeunit = getConfTimeTimeUnit(get(confKeys));
    return intervalTimeunit.toMillis(intervalValue);
  }

  private static final Map<String, TimeUnit> TIME_SUFFIXES;

  static {
    TIME_SUFFIXES = new HashMap<>(5);
    TIME_SUFFIXES.put("ms", TimeUnit.MILLISECONDS);
    TIME_SUFFIXES.put("s", TimeUnit.SECONDS);
    TIME_SUFFIXES.put("m", TimeUnit.MINUTES);
    TIME_SUFFIXES.put("h", TimeUnit.HOURS);
    TIME_SUFFIXES.put("d", TimeUnit.DAYS);
  }
  private static final Pattern TIME_CONF_PATTERN = Pattern.compile("([0-9]+)([a-z]+)?");


  private Long getConfTimeValue(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    return Long.parseLong(matcher.group(1));
  }

  private TimeUnit getConfTimeTimeUnit(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    String timeUnitStr = matcher.group(2);
    if (null != timeUnitStr && !TIME_SUFFIXES.containsKey(timeUnitStr.toLowerCase())) {
      throw new IllegalArgumentException("Invalid time suffix in configuration: " + configurationTime);
    }
    return timeUnitStr == null ? TimeUnit.MINUTES : TIME_SUFFIXES.get(timeUnitStr.toLowerCase());
  }
}
