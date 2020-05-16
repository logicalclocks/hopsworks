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

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless(name = "HopsFSProvenanceController")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HopsFSProvenanceController {
  private static final Logger LOGGER = Logger.getLogger(HopsFSProvenanceController.class.getName());
  
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
      for(DatasetSharedWith dataset : project.getDatasetSharedWithCollection()) {
        String datasetPath = Utils.getFileSystemDatasetPath(dataset.getDataset(), settings);
        ProvCoreDTO provCore = getProvCoreXAttr(datasetPath, udfso);
        if(provCore == null) {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
            "malformed dataset - provenance", "no provenance core xattr");
        }
        ProvDatasetDTO dsState = new ProvDatasetDTO(
          dataset.getDataset().getProject().getName() + "::" + dataset.getDataset().getName(),
          dataset.getDataset().getInode().getId(), provCore.getType());
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
      String path = Utils.getFeaturestorePath(project, settings)
        + "/" + Utils.getFeaturegroupName(featuregroup.getName(), featuregroup.getVersion());
      FeaturegroupXAttr.FullDTO fg = fromFeaturegroup(featuregroup);
      try {
        byte[] xattrVal = converter.marshal(fg).getBytes();
        if(xattrVal.length > 13500) {
          LOGGER.log(Level.INFO,
            "xattr is too large to attach - featuregroup:{0} will not have features attached", path);
          fg = new FeaturegroupXAttr.FullDTO(featuregroup.getFeaturestoreId(), featuregroup.getDescription(),
            featuregroup.getCreated(), featuregroup.getCreator());
          xattrVal = converter.marshal(fg).getBytes();
        }
        udfso.upsertXAttr(path, FeaturestoreXAttrsConstants.getFeaturestoreXAttrKey(), xattrVal);
      } catch (IOException | GenericException e) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
          "hopsfs - set xattr - featuregroup - error", "hopsfs - set xattr - featuregroup - error", e);
      }
    } finally {
      if(udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void trainingDatasetAttachXAttr(Users user, Project project, String path,
    TrainingDatasetDTO trainingDatasetDTO)
    throws ProvenanceException {
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsername);
    try {
      TrainingDatasetXAttrDTO td = new TrainingDatasetXAttrDTO(trainingDatasetDTO.getFeaturestoreId(),
        trainingDatasetDTO.getDescription(), trainingDatasetDTO.getCreated(), trainingDatasetDTO.getCreator());
      if(trainingDatasetDTO.getFeatures() != null) {
        List<FeaturegroupXAttr.SimplifiedDTO> featuresDTO = fromTrainingDataset(trainingDatasetDTO);
        td.setFeatures(featuresDTO);
      }
      try {
        byte[] xattrVal = converter.marshal(td).getBytes();
        if(xattrVal.length > 13500) {
          LOGGER.log(Level.INFO,
            "xattr is too large to attach - trainingdataset:{0} will not have features attached", path);
          td = new TrainingDatasetXAttrDTO(trainingDatasetDTO.getFeaturestoreId(),
            trainingDatasetDTO.getDescription(), trainingDatasetDTO.getCreated(), trainingDatasetDTO.getCreator());
          xattrVal = converter.marshal(td).getBytes();
        }
        udfso.upsertXAttr(path, FeaturestoreXAttrsConstants.getFeaturestoreXAttrKey(), xattrVal);
      } catch (IOException | GenericException e) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
          "hopsfs - set xattr - training dataset - error", "hopsfs - set xattr - training dataset - error", e);
      }
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
  
  //TODO - featurestore without knowing the featurestoreId I can't split them
  private List<FeaturegroupXAttr.SimplifiedDTO> fromTrainingDataset(TrainingDatasetDTO trainingDatasetDTO) {
    List<FeaturegroupXAttr.SimplifiedDTO> result = new LinkedList<>();
    Map<String, FeaturegroupXAttr.SimplifiedDTO> featuregroups = new HashMap<>();
    for(FeatureDTO feature : trainingDatasetDTO.getFeatures()) {
      FeaturegroupXAttr.SimplifiedDTO featuregroup = featuregroups.get(feature.getFeaturegroup());
      if(featuregroup == null) {
        featuregroup = new FeaturegroupXAttr.SimplifiedDTO(trainingDatasetDTO.getFeaturestoreId(),
          feature.getFeaturegroup(), feature.getVersion());
        featuregroups.put(feature.getFeaturegroup(), featuregroup);
        result.add(featuregroup);
      }
      featuregroup.addFeature(feature.getName());
    }
    return result;
  }
  
  private FeaturegroupXAttr.FullDTO fromFeaturegroup(FeaturegroupDTO featuregroup) {
    List<String> features = new LinkedList<>();
    for(FeatureDTO feature : featuregroup.getFeatures()) {
      features.add(feature.getName());
    }
    return new FeaturegroupXAttr.FullDTO(featuregroup.getFeaturestoreId(), featuregroup.getDescription(),
      featuregroup.getCreated(), featuregroup.getCreator(), features);
  }
}
