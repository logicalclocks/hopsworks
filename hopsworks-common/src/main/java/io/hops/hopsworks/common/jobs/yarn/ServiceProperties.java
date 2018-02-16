/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
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
