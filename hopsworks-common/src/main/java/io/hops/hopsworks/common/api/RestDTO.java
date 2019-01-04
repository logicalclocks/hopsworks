/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public abstract class RestDTO<D> {
  
  private URI href;
  protected Boolean expand;
  protected List<D> items;
  protected Long count = null;
  
  public RestDTO(URI href) {
    this.href = href;
  }
  
  public RestDTO(URI href, ResourceRequest resourceRequest) {
    this.href = href;
    if (resourceRequest != null) {
      this.expand = true;
    }
  }
  
  public RestDTO() {
  }
  
  public URI getHref() {
    return href;
  }
  
  public void setHref(URI href) {
    this.href = href;
  }
  
  @XmlTransient
  @JsonIgnore
  public Boolean isExpand() {
    return expand != null ? expand : false;
  }
  
  @XmlTransient
  @JsonIgnore
  public void setExpand(Boolean expand) {
    this.expand = expand;
  }
  
  public List<D> getItems() {
    return items;
  }
  
  public void setItems(List<D> items) {
    this.items = items;
  }
  
  public void addItems(List<D> items) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.addAll(items);
  }
  
  public void addItem(D item) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(item);
  }
  
  public Long getCount() {
    return count;
  }
  
  public void setCount(Long count) {
    this.count = count;
  }
}