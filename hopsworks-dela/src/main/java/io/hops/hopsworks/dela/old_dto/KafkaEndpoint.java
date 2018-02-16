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

package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class KafkaEndpoint {

  private String brokerEndpoint;
  private String restEndpoint;
  private String domain;
  private String projectId;
  private String keyStore;
  private String trustStore;

  public KafkaEndpoint() {
  }

  public KafkaEndpoint(String brokerEndpoint, String restEndpoint, String domain,
          String projectId, String keyStore, String trustStore) {
    this.brokerEndpoint = brokerEndpoint;
    this.restEndpoint = restEndpoint;
    this.domain = domain;
    this.projectId = projectId;
    this.keyStore = keyStore;
    this.trustStore = trustStore;
  }

  public String getBrokerEndpoint() {
    return brokerEndpoint;
  }

  public String getRestEndpoint() {
    return restEndpoint;
  }

  public String getDomain() {
    return domain;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getKeyStore() {
    return keyStore;
  }

  public String getTrustStore() {
    return trustStore;
  }

  public void setBrokerEndpoint(String brokerEndpoint) {
    this.brokerEndpoint = brokerEndpoint;
  }

  public void setRestEndpoint(String restEndpoint) {
    this.restEndpoint = restEndpoint;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public void setKeyStore(String keyStore) {
    this.keyStore = keyStore;
  }

  public void setTrustStore(String trustStore) {
    this.trustStore = trustStore;
  }

}
