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

package io.hops.hopsworks.common.dao.metadata;

import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JSONifiable version of the Metadata entity so that it is restful accessed.
 * It either represents a flat list of metadata, or a group of metadata
 */
@XmlRootElement
public final class MetadataView {

  private String tagName;
  private String data;
  private Integer id;
  private Integer fieldid;
  private Integer tupleid;
  private List<String> content;
  //sub-views inside this view. Useful when grouping data under a parent
  //entity (a field or a table)
  private List<MetadataView> metadataview;

  public MetadataView() {
  }

  //a single metadata instance: an id and a string value
  public MetadataView(Integer id, String data) {
    this.id = id;
    this.data = data;
  }

  public MetadataView(String tagName) {
    this.tagName = tagName;
    this.content = new LinkedList<>();
    this.metadataview = new LinkedList<>();
  }

  public MetadataView(Metadata m) {
    this.data = m.getData();
    this.id = m.getMetadataPK().getId();
    this.fieldid = m.getMetadataPK().getFieldid();
    this.tupleid = m.getMetadataPK().getTupleid();
  }

  public List<MetadataView> getMetadataView() {
    return this.metadataview;
  }

  public void setMetadataView(List<MetadataView> metadataview) {
    this.metadataview = metadataview;
  }

  public List<String> getContent() {
    return this.content;
  }

  public void setContent(List<String> content) {
    this.content = content;
  }

  public String getTagName() {
    return this.tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  public String getData() {
    return this.data;
  }

  public Integer getId() {
    return this.id;
  }

  public Integer getFieldid() {
    return this.fieldid;
  }

  public Integer getTupleid() {
    return this.tupleid;
  }

  public void setData(String data) {
    this.data = data;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setFieldid(Integer fieldid) {
    this.fieldid = fieldid;
  }

  public void setTupleid(Integer tupleid) {
    this.tupleid = tupleid;
  }
}
