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
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeatureViewXAttrDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.persistence.entity.commands.search.SearchFSCommand;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SearchFSOpenSearchController {
  @EJB
  private Settings settings;
  @EJB
  private OpenSearchClientController opensearchClient;
  @Inject
  private FeatureStoreKeywordControllerIface keywordCtrl;
  @Inject
  private FeatureStoreTagControllerIface tagCtrl;
  @EJB
  private FeaturegroupController featureGroupCtrl;
  @EJB
  private FeatureViewController featureViewCtrl;
  @EJB
  private TrainingDatasetController trainingDatasetCtrl;
  @EJB
  private XAttrsController xattrCtrl;
  
  @EJB
  private HopsworksJAXBContext converter;
  @EJB
  private InodeController inodeCtrl;
  
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
  
  public void setFeaturestore(Long docId, SearchFSCommand c) throws CommandException, OpenSearchException {
    UpdateRequest request = new UpdateRequest().index(Settings.FEATURESTORE_INDEX).id(String.valueOf(docId));
    request.doc(docBuilder(setFeaturestoreXAttr(c)));
    opensearchClient.updateDoc(request);
  }
  private SearchDoc create(SearchFSCommand c) {
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
  
  private SearchDoc setFeaturestoreXAttr(SearchFSCommand c) throws CommandException {
    SearchDoc doc =  new SearchDoc();
    SearchDoc.XAttr xattr = new SearchDoc.XAttr();
    doc.setXattr(xattr);
    
    try {
      String path;
      String featurestoreStr;
      if (c.getFeatureGroup() != null) {
        path = featureGroupCtrl.getFeatureGroupLocation(c.getFeatureGroup());
        featurestoreStr = xattrCtrl.getXAttrAsSuperUser(path, XAttrsController.XATTR_PROV_NAMESPACE,
          FeaturestoreXAttrsConstants.FEATURESTORE);
        xattr.setFeaturestore(converter.unmarshal(featurestoreStr, FeaturegroupXAttr.FullDTO.class));
      } else if (c.getFeatureView() != null) {
        path = featureViewCtrl.getLocation(c.getFeatureView());
        featurestoreStr = xattrCtrl.getXAttrAsSuperUser(path, XAttrsController.XATTR_PROV_NAMESPACE,
          FeaturestoreXAttrsConstants.FEATURESTORE);
        xattr.setFeaturestore(converter.unmarshal(featurestoreStr, FeatureViewXAttrDTO.class));
      } else if (c.getTrainingDataset() != null) {
        FeaturestoreHopsfsConnector fsConnector =
          trainingDatasetCtrl.getHopsFsConnector(c.getTrainingDataset()).getHopsfsConnector();
        String datasetPath = Utils.getDatasetPath(fsConnector.getHopsfsDataset(), settings).toString();
        path = trainingDatasetCtrl.getTrainingDatasetPath(datasetPath, c.getTrainingDataset().getName(),
          c.getTrainingDataset().getVersion());
        featurestoreStr = xattrCtrl.getXAttrAsSuperUser(path, XAttrsController.XATTR_PROV_NAMESPACE,
          FeaturestoreXAttrsConstants.FEATURESTORE);
        xattr.setFeaturestore(converter.unmarshal(featurestoreStr, TrainingDatasetXAttrDTO.class));
      } else {
        throw CommandException.unhandledArtifactType();
      }
    } catch (DatasetException | MetadataException e) {
      String errMsg = "error accessing featurestore xattr";
      throw new CommandException(RESTCodes.CommandErrorCode.FILESYSTEM_ACCESS_ERROR, Level.WARNING, errMsg, errMsg, e);
    } catch (FeaturestoreException e) {
      String errMsg = "error accessing featurestore";
      throw new CommandException(RESTCodes.CommandErrorCode.FEATURESTORE_ACCESS_ERROR, Level.WARNING,
        errMsg, errMsg, e);
    } catch (GenericException e) {
      String errMsg = "error accessing featurestore";
      throw new CommandException(RESTCodes.CommandErrorCode.SERIALIZATION_ERROR, Level.WARNING,
        errMsg, errMsg, e);
    }
    return doc;
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
