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

public class JupyterNotebookConfigTemplate extends JupyterTemplate {
  public static final String TEMPLATE_NAME = "jupyter_notebook_config_template.py";
  public static final String FILE_NAME = "jupyter_notebook_config.py";
  
  private final String namenodeIp;
  private final String namenodePort;
  private final String contentsManager;
  private final String hopsworksEndpoint;
  private final String elasticEndpoint;
  private final Integer port;
  private final String baseDirectory;
  private final String whiteListedKernels;
  private final String jupyterCertsDirectory;
  private final String secretDirectory;
  private final String allowOrigin;
  private final Long wsPingInterval;
  private final String apiKey;
  private final String flinkConfDirectory;
  private final Boolean requestsVerify;
  private final String domainCATruststorePem;
  private final String serviceDiscoveryDomain;
  
  public JupyterNotebookConfigTemplate(JupyterNotebookConfigTemplateBuilder builder) {
    super(builder.getHdfsUser(), builder.getHadoopHome(), builder.getProject());
    this.namenodeIp = builder.getNamenodeIp();
    this.namenodePort = builder.getNamenodePort();
    this.contentsManager = builder.getContentsManager();
    this.hopsworksEndpoint = builder.getHopsworksEndpoint();
    this.elasticEndpoint = builder.getElasticEndpoint();
    this.port = builder.getPort();
    this.baseDirectory = builder.getBaseDirectory();
    this.whiteListedKernels = builder.getWhiteListedKernels();
    this.jupyterCertsDirectory = builder.getJupyterCertsDirectory();
    this.secretDirectory = builder.getSecretDirectory();
    this.allowOrigin = builder.getAllowOrigin();
    this.wsPingInterval = builder.getWsPingInterval();
    this.apiKey = builder.getApiKey();
    this.flinkConfDirectory = builder.getFlinkConfDirectory();
    this.requestsVerify = builder.getRequestsVerify();
    this.domainCATruststorePem = builder.getDomainCATruststorePem();
    this.serviceDiscoveryDomain = builder.getServiceDiscoveryDomain();
  }
  
  public String getNamenodeIp() {
    return namenodeIp;
  }
  
  public String getNamenodePort() {
    return namenodePort;
  }
  
  public String getContentsManager() {
    return contentsManager;
  }
  
  public String getHopsworksEndpoint() {
    return hopsworksEndpoint;
  }
  
  public String getElasticEndpoint() {
    return elasticEndpoint;
  }
  
  public Integer getPort() {
    return port;
  }
  
  public String getBaseDirectory() {
    return baseDirectory;
  }

  public String getWhiteListedKernels() {
    return whiteListedKernels;
  }
  
  public String getJupyterCertsDirectory() {
    return jupyterCertsDirectory;
  }
  
  public String getSecretDirectory() {
    return secretDirectory;
  }
  
  public String getAllowOrigin() {
    return allowOrigin;
  }
  
  public Long getWsPingInterval() {
    return wsPingInterval;
  }
  
  public String getApiKey() {
    return apiKey;
  }
  
  public String getFlinkConfDirectory() {
    return flinkConfDirectory;
  }
  
  public Boolean getRequestsVerify() {
    return requestsVerify;
  }
  
  public String getDomainCATruststorePem() {
    return domainCATruststorePem;
  }
  
  public String getServiceDiscoveryDomain() {
    return serviceDiscoveryDomain;
  }
}
