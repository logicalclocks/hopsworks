/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.commands.featurestore.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.commands.CommandException;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeatureViewXAttrDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommand;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.update.UpdateRequest;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.DeleteByQueryRequest;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SearchFSOpenSearchController {
  @EJB
  private OpenSearchClientController opensearchClient;
  @Inject
  private FeatureStoreKeywordControllerIface keywordCtrl;
  @Inject
  private FeatureStoreTagControllerIface tagCtrl;
  @EJB
  private FeaturegroupController featureGroupCtrl;
  @EJB
  private TrainingDatasetController trainingDatasetCtrl;
  @EJB
  private InodeController inodeCtrl;
  @EJB
  private Settings settings;
  
  public long deleteProject(Project project) throws OpenSearchException {
    DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest(Settings.FEATURESTORE_INDEX);
    deleteRequest.setQuery(QueryBuilders.matchQuery(
      FeaturestoreXAttrsConstants.PROJECT_ID, project.getId()));
    return opensearchClient.deleteByQuery(deleteRequest);
  }
  
  public boolean delete(Long docId) throws OpenSearchException {
    DeleteRequest request = new DeleteRequest().index(Settings.FEATURESTORE_INDEX).id(String.valueOf(docId));
    return opensearchClient.deleteDoc(request);
  }
  
  public void create(Long docId, SearchFSCommand c) throws CommandException, OpenSearchException {
    IndexRequest request = new IndexRequest().index(Settings.FEATURESTORE_INDEX).id(String.valueOf(docId));
    request.source(docBuilder(create(c)));
    opensearchClient.indexDoc(request);
  }
  
  public void updateTags(Long docId, SearchFSCommand c) throws CommandException, OpenSearchException {
    UpdateRequest request = new UpdateRequest().index(Settings.FEATURESTORE_INDEX).id(String.valueOf(docId));
    request.doc(docBuilder(updateTags(c)));
    opensearchClient.updateDoc(request);
  }
  
  public void updateKeywords(Long docId, SearchFSCommand c) throws CommandException, OpenSearchException {
    UpdateRequest request = new UpdateRequest().index(Settings.FEATURESTORE_INDEX)
      .id(String.valueOf(docId));
    request.doc(docBuilder(updateKeywords(c)));
    opensearchClient.updateDoc(request);
  }
  
  public void updateMetadata(Long docId, SearchFSCommand c) throws CommandException, OpenSearchException {
    UpdateRequest request = new UpdateRequest().index(Settings.FEATURESTORE_INDEX)
      .id(String.valueOf(docId));
    request.doc(docBuilder(updateMetadata(c)));
    opensearchClient.updateDoc(request);
  }
  
  private SearchDoc updateMetadata(SearchFSCommand c) throws CommandException {
    SearchDoc doc =  new SearchDoc();
    SearchDoc.XAttr xattr = new SearchDoc.XAttr();
    doc.setXattr(xattr);
  
    try {
      if(c.getFeatureGroup() != null) {
        FeaturegroupDTO fgDTO = featureGroupCtrl.convertFeaturegrouptoDTO(c.getFeatureGroup(),
          c.getProject(), c.getFeatureGroup().getCreator());
        xattr.setFeaturestore(getFGXAttr(fgDTO));
      } else if(c.getFeatureView() != null) {
        xattr.setFeaturestore(getFVXAttr(c.getFeatureView()));
      } else if(c.getTrainingDataset() != null) {
        TrainingDatasetDTO tdDTO = trainingDatasetCtrl.convertTrainingDatasetToDTO(c.getTrainingDataset().getCreator(),
          c.getProject(), c.getTrainingDataset());
        xattr.setFeaturestore(getTDXAttr(tdDTO));
      } else{
        throw CommandException.unhandledArtifactType();
      }
      return doc;
    } catch (FeaturestoreException | ServiceException | CloudException e) {
      String errMsg = "error accessing featurestore";
      throw new CommandException(RESTCodes.CommandErrorCode.FEATURESTORE_ACCESS_ERROR, Level.WARNING,
        errMsg, errMsg, e);
    }
  }
  
  private SearchDoc create(SearchFSCommand c) throws CommandException {
    SearchDoc doc =  new SearchDoc();
    doc.setProjectId(c.getProject().getId());
    doc.setProjectName(c.getProject().getName());
  
    String featureStorePath = Utils.getFeaturestorePath(c.getProject(), settings);
    Long featureStoreInode = inodeCtrl.getInodeAtPath(featureStorePath).getId();
    doc.setDatasetIId(featureStoreInode);
    if(c.getFeatureGroup() != null) {
      doc.setDocType(OpenSearchDocType.FEATURE_GROUP);
      doc.setName(c.getFeatureGroup().getName());
      doc.setVersion(c.getFeatureGroup().getVersion());
    } else if(c.getFeatureView() != null) {
      doc.setDocType(OpenSearchDocType.FEATURE_VIEW);
      doc.setName(c.getFeatureView().getName());
      doc.setVersion(c.getFeatureView().getVersion());
    } else if(c.getTrainingDataset() != null) {
      doc.setDocType(OpenSearchDocType.TRAINING_DATASET);
      doc.setName(c.getTrainingDataset().getName());
      doc.setVersion(c.getTrainingDataset().getVersion());
    } else{
      throw CommandException.unhandledArtifactType();
    }
    return doc;
  }
  
  private SearchDoc updateTags(SearchFSCommand c) throws CommandException {
    SearchDoc doc =  new SearchDoc();
    Map<String, FeatureStoreTag> tags;
    if(c.getFeatureGroup() != null) {
      tags = tagCtrl.getTags(c.getFeatureGroup());
    } else if(c.getFeatureView() != null) {
      tags = tagCtrl.getTags(c.getFeatureView());
    } else if(c.getTrainingDataset() != null) {
      tags = tagCtrl.getTags(c.getTrainingDataset());
    } else {
      throw new CommandException(RESTCodes.CommandErrorCode.ARTIFACT_DELETED, Level.WARNING,
        "artifact targeted by command was deleted");
    }
    SearchDoc.XAttr xattr = new SearchDoc.XAttr();
    doc.setXattr(xattr);
    List<SearchDoc.Tag> docTags = new ArrayList<>();
    for(Map.Entry<String, FeatureStoreTag> e : tags.entrySet()) {
      docTags.add(new SearchDoc.Tag(e.getKey(), e.getValue().getValue()));
    }
    xattr.setTags(docTags);
    return doc;
  }
  
  private SearchDoc updateKeywords(SearchFSCommand c) throws CommandException {
    SearchDoc doc =  new SearchDoc();
    List<String> keywords;
    if (c.getFeatureGroup() != null) {
      keywords = keywordCtrl.getKeywords(c.getFeatureGroup());
    } else if (c.getFeatureView() != null) {
      keywords = keywordCtrl.getKeywords(c.getFeatureView());
    } else if (c.getTrainingDataset() != null) {
      keywords = keywordCtrl.getKeywords(c.getTrainingDataset());
    } else {
      throw CommandException.unhandledArtifactType();
    }
    SearchDoc.XAttr xattr = new SearchDoc.XAttr();
    doc.setXattr(xattr);
    xattr.setKeywords(keywords);
    return doc;
  }
  
  private FeaturegroupXAttr.FullDTO getFGXAttr(FeaturegroupDTO dto) {
    List<FeaturegroupXAttr.SimpleFeatureDTO> features = new LinkedList<>();
    FeaturegroupXAttr.FullDTO fgXAttr = new FeaturegroupXAttr.FullDTO(dto.getFeaturestoreId(),
      dto.getDescription(), dto.getCreated(), dto.getCreator().getEmail(), features);
    
    for(FeatureGroupFeatureDTO feature : dto.getFeatures()) {
      features.add(new FeaturegroupXAttr.SimpleFeatureDTO(feature.getName(), feature.getDescription()));
    }
    if(dto instanceof CachedFeaturegroupDTO) {
      fgXAttr.setFgType(FeaturegroupXAttr.FGType.CACHED);
    } else if (dto instanceof StreamFeatureGroupDTO) {
      fgXAttr.setFgType(FeaturegroupXAttr.FGType.STREAM);
    } else if (dto instanceof OnDemandFeaturegroupDTO) {
      fgXAttr.setFgType(FeaturegroupXAttr.FGType.ON_DEMAND);
    }
    return fgXAttr;
  }
  
  
  private FeatureViewXAttrDTO getFVXAttr(FeatureView featureView) {
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
    return new FeatureViewXAttrDTO(featureView.getFeaturestore().getId(),
      featureView.getDescription(),
      featureView.getCreated(),
      featureView.getCreator().getEmail(),
      new ArrayList<>(featuregroups.values()));
  }
  
  private TrainingDatasetXAttrDTO getTDXAttr(TrainingDatasetDTO trainingDatasetDTO) {
    List<FeaturegroupXAttr.SimplifiedDTO> features;
    if (trainingDatasetDTO.getFromQuery()) {
      // training dataset generated from hsfs query
      features = fromTrainingDatasetQuery(trainingDatasetDTO);
    } else {
      // training dataset generated from spark dataframe
      features = fromTrainingDatasetDataframe(trainingDatasetDTO);
    }
    return new TrainingDatasetXAttrDTO(trainingDatasetDTO.getFeaturestoreId(),
      trainingDatasetDTO.getDescription(),
      trainingDatasetDTO.getCreated(),
      trainingDatasetDTO.getCreator().getEmail(),
      features);
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
  
  private XContentBuilder docBuilder(SearchDoc doc) throws CommandException {
    XContentBuilder builder;
    try {
      builder = XContentFactory.jsonBuilder();
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.writeValue(builder.getOutputStream(), doc);
    } catch (IOException e) {
      String errMsg = "command failed due to search document parsing issues";
      throw new CommandException(RESTCodes.CommandErrorCode.SERIALIZATION_ERROR, Level.WARNING,
        errMsg, errMsg, e);
    }
    return builder;
  }
}
