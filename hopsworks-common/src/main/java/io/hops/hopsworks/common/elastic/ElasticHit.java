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

package io.hops.hopsworks.common.elastic;

import io.hops.hopsworks.common.util.Settings;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.XmlRootElement;
import org.elasticsearch.search.SearchHit;

/**
 * Represents a JSONifiable version of the elastic hit object
 */
@XmlRootElement
public class ElasticHit implements Comparator<ElasticHit> {

  private static final Logger LOG = Logger.getLogger(ElasticHit.class.getName());

  //the inode id
  private String id;
  //inode name 
  private String name;
  private String description;
  private String publicId;
  //whether the inode is a parent, a child or a dataset
  private String type;
  private boolean localDataset;
  private boolean public_ds;
  private float score;
  //the rest of the hit (search match) data
  private Map<String, Object> map;

  public ElasticHit() {
  }

  public ElasticHit(SearchHit hit) {
    //the source of the retrieved record (i.e. all the indexed information)
    this.map = hit.getSourceAsMap();
    this.score = hit.getScore();
    //export the name of the retrieved record from the list
    for (Entry<String, Object> entry : map.entrySet()) {
      //set the name explicitly so that it's easily accessible in the frontend
      if (entry.getKey().equals("name")) {
        this.name = entry.getValue().toString();
      } else if (entry.getKey().equals("description")) {
        this.description = entry.getValue().toString();
      } else if(entry.getKey().equals("public_ds")) {
        this.public_ds = Boolean.valueOf(entry.getValue().toString());
      } else if (entry.getKey().equals(Settings.META_DOC_TYPE_FIELD)) {
        this.type = entry.getValue().toString();
        if (!type.equals(Settings.DOC_TYPE_INODE) && !type.equals(Settings.DOC_TYPE_DATASET) && !type.equals(
            Settings.DOC_TYPE_PROJECT)) {
          LOG.log(Level.WARNING, "Got a wrong document type [{0}] was expecting one of these types [{1}, {2}, {3}]",
              new Object[]{type, Settings.DOC_TYPE_INODE, Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT});
        }
      }
    }
    
    if(type.equals(Settings.DOC_TYPE_PROJECT)){
      this.id = map.get(Settings.META_PROJECT_ID_FIELD).toString();
    }else{
      //the id of the retrieved hit (i.e. the inode_id)
      this.id = hit.getId();
    }
  }

  public boolean isPublic_ds() {
    return public_ds;
  }

  public void setPublic_ds(boolean public_ds) {
    this.public_ds = public_ds;
  }

  
  public boolean isLocalDataset() {
    return localDataset;
  }

  public void setLocalDataset(boolean localDataset) {
    this.localDataset = localDataset;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public float getScore() {
    return score;
  }

  public void setScore(float score) {
    this.score = score;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public final void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return this.type;
  }

  public void setMap(Map<String, Object> source) {
    this.map = new HashMap<>(source);
  }

  public Map<String, String> getMap() {
    //flatten hits (remove nested json objects) to make it more readable
    Map<String, String> refined = new HashMap<>();

    if (this.map != null) {
      for (Entry<String, Object> entry : this.map.entrySet()) {
        //convert value to string
        String value = (entry.getValue() == null) ? "null" : entry.getValue().
          toString();
        refined.put(entry.getKey(), value);
      }
    }

    return refined;
  }

  @Override
  public int compare(ElasticHit o1, ElasticHit o2) {
    return Float.compare(o2.getScore(), o1.getScore());
  }
}
