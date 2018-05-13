/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 */
package io.hops.hopsworks.api.util;

import io.hops.hopsworks.common.util.Settings;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class VersionsDTO {

  private List<Version> versions = new ArrayList<Version>();

  @XmlRootElement
  public static class Version implements Comparable {

    private String software;
    private String version;

    public Version() {
    }

    public Version(String software, String version) {
      if (software == null || version == null) {
        throw new NullPointerException("Software and/or version cannot be null");
      }
      this.software = software;
      this.version = version;
    }

    public String getSoftware() {
      return software;
    }

    public String getVersion() {
      return version;
    }

    public void setSoftware(String software) {
      this.software = software;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    @Override
    public int compareTo(Object o) {
      if (o instanceof Version == false) {
        return -1;
      }
      Version other = (Version) o;
      return this.software.compareToIgnoreCase(other.software);
    }
    
  }

  public VersionsDTO() {

  }

  public VersionsDTO(Settings settings) {

    versions.add(new Version("zookeeper", settings.getZookeeperVersion()));
    versions.add(new Version("influxdb", settings.getInfluxdbVersion()));
    versions.add(new Version("grafana", settings.getGrafanaVersion()));
    versions.add(new Version("telegraf", settings.getTelegrafVersion()));
    versions.add(new Version("kapacitor", settings.getKapacitorVersion()));
    versions.add(new Version("logstash", settings.getLogstashVersion()));
    versions.add(new Version("kibana", settings.getKibanaVersion()));
    versions.add(new Version("filebeat", settings.getFilebeatVersion()));
    versions.add(new Version("ndb", settings.getNdbVersion()));
    versions.add(new Version("livy", settings.getLivyVersion()));
    versions.add(new Version("zeppelin", settings.getZeppelinVersion()));
    versions.add(new Version("hive2", settings.getHive2Version()));
    versions.add(new Version("tez", settings.getTezVersion()));
    versions.add(new Version("slider", settings.getSliderVersion()));
    versions.add(new Version("spark", settings.getSparkVersion()));
    versions.add(new Version("flink", settings.getFlinkVersion()));
    versions.add(new Version("epipe", settings.getEpipeVersion()));
    versions.add(new Version("dela", settings.getDelaVersion()));
    versions.add(new Version("kafka", settings.getKafkaVersion()));
    versions.add(new Version("elastic", settings.getElasticVersion()));
    versions.add(new Version("drelephant", settings.getDrelephantVersion()));
    versions.add(new Version("tensorflow", settings.getTensorflowVersion()));
    versions.add(new Version("cuda", settings.getCudaVersion()));
    versions.add(new Version("hopsworks", settings.getHopsworksVersion()));
  }

  public List<Version> getVersions() {
    return versions;
  }

  public void setVersions(List<Version> versions) {
    this.versions = versions;
  }

}
