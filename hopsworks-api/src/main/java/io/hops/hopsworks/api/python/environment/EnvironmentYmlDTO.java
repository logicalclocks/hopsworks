/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.python.environment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@ApiModel(value="EnvironmentYmlDTO")
public class EnvironmentYmlDTO {
  
  private String allYmlPath;
  private String cpuYmlPath;
  private String gpuYmlPath;
  private Boolean pythonKernelEnable;
  private Boolean installJupyter;
  
  public EnvironmentYmlDTO() {
  }
  
  public EnvironmentYmlDTO(String allYmlPath, String cpuYmlPath, String gpuYmlPath, Boolean pythonKernelEnable,
    Boolean installJupyter) {
    this.allYmlPath = allYmlPath;
    this.cpuYmlPath = cpuYmlPath;
    this.gpuYmlPath = gpuYmlPath;
    this.pythonKernelEnable = pythonKernelEnable;
    this.installJupyter = installJupyter;
  }
  
  @ApiModelProperty(value = "Path to a yml with libraries to be installed on all machine types.")
  public String getAllYmlPath() {
    return allYmlPath;
  }
  
  public void setAllYmlPath(String allYmlPath) {
    this.allYmlPath = allYmlPath;
  }
  
  @ApiModelProperty(value = "Path to a yml with libraries to be installed on CPU machines.")
  public String getCpuYmlPath() {
    return cpuYmlPath;
  }
  
  public void setCpuYmlPath(String cpuYmlPath) {
    this.cpuYmlPath = cpuYmlPath;
  }
  
  @ApiModelProperty(value = "Path to a yml with libraries to be installed on GPU machines.")
  public String getGpuYmlPath() {
    return gpuYmlPath;
  }
  
  public void setGpuYmlPath(String gpuYmlPath) {
    this.gpuYmlPath = gpuYmlPath;
  }
  
  @ApiModelProperty(value = "Enable python kernel for the environment.")
  public Boolean getPythonKernelEnable() {
    return pythonKernelEnable;
  }
  
  public void setPythonKernelEnable(Boolean pythonKernelEnable) {
    this.pythonKernelEnable = pythonKernelEnable;
  }
  
  @ApiModelProperty(value = "Install Jupyter in the environment.")
  public Boolean getInstallJupyter() {
    return installJupyter;
  }
  
  public void setInstallJupyter(Boolean installJupyter) {
    this.installJupyter = installJupyter;
  }
  
  @Override
  public String toString() {
    return "EnvironmentYmlDTO{" +
      "allYmlPath='" + allYmlPath + '\'' +
      ", cpuYmlPath='" + cpuYmlPath + '\'' +
      ", gpuYmlPath='" + gpuYmlPath + '\'' +
      ", pythonKernelEnable=" + pythonKernelEnable +
      ", installJupyter=" + installJupyter +
      '}';
  }
}
