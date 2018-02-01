/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
