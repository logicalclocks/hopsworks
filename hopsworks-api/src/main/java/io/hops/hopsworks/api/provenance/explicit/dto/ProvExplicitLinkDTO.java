/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.explicit.dto;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class ProvExplicitLinkDTO<A extends RestDTO> extends RestDTO<ProvExplicitLinkDTO> {
  private ProvNodeDTO<A> node;
  private List<ProvExplicitLinkDTO> upstream = new ArrayList<>();
  private List<ProvExplicitLinkDTO> downstream = new ArrayList<>();
  
  public ProvExplicitLinkDTO() {
  }
  
  public ProvNodeDTO<A> getNode() {
    return node;
  }
  
  public void setNode(ProvNodeDTO<A> node) {
    this.node = node;
  }
  
  public List<ProvExplicitLinkDTO> getUpstream() {
    return upstream;
  }
  
  public void setUpstream(List<ProvExplicitLinkDTO> upstream) {
    this.upstream = upstream;
  }
  
  public void addUpstream(ProvExplicitLinkDTO link) {
    upstream.add(link);
  }
  
  public List<ProvExplicitLinkDTO> getDownstream() {
    return downstream;
  }
  
  public void setDownstream(List<ProvExplicitLinkDTO> downstream) {
    this.downstream = downstream;
  }
  
  public void addDownstream(ProvExplicitLinkDTO link) {
    downstream.add(link);
  }
}
