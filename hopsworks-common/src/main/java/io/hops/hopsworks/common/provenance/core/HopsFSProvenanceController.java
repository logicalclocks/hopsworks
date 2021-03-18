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

import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
  @EJB
  private XAttrsController xattrCtrl;
  
  /**
   * To be used on projects/datasets - only these have a provenance core xattr
   * @param path
   * @param udfso
   * @return
   */
  private ProvCoreDTO getProvCoreXAttr(String path, DistributedFileSystemOps udfso) throws ProvenanceException {
    byte[] provTypeB;
    try {
      provTypeB = xattrCtrl.getProvXAttr(udfso, path, ProvXAttrs.PROV_XATTR_CORE_VAL);
      if(provTypeB == null) {
        return null;
      }
      return converter.unmarshal(new String(provTypeB), ProvCoreDTO.class);
    } catch (GenericException | DatasetException | MetadataException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - get xattr - prov core - error", "hopsfs - get xattr - prov core - error", e);
    }
  }
  
  private void setProvCoreXAttr(String path, ProvCoreDTO provCore, DistributedFileSystemOps udfso)
    throws ProvenanceException {
    try {
      String provType = converter.marshal(provCore);
      xattrCtrl.upsertProvXAttr(udfso, path, ProvXAttrs.PROV_XATTR_CORE_VAL, provType.getBytes());
    } catch (GenericException | DatasetException | MetadataException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - set xattr - prov core - error", "hopsfs - set xattr - prov core - error", e);
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
  
  public void updateHiveDatasetProvCore(Project project, String hiveDBPath, ProvTypeDTO newProvType,
    DistributedFileSystemOps dfso)
    throws ProvenanceException {
    ProvCoreDTO newProvCore = new ProvCoreDTO(newProvType, project.getInode().getId());
    ProvCoreDTO currentProvCore = getProvCoreXAttr(hiveDBPath, dfso);
    if(currentProvCore != null && currentProvCore.getType().equals(newProvType)) {
      return;
    }
    updateDatasetProvType(hiveDBPath, newProvCore, dfso);
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
        dfs.closeDfsClient(udfso);
      }
    }
  }
  
  public void featuregroupAttachXAttrs(String fgPath, FeaturegroupDTO featuregroup, DistributedFileSystemOps udfso)
    throws ProvenanceException {
    FeaturegroupXAttr.FullDTO fg = fromFeaturegroup(featuregroup);
    try {
      byte[] xattrVal = converter.marshal(fg).getBytes();
      try{
        xattrCtrl.upsertProvXAttr(udfso, fgPath, FeaturestoreXAttrsConstants.FEATURESTORE, xattrVal);
      } catch (MetadataException e) {
        if (RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED.equals(e.getErrorCode())) {
          LOGGER.log(Level.INFO,
            "xattr is too large to attach - featuregroup:{0} will not have features attached", fgPath);
          fg = new FeaturegroupXAttr.FullDTO(featuregroup.getFeaturestoreId(), featuregroup.getDescription(),
            featuregroup.getCreated(), featuregroup.getCreator());
          xattrVal = converter.marshal(fg).getBytes();
          xattrCtrl.upsertProvXAttr(udfso, fgPath, FeaturestoreXAttrsConstants.FEATURESTORE, xattrVal);
        } else {
          throw e;
        }
      }
    } catch (GenericException | MetadataException | DatasetException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
        "hopsfs - set xattr - featuregroup - error", "hopsfs - set xattr - featuregroup - error", e);
    }
  }

  public void trainingDatasetAttachXAttr(String path, TrainingDatasetDTO trainingDatasetDTO,
                                         DistributedFileSystemOps udfso)
      throws ProvenanceException {
    TrainingDatasetXAttrDTO td = new TrainingDatasetXAttrDTO(trainingDatasetDTO.getFeaturestoreId(),
        trainingDatasetDTO.getDescription(),
        trainingDatasetDTO.getCreated(),
        trainingDatasetDTO.getCreator(),
        fromTrainingDataset(trainingDatasetDTO));

    try {
      byte[] xattrVal = converter.marshal(td).getBytes();
      try{
        xattrCtrl.upsertProvXAttr(udfso, path, FeaturestoreXAttrsConstants.FEATURESTORE, xattrVal);
      } catch (MetadataException e) {
        if (RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED.equals(e.getErrorCode())) {
          LOGGER.log(Level.INFO,
              "xattr is too large to attach - trainingdataset:{0} will not have features attached", path);
          td = new TrainingDatasetXAttrDTO(trainingDatasetDTO.getFeaturestoreId(),
              trainingDatasetDTO.getDescription(), trainingDatasetDTO.getCreated(), trainingDatasetDTO.getCreator());
          xattrVal = converter.marshal(td).getBytes();
          xattrCtrl.upsertProvXAttr(udfso, path, FeaturestoreXAttrsConstants.FEATURESTORE, xattrVal);
        } else {
          throw e;
        }
      }
    } catch (GenericException | MetadataException | DatasetException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
          "hopsfs - set xattr - training dataset - error", "hopsfs - set xattr - training dataset - error", e);
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


  private List<FeaturegroupXAttr.SimplifiedDTO> fromTrainingDataset(TrainingDatasetDTO trainingDatasetDTO) {
    if (trainingDatasetDTO.getFromQuery()) {
      // training dataset generated from hsfs query
      return fromTrainingDatasetQuery(trainingDatasetDTO);
    } else {
      // training dataset generated from spark dataframe
      return fromTrainingDatasetDataframe(trainingDatasetDTO);
    }
  }

  private List<FeaturegroupXAttr.SimplifiedDTO> fromTrainingDatasetQuery(TrainingDatasetDTO trainingDatasetDTO) {
    Map<Integer, FeaturegroupXAttr.SimplifiedDTO> featuregroups = new HashMap<>();
    for(TrainingDatasetFeatureDTO feature : trainingDatasetDTO.getFeatures()) {
      FeaturegroupXAttr.SimplifiedDTO featuregroup = featuregroups.get(feature.getFeaturegroup().getId());
      if(featuregroup == null) {
        featuregroup = new FeaturegroupXAttr.SimplifiedDTO(feature.getFeaturegroup().getFeaturestoreId(),
          feature.getFeaturegroup().getName(), feature.getFeaturegroup().getVersion());
        featuregroups.put(feature.getFeaturegroup().getId(), featuregroup);
      }
      featuregroup.addFeature(feature.getName());
    }
    return new ArrayList<>(featuregroups.values());
  }

  private List<FeaturegroupXAttr.SimplifiedDTO> fromTrainingDatasetDataframe(TrainingDatasetDTO trainingDatasetDTO) {
    FeaturegroupXAttr.SimplifiedDTO containerFeatureGroup =
        new FeaturegroupXAttr.SimplifiedDTO(-1, "", -1);
    containerFeatureGroup.addFeatures(trainingDatasetDTO.getFeatures().stream()
        .map(TrainingDatasetFeatureDTO::getName).collect(Collectors.toList()));

    return Arrays.asList(containerFeatureGroup);
  }

  private FeaturegroupXAttr.FullDTO fromFeaturegroup(FeaturegroupDTO featuregroup) {
    List<String> features = new LinkedList<>();
    for(FeatureGroupFeatureDTO feature : featuregroup.getFeatures()) {
      features.add(feature.getName());
    }
    return new FeaturegroupXAttr.FullDTO(featuregroup.getFeaturestoreId(), featuregroup.getDescription(),
      featuregroup.getCreated(), featuregroup.getCreator(), features);
  }
}
