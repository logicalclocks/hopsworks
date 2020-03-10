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
package io.hops.hopsworks.common.elastic;

import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.search.SearchHit;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a JSONifiable version of the featurestore elastic hit object
 */
@XmlRootElement
public class ElasticFeaturestoreHit implements Comparator<ElasticFeaturestoreHit> {
  
  private static final Logger LOG = Logger.getLogger(ElasticFeaturestoreHit.class.getName());
  
  //the inode id
  private String id;
  private float score;
  
  //inode name
  private String docType;
  private String name;
  private Integer version;
  private Integer projectId;
  private String projectName;
  private Long datasetIId;
  private Map<String, Object> xattrs;

  public ElasticFeaturestoreHit() {
  }
  
  public static ElasticFeaturestoreHit instance(SearchHit hit) throws ElasticException {
    ElasticFeaturestoreHit feHit = new ElasticFeaturestoreHit();
    feHit.id = hit.getId();
    //the source of the retrieved record (i.e. all the indexed information)
    feHit.score = hit.getScore();
  
    try {
      for (Map.Entry<String, Object> entry : hit.getSourceAsMap().entrySet()) {
        //set the name explicitly so that it's easily accessible in the frontend
        if (entry.getKey().equals("doc_type")) {
          feHit.docType = entry.getValue().toString();
        } else if (entry.getKey().equals(FeaturestoreXAttrsConstants.NAME)) {
          feHit.name = entry.getValue().toString();
        } else if (entry.getKey().equals(FeaturestoreXAttrsConstants.VERSION)) {
          feHit.version = Integer.parseInt(entry.getValue().toString());
        } else if (entry.getKey().equals(FeaturestoreXAttrsConstants.PROJECT_ID)) {
          feHit.projectId = Integer.parseInt(entry.getValue().toString());
        } else if (entry.getKey().equals(FeaturestoreXAttrsConstants.PROJECT_NAME)) {
          feHit.projectName = entry.getValue().toString();
        } else if (entry.getKey().equals(FeaturestoreXAttrsConstants.DATASET_INODE_ID)) {
          feHit.datasetIId = Long.parseLong(entry.getValue().toString());
        } else if (entry.getKey().equals(FeaturestoreXAttrsConstants.ELASTIC_XATTR)) {
          feHit.xattrs = (Map)entry.getValue();
        }
      }
    } catch (NumberFormatException e) {
      String userMsg = "Internal error during query";
      String devMsg = "Hopsworks and Elastic types do not match - parsing problem";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        userMsg, devMsg, e);
    }
    return feHit;
  }
  
  @Override
  public int compare(ElasticFeaturestoreHit o1, ElasticFeaturestoreHit o2) {
    return Float.compare(o2.getScore(), o1.getScore());
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public float getScore() {
    return score;
  }
  
  public void setScore(float score) {
    this.score = score;
  }
  
  public String getDocType() {
    return docType;
  }
  
  public void setDocType(String docType) {
    this.docType = docType;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Integer getVersion() {
    return version;
  }
  
  public void setVersion(Integer version) {
    this.version = version;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  public Long getDatasetIId() {
    return datasetIId;
  }
  
  public void setDatasetIId(Long datasetIId) {
    this.datasetIId = datasetIId;
  }
  
  public Map<String, Object> getXattrs() {
    return xattrs;
  }
  
  public void setXattrs(Map<String, Object> xattrs) {
    this.xattrs = xattrs;
  }
  
  @Override
  public String toString() {
    return "FeaturestoreElasticHit{" +
      "id='" + id + '\'' +
      ", score=" + score +
      ", docType='" + docType + '\'' +
      ", name='" + name + '\'' +
      ", version=" + version +
      ", projectId=" + projectId +
      ", projectName='" + projectName + '\'' +
      ", datasetIId=" + datasetIId +
      ", features=" + xattrs +
      '}';
  }
}
