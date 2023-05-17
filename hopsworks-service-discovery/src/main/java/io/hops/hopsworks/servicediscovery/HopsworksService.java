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
package io.hops.hopsworks.servicediscovery;


import io.hops.hopsworks.servicediscovery.tags.AirflowTags;
import io.hops.hopsworks.servicediscovery.tags.FlinkTags;
import io.hops.hopsworks.servicediscovery.tags.FlyingDuckTags;
import io.hops.hopsworks.servicediscovery.tags.GlassfishTags;
import io.hops.hopsworks.servicediscovery.tags.HiveTags;
import io.hops.hopsworks.servicediscovery.tags.KafkaTags;
import io.hops.hopsworks.servicediscovery.tags.LogstashTags;
import io.hops.hopsworks.servicediscovery.tags.MysqlTags;
import io.hops.hopsworks.servicediscovery.tags.NamenodeTags;
import io.hops.hopsworks.servicediscovery.tags.NoTags;
import io.hops.hopsworks.servicediscovery.tags.OpenSearchTags;
import io.hops.hopsworks.servicediscovery.tags.PrometheusTags;
import io.hops.hopsworks.servicediscovery.tags.ResourceManagerTags;
import io.hops.hopsworks.servicediscovery.tags.ServiceTags;
import io.hops.hopsworks.servicediscovery.tags.ZooKeeperTags;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HopsworksService<T extends ServiceTags> {

  public static final HopsworksService<ResourceManagerTags> RESOURCE_MANAGER = HopsworksService.of("resourcemanager",
      ResourceManagerTags.values());
  public static final HopsworksService<NoTags> LIVY = HopsworksService.of("livy");
  public static final HopsworksService<ZooKeeperTags> ZOOKEEPER = HopsworksService.of("zookeeper",
      ZooKeeperTags.values());
  public static final HopsworksService<NoTags> SPARK_HISTORY_SERVER = HopsworksService.of("sparkhistoryserver");
  public static final HopsworksService<HiveTags> HIVE = HopsworksService.of("hive", HiveTags.values());
  public static final HopsworksService<NamenodeTags> NAMENODE = HopsworksService.of("namenode",
      NamenodeTags.values());
  public static final HopsworksService<LogstashTags> LOGSTASH = HopsworksService.of("logstash",
      LogstashTags.values());
  public static final HopsworksService<GlassfishTags> GLASSFISH = HopsworksService.of("glassfish",
      GlassfishTags.values());
  public static final HopsworksService<NoTags> DOCKER_REGISTRY = HopsworksService.of("registry");
  public static final HopsworksService<NoTags> CONSUL = HopsworksService.of("consul");
  public static final HopsworksService<MysqlTags> MYSQL = HopsworksService.of("mysql", MysqlTags.values());
  public static final HopsworksService<PrometheusTags> PROMETHEUS = HopsworksService.of("prometheus",
      PrometheusTags.values());
  public static final HopsworksService<NoTags> NODE_EXPORTER = HopsworksService.of("node_exporter");
  public static final HopsworksService<NoTags> GRAFANA = HopsworksService.of("grafana");
  public static final HopsworksService<AirflowTags> AIRFLOW = HopsworksService.of("airflow",
      AirflowTags.values());
  public static final HopsworksService<FlinkTags> FLINK = HopsworksService.of("flink", FlinkTags.values());
  public static final HopsworksService<FlyingDuckTags> FLYING_DUCK = HopsworksService.of("flyingduck",
      FlyingDuckTags.values());
  public static final HopsworksService<OpenSearchTags> OPENSEARCH = HopsworksService.of("elastic",
      OpenSearchTags.values());

  public static final HopsworksService<KafkaTags> KAFKA = HopsworksService.of("kafka", KafkaTags.values());

  private final String name;
  private final T[] tags;
  private final Set<String> domains;

  private HopsworksService(String name, T[] tags) {
    this.name = name;
    this.tags = tags;
    domains = new HashSet<>();
    domains.add(getName());
    if (tags != null) {
      Arrays.stream(tags).forEach(s -> domains.add(String.format("%s.%s", s.getValue(), name)));
    }
  }

  public static <T extends ServiceTags> HopsworksService<T> of(String name) {
    return new HopsworksService<>(name, null);
  }

  public static <T extends ServiceTags> HopsworksService<T> of(String name, T[] tags) {
    return new HopsworksService<>(name, tags);
  }

  public String getName() {
    return name;
  }

  public String getNameWithTag(T tag) {
    if (tag.getValue().isEmpty()) {
      return name;
    }
    return tag.getValue() + "." + name;
  }

  public Set<String> domains() {
    return domains;
  }
}
