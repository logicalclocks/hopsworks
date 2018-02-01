/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.yarn;

/**
 *
 * <p>
 */
public class ServiceProperties {

  private KafkaProperties kafka;
  private AnacondaProperties anaconda;
  private ElasticProperties elastic;

  private Integer projectId;
  private String projectName;
  private String restEndpoint;
  private String jobName;

  public ServiceProperties() {
  }

  public ServiceProperties(Integer projectId, String projectName, String restEndPoint, String jobName,
      ElasticProperties elastic) {
    this.projectId = projectId;
    this.projectName = projectName;
    this.restEndpoint = restEndPoint;
    this.jobName = jobName;
    this.elastic = elastic;
  }

  public KafkaProperties getKafka() {
    return kafka;
  }

  public boolean isKafkaEnabled() {
    return kafka != null;
  }

  public void setKafka(KafkaProperties kafka) {
    this.kafka = kafka;
  }

  public void initKafka() {
    kafka = new KafkaProperties();
  }

  public boolean isAnacondaEnabled() {
    return anaconda != null;
  }

  public AnacondaProperties getAnaconda() {
    return anaconda;
  }

  public void setAnaconda(AnacondaProperties anaconda) {
    this.anaconda = anaconda;
  }

  public void initAnaconda(String envPath) {
    anaconda = new AnacondaProperties(envPath);
  }

  public ElasticProperties getElastic() {
    return elastic;
  }

  public void setElastic(ElasticProperties elastic) {
    this.elastic = elastic;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getRestEndpoint() {
    return restEndpoint;
  }

  public void setRestEndpoint(String restEndPoint) {
    this.restEndpoint = restEndPoint;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

}
