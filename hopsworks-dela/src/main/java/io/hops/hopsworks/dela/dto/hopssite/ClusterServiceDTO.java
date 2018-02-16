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

package io.hops.hopsworks.dela.dto.hopssite;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

public class ClusterServiceDTO {
  @XmlRootElement
  public static class Register {

    private String delaTransferAddress;
    private String delaClusterAddress;

    public Register() {
    }

    public Register(String delaTransferAddress, String delaClusterAddress) {
      this.delaTransferAddress = delaTransferAddress;
      this.delaClusterAddress = delaClusterAddress;
    }

    public String getDelaTransferAddress() {
      return delaTransferAddress;
    }

    public void setDelaTransferAddress(String delaTransferAddress) {
      this.delaTransferAddress = delaTransferAddress;
    }

    public String getDelaClusterAddress() {
      return delaClusterAddress;
    }

    public void setDelaClusterAddress(String delaClusterAddress) {
      this.delaClusterAddress = delaClusterAddress;
    }
  }
  
  @XmlRootElement
  public static class HeavyPing {

    private List<String> upldDSIds;
    private List<String> dwnlDSIds;

    public HeavyPing() {
    }

    public HeavyPing(List<String> upldDSIds, List<String> dwnlDSIds) {
      this.upldDSIds = upldDSIds;
      this.dwnlDSIds = dwnlDSIds;
    }

    public List<String> getUpldDSIds() {
      return upldDSIds;
    }

    public void setUpldDSIds(List<String> upldDSIds) {
      this.upldDSIds = upldDSIds;
    }

    public List<String> getDwnlDSIds() {
      return dwnlDSIds;
    }

    public void setDwnlDSIds(List<String> dwnlDSIds) {
      this.dwnlDSIds = dwnlDSIds;
    }
  }
  
  @XmlRootElement
  public static class Ping {
    private int upldDSSize;
    private int dwnlDSSize;

    public Ping() {
    }

    public Ping(int upldDSSize, int dwnlDSSize) {
      this.upldDSSize = upldDSSize;
      this.dwnlDSSize = dwnlDSSize;
    }

    public int getUpldDSSize() {
      return upldDSSize;
    }

    public void setUpldDSSize(int upldDSSize) {
      this.upldDSSize = upldDSSize;
    }

    public int getDwnlDSSize() {
      return dwnlDSSize;
    }

    public void setDwnlDSSize(int dwnlDSSize) {
      this.dwnlDSSize = dwnlDSSize;
    }
  }
}
