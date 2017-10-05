package io.hops.hopsworks.dela.dto.hopsworks;

import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

public class HopsworksTransferDTO {

  @XmlRootElement
  public static class Download {

    private int projectId;
    
    private String publicDSId;
    private String name;
    private List<ClusterAddressDTO> bootstrap;

    private String topics;

    public Download() {
    }

    public int getProjectId() {
      return projectId;
    }

    public void setProjectId(int projectId) {
      this.projectId = projectId;
    }

    public String getPublicDSId() {
      return publicDSId;
    }

    public void setPublicDSId(String publicDSId) {
      this.publicDSId = publicDSId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<ClusterAddressDTO> getBootstrap() {
      return bootstrap;
    }

    public void setBootstrap(List<ClusterAddressDTO> bootstrap) {
      this.bootstrap = bootstrap;
    }

    public String getTopics() {
      return topics;
    }

    public void setTopics(String topics) {
      this.topics = topics;
    }
  }
}
