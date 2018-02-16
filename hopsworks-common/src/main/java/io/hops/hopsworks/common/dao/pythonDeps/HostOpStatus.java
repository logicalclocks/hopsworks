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
public class HostOpStatus {

  private String hostId;
  private String status;

  public HostOpStatus() {
  }

  public HostOpStatus(String hostId, String status) {
    this.hostId = hostId;
    this.status = status;
  }

  public String getHostId() {
    return hostId;
  }

  public void setHostId(String hostId) {
    this.hostId = hostId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return hostId + ":" + status;
  }
  
  // Two versions are equal if they have the same 'name', status doesn't matter.
  @Override
  public boolean equals(Object o) {
    if (o instanceof HostOpStatus == false) {
      return false;
    }
    HostOpStatus v = (HostOpStatus) o;
    if (v.hostId.equals(this.hostId)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hostId.hashCode() * 17; //To change body of generated methods, choose Tools | Templates.
  }

}
