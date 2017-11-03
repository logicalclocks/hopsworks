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
