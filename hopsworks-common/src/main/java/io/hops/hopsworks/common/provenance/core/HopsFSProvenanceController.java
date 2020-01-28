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
package io.hops.hopsworks.common.provenance.core;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.core.dto.ProvFeatureDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvDatasetDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvCoreDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

@Stateless(name = "HopsFSProvenanceController")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HopsFSProvenanceController {
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private Settings settings;
  @EJB
  private HopsworksJAXBContext converter;
  
  /**
   * To be used on projects/datasets - only these have a provenance core xattr
   * @param path
   * @param udfso
   * @return
   */
  private ProvCoreDTO getProvCoreXAttr(String path, DistributedFileSystemOps udfso) throws ProvenanceException {
    byte[] provTypeB;
    try {
      provTypeB = udfso.getXAttr(path, ProvXAttrs.PROV_XATTR_CORE);
      if(provTypeB == null) {
        return null;
      }
      return converter.unmarshal(new String(provTypeB), ProvCoreDTO.class);
    } catch (IOException | GenericException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - get xattr - prov core - error", "hopsfs - get xattr - prov core - error", e);
    }
  }
  
  private void setProvCoreXAttr(String path, ProvCoreDTO provCore, DistributedFileSystemOps udfso)
    throws ProvenanceException {
    try {
      String provType = converter.marshal(provCore);
      udfso.upsertXAttr(path, ProvXAttrs.PROV_XATTR_CORE, provType.getBytes());
    } catch (IOException | GenericException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - set xattr - prov core - error", "hopsfs - set xattr - prov core - error", e);
    }
  }
  
  private void setFeaturesXAttr(String path, List<FeatureDTO> features, DistributedFileSystemOps udfso)
    throws ProvenanceException {
    if(features == null) {
      return;
    }
    List<ProvFeatureDTO> featuresDTO = fromFeatures(features);
    try {
      udfso.upsertXAttr(path, ProvXAttrs.PROV_XATTR_FEATURES, converter.marshal(featuresDTO).getBytes());
    } catch (IOException | GenericException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - set xattr - prov features - error", "hopsfs - set xattr - prov features - error", e);
    }
  }
  
  private void setFeaturesXAttr(String path, FeaturegroupDTO featuregroup, DistributedFileSystemOps udfso)
    throws ProvenanceException {
    List<ProvFeatureDTO> featuresDTO = fromFeatures(featuregroup);
    try {
      udfso.upsertXAttr(path, ProvXAttrs.PROV_XATTR_FEATURES, converter.marshal(featuresDTO).getBytes());
    } catch (IOException | GenericException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - set xattr - prov features - error", "hopsfs - set xattr - prov features - error", e);
    }
  }
  
  public ProvCoreDTO getDatasetProvCore(Users user, Dataset dataset)
    throws ProvenanceException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(dataset.getProject(), user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    String datasetPath = Utils.getFileSystemDatasetPath(dataset, settings);
    try {
      return getProvCoreXAttr(datasetPath, udfso);
    } finally {
      if(udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public ProvTypeDTO getProjectProvType(Users user, Project project) throws ProvenanceException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    String projectPath = Utils.getProjectPath(project.getName());
    try {
      ProvCoreDTO provCore = getProvCoreXAttr(projectPath, udfso);
      return provCore == null ? null : provCore.getType();
    } finally {
      if(udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void updateProjectProvType(Users user, Project project, ProvTypeDTO provType) throws ProvenanceException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    try {
      updateProjectProvType(project, provType, udfso);
    } finally {
      if(udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void updateProjectProvType(Project project, ProvTypeDTO newProvType, DistributedFileSystemOps dfso)
    throws ProvenanceException {
    String projectPath = Utils.getProjectPath(project.getName());
    
    ProvCoreDTO provCore = getProvCoreXAttr(projectPath, dfso);
    if (provCore != null && newProvType.equals(provCore.getType())) {
      return;
    }
    provCore = new ProvCoreDTO(newProvType, null);
    setProvCoreXAttr(projectPath, provCore, dfso);
  
    provCore = new ProvCoreDTO(newProvType, project.getInode().getId());
    for (Dataset dataset : project.getDatasetCollection()) {
      String datasetPath = Utils.getFileSystemDatasetPath(dataset, settings);
      ProvCoreDTO datasetProvCore = getProvCoreXAttr(datasetPath, dfso);
      if(datasetProvCore != null
        && (datasetProvCore.getType().equals(Provenance.Type.DISABLED.dto)
          || datasetProvCore.getType().equals(newProvType))) {
        continue;
      }
      updateDatasetProvType(datasetPath, provCore, dfso);
    }
  }
  
  public void updateDatasetProvType(Dataset dataset, ProvTypeDTO newProvType, DistributedFileSystemOps dfso)
    throws ProvenanceException {
    ProvCoreDTO newProvCore = new ProvCoreDTO(newProvType, dataset.getProject().getInode().getId());
    String datasetPath = Utils.getFileSystemDatasetPath(dataset, settings);
    ProvCoreDTO currentProvCore = getProvCoreXAttr(datasetPath, dfso);
    if(currentProvCore != null && currentProvCore.getType().equals(newProvType)) {
      return;
    }
    updateDatasetProvType(datasetPath, newProvCore, dfso);
  }
  
  public void newHiveDatasetProvCore(Project project, String hiveDBPath, DistributedFileSystemOps dfso)
    throws ProvenanceException {
    String projectPath = Utils.getProjectPath(project.getName());
    ProvCoreDTO projectProvCore = getProvCoreXAttr(projectPath, dfso);
    if(projectProvCore == null) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
        "hopsfs - hive db - set meta status error - project without prov core");
    }
    ProvCoreDTO datasetProvCore = new ProvCoreDTO(projectProvCore.getType(), project.getInode().getId());
    updateDatasetProvType(hiveDBPath, datasetProvCore, dfso);
  }
  
  private void updateDatasetProvType(String datasetPath, ProvCoreDTO provCore, DistributedFileSystemOps dfso)
    throws ProvenanceException {
    try {
      dfso.setMetaStatus(datasetPath, provCore.getType().getMetaStatus());
    } catch (IOException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - dataset set meta status error", "hopsfs - dataset set meta status error", e);
    }
    setProvCoreXAttr(datasetPath, provCore, dfso);
  }
  
  public List<ProvDatasetDTO> getDatasetsProvType(Users user, Project project) throws ProvenanceException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    
    try {
      List<ProvDatasetDTO> result = new ArrayList<>();
      for (Dataset dataset : project.getDatasetCollection()) {
        String datasetPath = Utils.getFileSystemDatasetPath(dataset, settings);
        ProvCoreDTO provCore = getProvCoreXAttr(datasetPath, udfso);
        if(provCore == null) {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
            "malformed dataset - provenance", "no provenance core xattr");
        }
        ProvDatasetDTO dsState = new ProvDatasetDTO(dataset.getName(), dataset.getInode().getId(), provCore.getType());
        result.add(dsState);
      }
      return result;
    } finally {
      if (udfso != null) {
        udfso.close();
      }
    }
  }
  
  public void featuregroupAttachXAttrs(Users user, Project project, FeaturegroupDTO featuregroup)
    throws ProvenanceException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    
    try {
      String featuregroupPath = Utils.getFeaturestorePath(project, settings)
        + "/" + Utils.getFeaturegroupName(featuregroup.getName(), featuregroup.getVersion());
      setFeaturesXAttr(featuregroupPath, featuregroup, udfso);
    } finally {
      if(udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void trainingDatasetAttachXAttr(Users user, Project project, String path, List<FeatureDTO> features)
    throws ProvenanceException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    try {
      setFeaturesXAttr(path, features, udfso);
    } finally {
      if(udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public ProvTypeDTO getMetaStatus(Users user, Project project, Boolean searchable) throws ProvenanceException {
    if(searchable != null && searchable) {
      ProvTypeDTO projectMetaStatus = getProjectProvType(user, project);
      if(Inode.MetaStatus.DISABLED.equals(projectMetaStatus.getMetaStatus())) {
        return Provenance.Type.META.dto;
      } else {
        return projectMetaStatus;
      }
    } else {
      return Provenance.Type.DISABLED.dto;
    }
  }
  
  private List<ProvFeatureDTO> fromFeatures(List<FeatureDTO> features) {
    List<ProvFeatureDTO> result = new LinkedList<>();
    for(FeatureDTO feature : features) {
      result.add(new ProvFeatureDTO(feature.getFeaturegroup(), feature.getName(), feature.getVersion()));
    }
    return result;
  }
  
  private List<ProvFeatureDTO> fromFeatures(FeaturegroupDTO featuregroup) {
    List<ProvFeatureDTO> result = new LinkedList<>();
    for(FeatureDTO feature : featuregroup.getFeatures()) {
      result.add(new ProvFeatureDTO(featuregroup.getName(), feature.getName(), featuregroup.getVersion()));
    }
    return result;
  }
}
