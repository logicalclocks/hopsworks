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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@XmlRootElement
public class ProvLinksDTO extends RestDTO<ProvLinksDTO> {
  Map<String, ProvOpsDTO> in = new HashMap<>();
  Map<String, ProvOpsDTO> out = new HashMap<>();
  
  public ProvLinksDTO() {}
  
  public ProvLinksDTO(Map<String, ProvOpsDTO> in, Map<String, ProvOpsDTO> out) {
    this.in = in;
    this.out = out;
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
