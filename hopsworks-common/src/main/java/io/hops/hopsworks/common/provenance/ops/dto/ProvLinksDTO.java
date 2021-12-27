/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.ops.dto;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@XmlRootElement
public class ProvLinksDTO extends RestDTO<ProvLinksDTO> {
  String appId;
  Map<String, ProvOpsDTO> in = new HashMap<>();
  Map<String, ProvOpsDTO> out = new HashMap<>();
  ProvOpsDTO root = null;
  List<ProvLinksDTO> upstreamLinks = new ArrayList<>();
  List<ProvLinksDTO> downstreamLinks = new ArrayList<>();
  boolean maxProvenanceGraphSizeReached = false;

  public ProvLinksDTO() {}
  
  public ProvLinksDTO(String appId, Map<String, ProvOpsDTO> in, Map<String, ProvOpsDTO> out) {
    this.in = in;
    this.out = out;
  }
  
  public String getAppId() {
    return appId;
  }
  
  public void setAppId(String appId) {
    this.appId = appId;
  }
  
  public Map<String, ProvOpsDTO> getIn() {
    return in;
  }
  
  public void setIn(Map<String, ProvOpsDTO> in) {
    this.in = in;
  }
  
  public Map<String, ProvOpsDTO> getOut() {
    return out;
  }
  
  public void setOut(Map<String, ProvOpsDTO> out) {
    this.out = out;
  }

  public ProvOpsDTO getRoot() {
    return root;
  }

  public void setRoot(ProvOpsDTO root) {
    this.root = root;
  }

  public List<ProvLinksDTO> getUpstreamLinks() {
    return upstreamLinks;
  }

  public void setUpstreamLinks(List<ProvLinksDTO> upstreamLinks) {
    this.upstreamLinks = upstreamLinks;
  }

  public void addUpstreamLink(ProvLinksDTO link) {
    this.upstreamLinks.add(link);
  }

  public List<ProvLinksDTO> getDownstreamLinks() {
    return downstreamLinks;
  }

  public void setDownstreamLinks(List<ProvLinksDTO> downstreamLinks) {
    this.downstreamLinks = downstreamLinks;
  }

  public void addDownstreamLink(ProvLinksDTO link) {
    this.downstreamLinks.add(link);
  }

  public void setMaxProvenanceGraphSizeReached(boolean maxProvenanceGraphSizeReached) {
    this.maxProvenanceGraphSizeReached = maxProvenanceGraphSizeReached;
  }

  public boolean isMaxProvenanceGraphSizeReached() {
    return maxProvenanceGraphSizeReached;
  }
  
  
  public static class Builder {
    //<app_id, <in, out>>
    Map<String, ProvLinksDTO> appLinks = new HashMap<>();
  
    public void addOutArtifacts(String appId, Map<String, ProvOpsDTO> artifacts) {
      addArtifacts(appId, Collections.emptyMap(), artifacts);
    }
  
    public void addInArtifacts(String appId, Map<String, ProvOpsDTO> artifacts) {
      addArtifacts(appId, artifacts, Collections.emptyMap());
    }
    
    public void addArtifacts(String appId, Map<String, ProvOpsDTO> in, Map<String, ProvOpsDTO>  out) {
      ProvLinksDTO appArtifacts = appLinks.get(appId);
      if(appArtifacts == null) {
        appArtifacts = new ProvLinksDTO();
        appLinks.put(appId, appArtifacts);
        appArtifacts.setAppId(appId);
      }
      appArtifacts.in.putAll(in);
      appArtifacts.out.putAll(out);
    }
  
    public Map<String, ProvLinksDTO> getAppLinks() {
      return appLinks;
    }
  
    public ProvLinksDTO build() {
      ProvLinksDTO result = new ProvLinksDTO();
      result.setItems(new ArrayList<>(appLinks.values()));
      result.setCount((long)appLinks.size());
      return result;
    }
  }
}
