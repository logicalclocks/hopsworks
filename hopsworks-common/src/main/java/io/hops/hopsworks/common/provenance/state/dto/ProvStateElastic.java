/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.state.dto;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.annotation.XmlRootElement;

import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateDTO;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.state.ProvTree;
import io.hops.hopsworks.common.provenance.state.ProvSParser;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;

@XmlRootElement
public class ProvStateElastic implements Comparator<ProvStateElastic>, ProvTree.State {
  
  private String id;
  private float score;
  private Map<String, Object> map;
  
  private Long inodeId;
  private String appId;
  private Integer userId;
  private Long projectInodeId;
  private Long datasetInodeId;
  private String inodeName;
  private String projectName;
  private Provenance.MLType mlType;
  private String mlId;
  private Long createTime;
  private String readableCreateTime;
  private Map<String, String> xattrs = new HashMap<>();
  private ProvAppStateDTO appState;
  private String fullPath;
  private Long partitionId;
  private Long parentInodeId;
  
  public static ProvStateElastic instance(BasicElasticHit hit) throws ProvenanceException {
    ProvStateElastic result = new ProvStateElastic();
    result.id = hit.getId();
    result.score = Float.isNaN(hit.getScore()) ? 0 : hit.getScore();
    result.map = hit.getSource();
    return instance(result);
  }
  
  private static ProvStateElastic instance(ProvStateElastic result)
    throws ProvenanceException {
    Map<String, Object> auxMap = new HashMap<>(result.map);
    //Even though we define in the elastic mapping a Long,
    //if the value is small, it seems to be retrieved as an Integer
    //so we need to do the complicated castings lambda
    try {
      result.projectInodeId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.PROJECT_I_ID);
      result.inodeId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.FILE_I_ID);
      result.appId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.APP_ID);
      result.userId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.USER_ID);
      result.inodeName = ProvHelper.extractElasticField(auxMap, ProvSParser.State.FILE_NAME);
      result.createTime = ProvHelper.extractElasticField(auxMap, ProvSParser.State.CREATE_TIMESTAMP);
      result.mlType = ProvHelper.extractElasticField(auxMap, ProvSParser.State.ML_TYPE);
      result.mlId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.ML_ID);
      result.datasetInodeId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.DATASET_I_ID);
      result.parentInodeId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.PARENT_I_ID);
      result.partitionId = ProvHelper.extractElasticField(auxMap, ProvSParser.State.PARTITION_ID);
      result.projectName = ProvHelper.extractElasticField(auxMap, ProvSParser.State.PROJECT_NAME);
      result.readableCreateTime = ProvHelper.extractElasticField(auxMap, ProvSParser.State.R_CREATE_TIMESTAMP);
      ProvHelper.extractElasticField(auxMap, ProvSParser.State.ENTRY_TYPE);
      result.xattrs = ProvHelper.extractElasticField(auxMap,
        ProvParser.XAttrField.XATTR_PROV, ProvHelper.asXAttrMap(), true);
      if (result.xattrs != null && result.xattrs.containsKey(ProvSParser.State.APP_ID.toString())) {
        //update if we have a value - useful if tls is disabled and out appId is set to none
        result.appId = ProvHelper.extractElasticField(result.xattrs.get(ProvParser.BaseField.APP_ID.toString()));
      }
      for (Map.Entry<String, Object> entry : auxMap.entrySet()) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.INFO,
          "field:" + entry.getKey() + "not managed in file state return");
      }
    } catch(ClassCastException e) {
      String msg = "mistmatch between DTO class and ProvSParser field types (elastic)";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING, msg, msg, e);
    }
    return result;
  }

  public float getScore() {
    return score;
  }
  
  @Override
  public int compare(ProvStateElastic o1, ProvStateElastic o2) {
    return Float.compare(o2.getScore(), o1.getScore());
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Map<String, String> getMap() {
    //flatten hits (remove nested json objects) to make it more readable
    Map<String, String> refined = new HashMap<>();

    if (this.map != null) {
      for (Map.Entry<String, Object> entry : this.map.entrySet()) {
        //convert value to string
        String value = (entry.getValue() == null) ? "null" : entry.getValue().toString();
        refined.put(entry.getKey(), value);
      }
    }

    return refined;
  }

  public void setMap(Map<String, Object> map) {
    this.map = map;
  }

  @Override
  public Long getInodeId() {
    return inodeId;
  }

  public void setInodeId(long inodeId) {
    this.inodeId = inodeId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public int getUserId() {
    return userId;
  }

  public void setUserId(int userId) {
    this.userId = userId;
  }

  @Override
  public Long getProjectInodeId() {
    return projectInodeId;
  }

  public void setProjectInodeId(Long projectInodeId) {
    this.projectInodeId = projectInodeId;
  }
  
  public Long getDatasetInodeId() {
    return datasetInodeId;
  }
  
  public void setDatasetInodeId(Long datasetInodeId) {
    this.datasetInodeId = datasetInodeId;
  }
  
  @Override
  public String getInodeName() {
    return inodeName;
  }

  public void setInodeName(String inodeName) {
    this.inodeName = inodeName;
  }

  public Provenance.MLType getMlType() {
    return mlType;
  }

  public void setMlType(Provenance.MLType mlType) {
    this.mlType = mlType;
  }

  public String getMlId() {
    return mlId;
  }

  public void setMlId(String mlId) {
    this.mlId = mlId;
  }

  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  public String getReadableCreateTime() {
    return readableCreateTime;
  }

  public void setReadableCreateTime(String readableCreateTime) {
    this.readableCreateTime = readableCreateTime;
  }

  public Map<String, String> getXattrs() {
    return xattrs;
  }

  public void setXattrs(Map<String, String> xattrs) {
    this.xattrs = xattrs;
  }

  public ProvAppStateDTO getAppState() {
    return appState;
  }

  public void setAppState(ProvAppStateDTO appState) {
    this.appState = appState;
  }
  
  public String getFullPath() {
    return fullPath;
  }
  
  public void setFullPath(String fullPath) {
    this.fullPath = fullPath;
  }
  
  public Long getPartitionId() {
    return partitionId;
  }
  
  public void setPartitionId(Long partitionId) {
    this.partitionId = partitionId;
  }
  
  @Override
  public Long getParentInodeId() {
    return parentInodeId;
  }
  
  public void setParentInodeId(Long parentInodeId) {
    this.parentInodeId = parentInodeId;
  }
  
  public void setInodeId(Long inodeId) {
    this.inodeId = inodeId;
  }
  
  public void setUserId(Integer userId) {
    this.userId = userId;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  @Override
  public boolean isProject() {
    return projectInodeId == inodeId;
  }
}
