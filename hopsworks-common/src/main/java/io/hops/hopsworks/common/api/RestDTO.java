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

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public abstract class RestDTO<E, D> {
  
  private URI href;
  protected Boolean expand;
  protected List<D> items;
  
  public RestDTO(URI href) {
    this.href = href;
  }
  
  public RestDTO(URI href, ResourceProperties resourceProperties, ResourceProperties.Name resource) {
    this.href = href;
    if (resourceProperties != null) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(resource);
      if (property != null) {
        this.expand = true;
      }
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
  
  public Boolean isExpand() {
    return expand != null ? expand : false;
  }
  
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
}