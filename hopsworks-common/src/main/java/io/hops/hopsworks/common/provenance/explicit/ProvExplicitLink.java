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
package io.hops.hopsworks.common.provenance.explicit;

import io.hops.hopsworks.persistence.entity.provenance.ProvExplicitNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ProvExplicitLink<T> {
  private T node;
  private List<ProvExplicitLink> upstream = new ArrayList<>();
  private List<ProvExplicitLink> downstream = new ArrayList<>();
  private ProvExplicitNode.Type artifactType;
  private boolean traversed = false;
  private boolean shared = false;
  private boolean accessible = false;
  private boolean deleted = false;
  private Function<T, Integer> idProvider;
  
  public ProvExplicitLink() {
  }
  
  public T getNode() {
    return node;
  }
  
  public void setNode(T node, Function<T, Integer> idProvider) {
    this.node = node;
    this.idProvider = idProvider;
  }
  
  public ProvExplicitNode.Type getArtifactType() {
    return artifactType;
  }
  
  public void setArtifactType(ProvExplicitNode.Type artifactType) {
    this.artifactType = artifactType;
  }
  
  public boolean isTraversed() {
    return traversed;
  }
  
  public void setTraversed() {
    this.traversed = true;
  }
  
  public void setTraversed(boolean traversed) {
    this.traversed = traversed;
  }
  
  public boolean isShared() {
    return shared;
  }
  
  public void setShared(boolean shared) {
    this.shared = shared;
  }
  
  public boolean isAccessible() {
    return accessible;
  }
  
  public void setAccessible(boolean accessible) {
    this.accessible = accessible;
  }
  
  public boolean isDeleted() {
    return deleted;
  }
  
  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }
  
  public List<ProvExplicitLink> getUpstream() {
    return upstream;
  }
  
  public void setUpstream(List<ProvExplicitLink> upstream) {
    this.upstream = upstream;
  }
  
  public void addUpstream(ProvExplicitLink node) {
    upstream.add(node);
  }
  
  public List<ProvExplicitLink> getDownstream() {
    return downstream;
  }
  
  public void setDownstream(List<ProvExplicitLink> downstream) {
    this.downstream = downstream;
  }
  
  public void addDownstream(ProvExplicitLink node) {
    downstream.add(node);
  }
  
  public Integer getNodeId() {
    return idProvider.apply(node);
  }
}
