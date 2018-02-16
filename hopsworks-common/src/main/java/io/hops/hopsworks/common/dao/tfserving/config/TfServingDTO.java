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

package io.hops.hopsworks.common.dao.tfserving.config;

import io.hops.hopsworks.common.dao.jupyter.config.JupyterDTO;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TfServingDTO {

  private int port=0;
  private BigInteger pid=BigInteger.valueOf(1);
  private String hostIp="";
  private int exitValue=-1;

  public TfServingDTO(BigInteger pid, int port, int exitValue) {
    this.port = port;
    this.pid = pid;
    this.exitValue = exitValue;
    try {
      this.hostIp = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException ex) {
      Logger.getLogger(JupyterDTO.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public BigInteger getPid() {
    return pid;
  }

  public void setPid(BigInteger pid) {
    this.pid = pid;
  }

  public String getHostIp() {
    return hostIp;
  }

  public void setHostIp(String hostIp) {
    this.hostIp = hostIp;
  }

  public int getExitValue() {
    return exitValue;
  }

  public void setExitValue(int exitValue) {
    this.exitValue = exitValue;
  }
}
