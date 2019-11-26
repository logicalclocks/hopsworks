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

public class KernelTemplate extends JupyterTemplate {
  public static final String TEMPLATE_NAME = "kernel_template.json";
  public static final String FILE_NAME = "kernel.json";
  
  private final String anacondaHome;
  private final String hadoopVersion;
  private final String secretDirectory;
  private final String hiveEndpoints;
  private final String libHdfsOpts;
  
  public KernelTemplate(KernelTemplateBuilder builder) {
    super(builder.getHdfsUser(), builder. getHadoopHome(), builder.getProject());
    this.anacondaHome = builder.getAnacondaHome();
    this.hadoopVersion = builder.getHadoopVersion();
    this.secretDirectory = builder.getSecretDirectory();
    this.hiveEndpoints = builder.getHiveEndpoints();
    this.libHdfsOpts = builder.getLibHdfsOpts();
  }
  
  public String getAnacondaHome() {
    return anacondaHome;
  }
  
  public String getHadoopVersion() {
    return hadoopVersion;
  }
  
  public String getSecretDirectory() {
    return secretDirectory;
  }
  
  public String getHiveEndpoints() {
    return hiveEndpoints;
  }

  public String getLibHdfsOpts() {
    return libHdfsOpts;
  }
}
