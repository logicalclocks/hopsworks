package io.hops.hopsworks.api.dela.dto;

import io.hops.hopsworks.dela.dto.common.ClusterAddressDTO;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BootstrapDTO {
  private List<ClusterAddressDTO> bootstrap;

  public List<ClusterAddressDTO> getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(List<ClusterAddressDTO> bootstrap) {
    this.bootstrap = bootstrap;
  }
}
