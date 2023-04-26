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

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeatureViewXAttrDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.provenance.core.dto.ProvCoreDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvDatasetDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

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
  @EJB
  private InodeController inodeController;
  
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
    Inode projectInode = inodeController.getProjectRoot(project.getName());
    
    ProvCoreDTO provCore = getProvCoreXAttr(projectPath, dfso);
    if (provCore != null && newProvType.equals(provCore.getType())) {
      return;
    }
    provCore = new ProvCoreDTO(newProvType, null);
    setProvCoreXAttr(projectPath, provCore, dfso);
  
    provCore = new ProvCoreDTO(newProvType, projectInode.getId());
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
    Inode projectInode = inodeController.getProjectRoot(dataset.getProject().getName());
    ProvCoreDTO newProvCore = new ProvCoreDTO(newProvType, projectInode.getId());
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
    Inode projectInode = inodeController.getProjectRoot(project.getName());
    ProvCoreDTO newProvCore = new ProvCoreDTO(newProvType, projectInode.getId());
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
      Inode projectInode = inodeController.getProjectRoot(project.getName());
      for (Dataset dataset : project.getDatasetCollection()) {
        Path datasetPath = Utils.getDatasetPath(dataset, settings);
        ProvCoreDTO provCore = getProvCoreXAttr(datasetPath.toString(), udfso);
        if(provCore == null) {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
            "malformed dataset - provenance", "no provenance core xattr");
        }
        Inode datasetInode = inodeController.getProjectDatasetInode(projectInode, datasetPath.toString(), dataset);
        ProvDatasetDTO dsState = new ProvDatasetDTO(dataset.getName(), datasetInode.getId(), provCore.getType());
        result.add(dsState);
      }
      for(DatasetSharedWith dataset : project.getDatasetSharedWithCollection()) {
        Path datasetPath = Utils.getDatasetPath(dataset.getDataset(), settings);
        Inode datasetInode = inodeController.getInodeAtPath(datasetPath.toString());
        ProvCoreDTO provCore = getProvCoreXAttr(datasetPath.toString(), udfso);
        if(provCore == null) {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.WARNING,
            "malformed dataset - provenance", "no provenance core xattr");
        }
        ProvDatasetDTO dsState = new ProvDatasetDTO(
          dataset.getDataset().getProject().getName() + "::" + dataset.getDataset().getName(),
          datasetInode.getId(), provCore.getType());
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
            featuregroup.getCreated(), featuregroup.getCreator().getEmail());
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
        trainingDatasetDTO.getCreator().getEmail(),
        fromTrainingDataset(trainingDatasetDTO));

    try {
      byte[] xattrVal = converter.marshal(td).getBytes();
      try{
        xattrCtrl.upsertProvXAttr(udfso, path, FeaturestoreXAttrsConstants.FEATURESTORE, xattrVal);
      } catch (MetadataException e) {
        if (RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED.equals(e.getErrorCode())) {
          LOGGER.log(Level.INFO,
              "xattr is too large to attach - trainingdataset:{0} will not have features attached", path);
          td = new TrainingDatasetXAttrDTO(trainingDatasetDTO.getFeaturestoreId(), trainingDatasetDTO.getDescription(),
                  trainingDatasetDTO.getCreated(), trainingDatasetDTO.getCreator().getEmail());
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

  public void featureViewAttachXAttr(String path, FeatureView featureView, DistributedFileSystemOps udfso)
      throws ProvenanceException {
    FeatureViewXAttrDTO fv = new FeatureViewXAttrDTO(featureView.getFeaturestore().getId(),
        featureView.getDescription(),
        featureView.getCreated(),
        featureView.getCreator().getEmail(),
        fromFeatureViewQuery(featureView));

    try {
      byte[] xattrVal = converter.marshal(fv).getBytes();
      try{
        xattrCtrl.upsertProvXAttr(udfso, path, FeaturestoreXAttrsConstants.FEATURESTORE, xattrVal);
      } catch (MetadataException e) {
        if (RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED.equals(e.getErrorCode())) {
          LOGGER.log(Level.INFO,
              "xattr is too large to attach - feature view:{0} will not have features attached", path);
          fv = new FeatureViewXAttrDTO(featureView.getFeaturestore().getId(), featureView.getDescription(),
              featureView.getCreated(), featureView.getCreator().getEmail());
          xattrVal = converter.marshal(fv).getBytes();
          xattrCtrl.upsertProvXAttr(udfso, path, FeaturestoreXAttrsConstants.FEATURESTORE, xattrVal);
        } else {
          throw e;
        }
      }
    } catch (GenericException | MetadataException | DatasetException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.FS_ERROR, Level.WARNING,
          "hopsfs - set xattr - feature view - error", "hopsfs - set xattr - feature view - error", e);
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

  private List<FeaturegroupXAttr.SimplifiedDTO> fromFeatureViewQuery(FeatureView featureView) {
    Map<Integer, FeaturegroupXAttr.SimplifiedDTO> featuregroups = new HashMap<>();
    for(TrainingDatasetFeature feature : featureView.getFeatures()) {
      FeaturegroupXAttr.SimplifiedDTO featuregroup = featuregroups.get(feature.getFeatureGroup().getId());
      if(featuregroup == null) {
        featuregroup = new FeaturegroupXAttr.SimplifiedDTO(feature.getFeatureGroup().getFeaturestore().getId(),
            feature.getFeatureGroup().getName(), feature.getFeatureGroup().getVersion());
        featuregroups.put(feature.getFeatureGroup().getId(), featuregroup);
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
    List<FeaturegroupXAttr.SimpleFeatureDTO> features = new LinkedList<>();
    for(FeatureGroupFeatureDTO feature : featuregroup.getFeatures()) {
      features.add(new FeaturegroupXAttr.SimpleFeatureDTO(feature.getName(), feature.getDescription()));
    }
    FeaturegroupXAttr.FullDTO fullDTO = new FeaturegroupXAttr.FullDTO(featuregroup.getFeaturestoreId(),
            featuregroup.getDescription(), featuregroup.getCreated(), featuregroup.getCreator().getEmail(), features);
    if(featuregroup instanceof CachedFeaturegroupDTO) {
      fullDTO.setFgType(FeaturegroupXAttr.FGType.CACHED);
    } else if (featuregroup instanceof StreamFeatureGroupDTO) {
      fullDTO.setFgType(FeaturegroupXAttr.FGType.STREAM);
    } else if (featuregroup instanceof OnDemandFeaturegroupDTO) {
      fullDTO.setFgType(FeaturegroupXAttr.FGType.ON_DEMAND);
    }
    return fullDTO;
  }
}
