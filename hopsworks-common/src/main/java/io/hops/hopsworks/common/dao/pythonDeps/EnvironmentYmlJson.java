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

package io.hops.hopsworks.common.dao.pythonDeps;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EnvironmentYmlJson {

  private String allYmlPath;
  private String cpuYmlPath;
  private String gpuYmlPath;
  private String pythonKernelEnable;

  public EnvironmentYmlJson() {
  }

  public EnvironmentYmlJson(String allYmlPath, String cpuYmlPath, String gpuYmlPath, String pythonKernelEnable) {
    this.allYmlPath = allYmlPath;
    this.cpuYmlPath = cpuYmlPath;
    this.gpuYmlPath = gpuYmlPath;
    this.pythonKernelEnable = pythonKernelEnable;
  }

  public String getCpuYmlPath() {
    return cpuYmlPath;
  }

  public void setCpuYmlPath(String cpuYmlPath) {
    this.cpuYmlPath = cpuYmlPath;
  }

  public String getGpuYmlPath() {
    return gpuYmlPath;
  }

  public void setGpuYmlPath(String gpuYmlPath) {
    this.gpuYmlPath = gpuYmlPath;
  }

  public String getPythonKernelEnable() {
    return pythonKernelEnable;
  }

  public void setPythonKernelEnable(String pythonKernelEnable) {
    this.pythonKernelEnable = pythonKernelEnable;
  }

  public String getAllYmlPath() {
    return allYmlPath;
  }

  public void setAllYmlPath(String allYmlPath) {
    this.allYmlPath = allYmlPath;
  }
}
