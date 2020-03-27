/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.util.templates.jupyter;

import io.hops.hopsworks.persistence.entity.project.Project;

public class JupyterNotebookConfigTemplateBuilder {
  private String hdfsUser;
  private String hadoopHome;
  private Project project;
  
  private String namenodeIp;
  private String namenodePort;
  private String contentsManager;
  private String hopsworksEndpoint;
  private String elasticEndpoint;
  private Integer port;
  private String baseDirectory;
  private String whiteListedKernels;
  private String jupyterCertsDirectory;
  private String secretDirectory;
  private String allowOrigin;
  private Long wsPingInterval;
  private String apiKey;
  private String flinkConfDirectory;
  private Boolean requestsVerify;
  private String domainCATruststorePem;
  private String serviceDiscoveryDomain = "consul";
  
  private JupyterNotebookConfigTemplateBuilder() {}
  
  public static JupyterNotebookConfigTemplateBuilder newBuilder() {
    return new JupyterNotebookConfigTemplateBuilder();
  }
  
  public JupyterNotebookConfigTemplateBuilder setHdfsUser(String hdfsUser) {
    this.hdfsUser = hdfsUser;
    return this;
  }
  
  public String getHdfsUser() {
    return hdfsUser;
  }
  
  public JupyterNotebookConfigTemplateBuilder setHadoopHome(String hadoopHome) {
    this.hadoopHome = hadoopHome;
    return this;
  }
  
  public String getHadoopHome() {
    return hadoopHome;
  }
  
  public JupyterNotebookConfigTemplateBuilder setProject(Project project) {
    this.project = project;
    return this;
  }
  
  public Project getProject() {
    return project;
  }
  
  public String getNamenodeIp() {
    return namenodeIp;
  }
  
  public JupyterNotebookConfigTemplateBuilder setNamenodeIp(String namenodeIp) {
    this.namenodeIp = namenodeIp;
    return this;
  }
  
  public String getNamenodePort() {
    return namenodePort;
  }
  
  public JupyterNotebookConfigTemplateBuilder setNamenodePort(String namenodePort) {
    this.namenodePort = namenodePort;
    return this;
  }
  
  public String getContentsManager() {
    return contentsManager;
  }
  
  public JupyterNotebookConfigTemplateBuilder setContentsManager(String contentsManager) {
    this.contentsManager = contentsManager;
    return this;
  }
  
  public String getHopsworksEndpoint() {
    return hopsworksEndpoint;
  }
  
  public JupyterNotebookConfigTemplateBuilder setHopsworksEndpoint(String hopsworksEndpoint) {
    this.hopsworksEndpoint = hopsworksEndpoint;
    return this;
  }
  
  public String getElasticEndpoint() {
    return elasticEndpoint;
  }
  
  public JupyterNotebookConfigTemplateBuilder setElasticEndpoint(String elasticEndpoint) {
    this.elasticEndpoint = elasticEndpoint;
    return this;
  }
  
  public Integer getPort() {
    return port;
  }
  
  public JupyterNotebookConfigTemplateBuilder setPort(Integer port) {
    this.port = port;
    return this;
  }
  
  public String getBaseDirectory() {
    return baseDirectory;
  }
  
  public JupyterNotebookConfigTemplateBuilder setBaseDirectory(String baseDirectory) {
    this.baseDirectory = baseDirectory;
    return this;
  }
  
  public String getWhiteListedKernels() {
    return whiteListedKernels;
  }
  
  public JupyterNotebookConfigTemplateBuilder setWhiteListedKernels(String whiteListedKernels) {
    this.whiteListedKernels = whiteListedKernels;
    return this;
  }
  
  public String getJupyterCertsDirectory() {
    return jupyterCertsDirectory;
  }
  
  public JupyterNotebookConfigTemplateBuilder setJupyterCertsDirectory(String jupyterCertsDirectory) {
    this.jupyterCertsDirectory = jupyterCertsDirectory;
    return this;
  }
  
  public String getSecretDirectory() {
    return secretDirectory;
  }
  
  public JupyterNotebookConfigTemplateBuilder setSecretDirectory(String secretDirectory) {
    this.secretDirectory = secretDirectory;
    return this;
  }
  
  public String getAllowOrigin() {
    return allowOrigin;
  }
  
  public JupyterNotebookConfigTemplateBuilder setAllowOrigin(String allowOrigin) {
    this.allowOrigin = allowOrigin;
    return this;
  }
  
  public Long getWsPingInterval() {
    return wsPingInterval;
  }
  
  public JupyterNotebookConfigTemplateBuilder setWsPingInterval(Long wsPingInterval) {
    this.wsPingInterval = wsPingInterval;
    return this;
  }
  
  public String getApiKey() {
    return apiKey;
  }
  
  public JupyterNotebookConfigTemplateBuilder setApiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }
  
  public String getFlinkConfDirectory() {
    return flinkConfDirectory;
  }
  
  public JupyterNotebookConfigTemplateBuilder setFlinkConfDirectory(String flinkConfDirectory) {
    this.flinkConfDirectory = flinkConfDirectory;
    return this;
  }
  
  public Boolean getRequestsVerify() {
    return requestsVerify;
  }
  
  public JupyterNotebookConfigTemplateBuilder setRequestsVerify(Boolean requestsVerify) {
    this.requestsVerify = requestsVerify;
    return this;
  }
  
  public String getDomainCATruststorePem() {
    return domainCATruststorePem;
  }
  
  public JupyterNotebookConfigTemplateBuilder setDomainCATruststorePem(String domainCATruststorePem) {
    this.domainCATruststorePem = domainCATruststorePem;
    return this;
  }
  
  public String getServiceDiscoveryDomain() {
    return serviceDiscoveryDomain;
  }
  
  public JupyterNotebookConfigTemplateBuilder setServiceDiscoveryDomain(String serviceDiscoveryDomain) {
    this.serviceDiscoveryDomain = serviceDiscoveryDomain;
    return this;
  }
  
  public JupyterNotebookConfigTemplate build() {
    return new JupyterNotebookConfigTemplate(this);
  }
}
