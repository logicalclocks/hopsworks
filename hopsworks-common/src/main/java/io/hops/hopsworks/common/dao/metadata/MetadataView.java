/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
