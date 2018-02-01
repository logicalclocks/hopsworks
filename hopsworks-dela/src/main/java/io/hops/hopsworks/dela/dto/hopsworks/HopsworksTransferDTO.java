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
