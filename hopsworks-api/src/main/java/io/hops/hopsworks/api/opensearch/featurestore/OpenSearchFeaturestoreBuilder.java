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
package io.hops.hopsworks.api.opensearch.featurestore;

import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.opensearch.OpenSearchController;
import io.hops.hopsworks.common.opensearch.FeaturestoreDocType;
import io.hops.hopsworks.common.opensearch.OpenSearchFeaturestoreHit;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Triplet;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.text.Text;
import org.opensearch.search.SearchHit;
import org.opensearch.search.fetch.subphase.highlight.HighlightField;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OpenSearchFeaturestoreBuilder {
  @EJB
  private OpenSearchController openSearchCtrl;
  @Inject
  private DatasetAccessController datasetAccessCtrl;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HopsworksJAXBContext converter;
  @EJB
  private OpenSearchFeaturestoreItemBuilder openSearchFeaturestoreItemBuilder;
  
  private void checkRequest(OpenSearchFeaturestoreRequest req) throws GenericException {
    if(req.getSize() == null || req.getFrom() == null
      || req.getSize() < 0 || 10000 < req.getSize() || req.getFrom() < 0) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT,
        Level.WARNING, "malformed pagination parameters");
    }
  }
  public OpenSearchFeaturestoreDTO build(OpenSearchFeaturestoreRequest req, Integer projectId)
    throws OpenSearchException, ServiceException, GenericException {
    checkRequest(req);
    Project project = projectFacade.find(projectId);
    Map<FeaturestoreDocType, Set<Integer>> searchProjects
      = datasetAccessCtrl.featurestoreSearchContext(project, req.getDocType());
    Map<FeaturestoreDocType, SearchResponse> response
      = openSearchCtrl.featurestoreSearch(req.getTerm(), searchProjects, req.getFrom(), req.getSize());
  
    DatasetAccessController.DatasetAccessCtrl localProjectOnly =
      (datasetDetails, projectsCollector) -> projectsCollector.addAccessProject(project);
    
    OpenSearchFeaturestoreDTO result = parseResult(response, localProjectOnly);
    result.setFeaturegroupsFrom(req.getFrom());
    result.setTrainingdatasetsFrom(req.getFrom());
    result.setFeaturesFrom(req.getFrom());
    return result;
  }
  
  public OpenSearchFeaturestoreDTO build(Users user, OpenSearchFeaturestoreRequest req)
    throws OpenSearchException, ServiceException, GenericException {
    checkRequest(req);
    Map<FeaturestoreDocType, SearchResponse> response
      = openSearchCtrl.featurestoreSearch(req.getDocType(), req.getTerm(), req.getFrom(), req.getSize());
   
    /** we are using the memoized accessor method here since we expect many results to actually belong to the same
     * dataset/project, thus there is  no use in going to database multiple times (stressing it) to get (possibly) the
     * same dataset/project/shared datasets.
     * we get a new accessor with each call and thus cache values for very short periods of time
     * (parsing this particular query result only)
     */
    OpenSearchFeaturestoreDTO result = parseResult(response, datasetAccessCtrl.memoizedAccessorProjects(user));
    result.setFeaturegroupsFrom(req.getFrom());
    result.setTrainingdatasetsFrom(req.getFrom());
    //TODO Alex fix v2 - from size of features
    result.setFeaturesFrom(0);
    return result;
  }
  
  private OpenSearchFeaturestoreDTO parseResult(Map<FeaturestoreDocType, SearchResponse> resp,
                                                DatasetAccessController.DatasetAccessCtrl accessCtrl)
    throws OpenSearchException, GenericException {
    OpenSearchFeaturestoreDTO result = new OpenSearchFeaturestoreDTO();
    for(Map.Entry<FeaturestoreDocType, SearchResponse> e : resp.entrySet()) {
      switch(e.getKey()) {
        case FEATUREGROUP: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            OpenSearchFeaturestoreHit hit = OpenSearchFeaturestoreHit.instance(hitAux);
            OpenSearchFeaturestoreItemDTO.Base item =
              openSearchFeaturestoreItemBuilder.fromFeaturegroup(hit, converter);
            item.setHighlights(getHighlights(hitAux.getHighlightFields()));
            accessCtrl.accept(inputWrapper(hit), collectorWrapper(item));
            result.addFeaturegroup(item);
          }
          result.setFeaturegroupsTotal(e.getValue().getHits().getTotalHits().value);
        } break;
        case FEATUREVIEW: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            OpenSearchFeaturestoreHit hit = OpenSearchFeaturestoreHit.instance(hitAux);
            OpenSearchFeaturestoreItemDTO.Base item
              = openSearchFeaturestoreItemBuilder.fromTrainingDataset(hit, converter);
            item.setHighlights(getHighlights(hitAux.getHighlightFields()));
            accessCtrl.accept(inputWrapper(hit), collectorWrapper(item));
            result.addFeatureView(item);
          }
          result.setFeatureViewsTotal(e.getValue().getHits().getTotalHits().value);
        } break;
        case TRAININGDATASET: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            OpenSearchFeaturestoreHit hit = OpenSearchFeaturestoreHit.instance(hitAux);
            OpenSearchFeaturestoreItemDTO.Base item
              = openSearchFeaturestoreItemBuilder.fromTrainingDataset(hit, converter);
            item.setHighlights(getHighlights(hitAux.getHighlightFields()));
            accessCtrl.accept(inputWrapper(hit), collectorWrapper(item));
            result.addTrainingdataset(item);
          }
          result.setTrainingdatasetsTotal(e.getValue().getHits().getTotalHits().value);
        } break;
        case FEATURE: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            setFeatureNameHighlights(hitAux, result, accessCtrl);
            setFeatureDescriptionHighlights(hitAux, result, accessCtrl);
          }
          //TODO Alex fix v2 - from size of features
          //result.setFeaturesTotal(e.getValue().getHits().getTotalHits().value);
          result.setFeaturesTotal((long)result.getFeatures().size());
        } break;
      }
    }
    return result;
  }
  
  private void setFeatureNameHighlights(SearchHit hitAux, OpenSearchFeaturestoreDTO result,
                                        DatasetAccessController.DatasetAccessCtrl accessCtrl)
    throws OpenSearchException, GenericException {
    OpenSearchFeaturestoreHit hit = OpenSearchFeaturestoreHit.instance(hitAux);
    Map<String, HighlightField> highlightFields = hitAux.getHighlightFields();
    String featureNameField = FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
      FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.NAME);
    
    //<highlighted text, name, description>
    Function<Triplet<String, String, String>, Boolean> matcher = (state) -> {
      //check if highlighted name equals feature name
      return removeHighlightTags(state.getValue0()).equals(state.getValue1());
    };
    BiConsumer<OpenSearchFeaturestoreItemDTO.Highlights, String> highlighter =
      OpenSearchFeaturestoreItemDTO.Highlights::setName;
    setFeatureHighlights(highlightFields.get(featureNameField), hit, matcher, highlighter, result, accessCtrl);
  }
  
  private void setFeatureDescriptionHighlights(SearchHit hitAux, OpenSearchFeaturestoreDTO result,
                                               DatasetAccessController.DatasetAccessCtrl accessCtrl)
    throws OpenSearchException, GenericException {
    OpenSearchFeaturestoreHit hit = OpenSearchFeaturestoreHit.instance(hitAux);
    Map<String, HighlightField> highlightFields = hitAux.getHighlightFields();
    String featureDescriptionField = FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
      FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.DESCRIPTION);
  
    //<highlighted text, name, description>
    Function<Triplet<String, String, String>, Boolean> matcher = (state) -> {
      //check if highlighted description equals feature description
      return removeHighlightTags(state.getValue0()).equals(state.getValue2());
    };
    BiConsumer<OpenSearchFeaturestoreItemDTO.Highlights, String> highlighter =
      OpenSearchFeaturestoreItemDTO.Highlights::setDescription;
    setFeatureHighlights(highlightFields.get(featureDescriptionField),
      hit, matcher, highlighter, result, accessCtrl);
  }
  
  private void setFeatureHighlights(HighlightField hField,
                                   OpenSearchFeaturestoreHit hit,
                                   Function<Triplet<String, String, String>, Boolean> matcher,
                                   BiConsumer<OpenSearchFeaturestoreItemDTO.Highlights, String> highlighter,
                                   OpenSearchFeaturestoreDTO result,
                                   DatasetAccessController.DatasetAccessCtrl accessCtrl)
      throws GenericException {
    if (hField != null) {
      //highlights only return the field that matched the search, we will check the xattr for the omologue for
      // complete info
      @SuppressWarnings("unchecked")
      Map<String, Object> featurestore = (Map)(hit.getXattrs()
        .get(FeaturestoreXAttrsConstants.FEATURESTORE));
      @SuppressWarnings("unchecked")
      ArrayList<Map<String, String>> hit_fg_features = (ArrayList<Map<String, String>>)featurestore
        .get(FeaturestoreXAttrsConstants.FG_FEATURES);
      for (Text ee : hField.fragments()) { // looking through all highligths
        for(Map<String, String> hit_fg_feature : hit_fg_features) { //comparing to each feature (from xattr)
          String featureNameAux = null;
          String featureDescriptionAux = "";
          //features are stored as an array of hashes [{name:<>, description:<>}]
          for(Map.Entry<String, String> hit_fg_feature_e : hit_fg_feature.entrySet()) {
            switch(hit_fg_feature_e.getKey()) {
              case FeaturestoreXAttrsConstants.NAME: featureNameAux = hit_fg_feature_e.getValue(); break;
              case FeaturestoreXAttrsConstants.DESCRIPTION: featureDescriptionAux = hit_fg_feature_e.getValue(); break;
            }
          }
          if(featureNameAux == null) {
            throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.WARNING,
              "opensearch indices might contain malformed entries");
          }
          //does the feature match the highlight (name/description)
          if (matcher.apply(Triplet.with(ee.toString(), featureNameAux, featureDescriptionAux))) {
            OpenSearchFeaturestoreItemDTO.Feature feature = result.getFeature(hit.getProjectName(), featureNameAux);
            if(feature == null) {
              OpenSearchFeaturestoreItemDTO.Base fgParent
                = openSearchFeaturestoreItemBuilder.fromFeaturegroup(hit, converter);
              feature = openSearchFeaturestoreItemBuilder.fromFeature(featureNameAux, featureDescriptionAux, fgParent);
              result.addFeature(feature);
              accessCtrl.accept(inputWrapper(hit), collectorWrapper(feature));
            }
            OpenSearchFeaturestoreItemDTO.Highlights highlights = feature.getHighlights();
            if(highlights == null) {
              highlights = new OpenSearchFeaturestoreItemDTO.Highlights();
              feature.setHighlights(highlights);
            }
            //set the appropriate highlight (name/description)
            highlighter.accept(highlights, ee.toString());
          }
        }
      }
    }
  }
  
  private OpenSearchFeaturestoreItemDTO.Highlights getHighlights(Map<String, HighlightField> map) {
    OpenSearchFeaturestoreItemDTO.Highlights highlights = new OpenSearchFeaturestoreItemDTO.Highlights();
    for(Map.Entry<String, HighlightField> e : map.entrySet()) {
      if(e.getKey().endsWith(".keyword")) {
        continue;
      }
      if(e.getKey().equals(FeaturestoreXAttrsConstants.NAME)
        || e.getKey().equals(
          FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(FeaturestoreXAttrsConstants.NAME))) {
        highlights.setName(e.getValue().fragments()[0].toString());
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(FeaturestoreXAttrsConstants.DESCRIPTION))) {
        highlights.setDescription(e.getValue().fragments()[0].toString());
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
          FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.NAME))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeature(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
          FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.DESCRIPTION))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeatureDescription(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
          FeaturestoreXAttrsConstants.FV_FEATURES, FeaturestoreXAttrsConstants.FG_FEATURES))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeature(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
          FeaturestoreXAttrsConstants.TD_FEATURES, FeaturestoreXAttrsConstants.FG_FEATURES))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeature(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(FeaturestoreXAttrsConstants.getTagsOpenSearchKey())) {
        for(Text t : e.getValue().fragments()) {
          highlights.addTagKey(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(FeaturestoreXAttrsConstants.getTagsOpenSearchValue())) {
        for(Text t : e.getValue().fragments()) {
          highlights.addTagValue(t.toString());
        }
        continue;
      }
      
      if(e.getKey().startsWith(FeaturestoreXAttrsConstants.OPENSEARCH_XATTR + ".")) {
        for(Text t : e.getValue().fragments()) {
          highlights.addOtherXAttr(e.getKey(), t.toString());
        }
      }
    }
    return highlights;
  }
  
  private String removeHighlightTags(String field) {
    field = field.replace("<em>", "");
    field = field.replace("</em>", "");
    return field;
  }
  
  private DatasetAccessController.DatasetDetails inputWrapper(OpenSearchFeaturestoreHit hit) {
    return new DatasetAccessController.DatasetDetails() {
  
      @Override
      public Integer getParentProjectId() {
        return hit.getProjectId();
      }
  
      @Override
      public Long getParentDatasetIId() {
        return hit.getDatasetIId();
      }
      
      @Override
      public String toString() {
        return hit.toString();
      }
    };
  }
  
  private DatasetAccessController.ProjectsCollector collectorWrapper(OpenSearchFeaturestoreItemDTO.Base item) {
    return new DatasetAccessController.ProjectsCollector() {
      @Override
      public void addAccessProject(Project project) {
        item.addAccessProject(project.getId(), project.getName());
      }
    };
  }
}
