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

public class KernelTemplateBuilder {
  private String hdfsUser;
  private String hadoopHome;
  private Project project;
  
  private String anacondaHome;
  private String hadoopVersion;
  private String secretDirectory;
  private String hiveEndpoints;
  private String libHdfsOpts;
  
  private KernelTemplateBuilder() {}
  
  public static KernelTemplateBuilder newBuilder() {
    return new KernelTemplateBuilder();
  }
  
  public KernelTemplateBuilder setHdfsUser(String hdfsUser) {
    this.hdfsUser = hdfsUser;
    return this;
  }
  
  public String getHdfsUser() {
    return hdfsUser;
  }
  
  public KernelTemplateBuilder setHadoopHome(String hadoopHome) {
    this.hadoopHome = hadoopHome;
    return this;
  }
  
  public String getHadoopHome() {
    return hadoopHome;
  }
  
  public KernelTemplateBuilder setProject(Project project) {
    this.project = project;
    return this;
  }
  
  public Project getProject() {
    return project;
  }
  
  public KernelTemplateBuilder setAnacondaHome(String anacondaHome) {
    this.anacondaHome = anacondaHome;
    return this;
  }
  
  public String getAnacondaHome() {
    return anacondaHome;
  }
  
  public KernelTemplateBuilder setHadoopVersion(String hadoopVersion) {
    this.hadoopVersion = hadoopVersion;
    return this;
  }
  
  public String getHadoopVersion() {
    return hadoopVersion;
  }
  
  public KernelTemplateBuilder setSecretDirectory(String secretDirectory) {
    this.secretDirectory = secretDirectory;
    return this;
  }
  
  public String getSecretDirectory() {
    return secretDirectory;
  }
  
  public KernelTemplateBuilder setHiveEndpoints(String hiveEndpoints) {
    this.hiveEndpoints = hiveEndpoints;
    return this;
  }

  public String getLibHdfsOpts() {
    return libHdfsOpts;
  }

  public KernelTemplateBuilder setLibHdfsOpts(String libHdfsOpts) {
    this.libHdfsOpts = libHdfsOpts;
    return this;
  }
  
  public String getHiveEndpoints() {
    return hiveEndpoints;
  }
  
  public KernelTemplate build() {
    return new KernelTemplate(this);
  }
}
