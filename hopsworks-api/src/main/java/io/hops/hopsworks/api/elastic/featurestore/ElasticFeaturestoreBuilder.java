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
package io.hops.hopsworks.api.elastic.featurestore;

import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.elastic.FeaturestoreDocType;
import io.hops.hopsworks.common.elastic.ElasticFeaturestoreHit;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.util.HopsworksJAXBContext;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticFeaturestoreBuilder {
  @EJB
  private ElasticController elasticCtrl;
  @Inject
  private DatasetAccessController datasetAccessCtrl;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HopsworksJAXBContext converter;
  
  private void checkRequest(ElasticFeaturestoreRequest req) throws GenericException {
    if(req.getSize() == null || req.getFrom() == null
      || req.getSize() < 0 || 10000 < req.getSize() || req.getFrom() < 0) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT,
        Level.WARNING, "malformed pagination parameters");
    }
  }
  public ElasticFeaturestoreDTO build(ElasticFeaturestoreRequest req, Integer projectId)
    throws ElasticException, ServiceException, GenericException {
    checkRequest(req);
    Project project = projectFacade.find(projectId);
    Map<FeaturestoreDocType, Set<Integer>> searchProjects
      = datasetAccessCtrl.featurestoreSearchContext(project, req.getDocType());
    Map<FeaturestoreDocType, SearchResponse> response
      = elasticCtrl.featurestoreSearch(req.getTerm(), searchProjects, req.getFrom(), req.getSize());
  
    DatasetAccessController.DatasetAccessCtrl localProjectOnly =
      (datasetDetails, projectsCollector) -> projectsCollector.addAccessProject(project);
    
    ElasticFeaturestoreDTO result = parseResult(response, localProjectOnly);
    result.setFeaturegroupsFrom(req.getFrom());
    result.setTrainingdatasetsFrom(req.getFrom());
    result.setFeaturesFrom(req.getFrom());
    return result;
  }
  
  public ElasticFeaturestoreDTO build(Users user, ElasticFeaturestoreRequest req)
    throws ElasticException, ServiceException, GenericException {
    checkRequest(req);
    Map<FeaturestoreDocType, SearchResponse> response
      = elasticCtrl.featurestoreSearch(req.getDocType(), req.getTerm(), req.getFrom(), req.getSize());
   
    /** we are using the memoized accessor method here since we expect many results to actually belong to the same
     * dataset/project, thus there is  no use in going to database multiple times (stressing it) to get (possibly) the
     * same dataset/project/shared datasets.
     * we get a new accessor with each call and thus cache values for very short periods of time
     * (parsing this particular query result only)
     */
    ElasticFeaturestoreDTO result = parseResult(response, datasetAccessCtrl.memoizedAccessorProjects(user));
    result.setFeaturegroupsFrom(req.getFrom());
    result.setTrainingdatasetsFrom(req.getFrom());
    //TODO Alex fix v2 - from size of features
    result.setFeaturesFrom(0);
    return result;
  }
  
  private ElasticFeaturestoreDTO parseResult(Map<FeaturestoreDocType, SearchResponse> resp,
    DatasetAccessController.DatasetAccessCtrl accessCtrl)
    throws ElasticException, GenericException {
    ElasticFeaturestoreDTO result = new ElasticFeaturestoreDTO();
    for(Map.Entry<FeaturestoreDocType, SearchResponse> e : resp.entrySet()) {
      switch(e.getKey()) {
        case FEATUREGROUP: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            ElasticFeaturestoreHit hit = ElasticFeaturestoreHit.instance(hitAux);
            ElasticFeaturestoreItemDTO.Base item = ElasticFeaturestoreItemDTO.fromFeaturegroup(hit, converter);
            item.setHighlights(getHighlights(hitAux.getHighlightFields()));
            accessCtrl.accept(inputWrapper(hit), collectorWrapper(item));
            result.addFeaturegroup(item);
          }
          result.setFeaturegroupsTotal(e.getValue().getHits().getTotalHits().value);
        } break;
        case TRAININGDATASET: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            ElasticFeaturestoreHit hit = ElasticFeaturestoreHit.instance(hitAux);
            ElasticFeaturestoreItemDTO.Base item = ElasticFeaturestoreItemDTO.fromTrainingDataset(hit, converter);
            item.setHighlights(getHighlights(hitAux.getHighlightFields()));
            accessCtrl.accept(inputWrapper(hit), collectorWrapper(item));
            result.addTrainingdataset(item);
          }
          result.setTrainingdatasetsTotal(e.getValue().getHits().getTotalHits().value);
        } break;
        case FEATURE: {
          for (SearchHit hitAux : e.getValue().getHits()) {
            ElasticFeaturestoreHit hit = ElasticFeaturestoreHit.instance(hitAux);
            ElasticFeaturestoreItemDTO.Base fgParent = ElasticFeaturestoreItemDTO.fromFeaturegroup(hit, converter);
            Map<String, HighlightField> highlightFields = hitAux.getHighlightFields();
            String featureField
              = FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.FG_FEATURES);
            HighlightField hf = highlightFields.get(featureField + ".keyword");
            if (hf != null) {
              for (Text ee : hf.fragments()) {
                String feature = removeHighlightTags(ee.toString());
                ElasticFeaturestoreItemDTO.Feature item = ElasticFeaturestoreItemDTO.fromFeature(feature, fgParent);
                ElasticFeaturestoreItemDTO.Highlights highlights = new ElasticFeaturestoreItemDTO.Highlights();
                highlights.setName(ee.toString());
                item.setHighlights(highlights);
                accessCtrl.accept(inputWrapper(hit), collectorWrapper(item));
                result.addFeature(item);
              }
            }
          }
          //TODO Alex fix v2 - from size of features
          //result.setFeaturesTotal(e.getValue().getHits().getTotalHits().value);
          result.setFeaturesTotal((long)result.getFeatures().size());
        } break;
      }
    }
    return result;
  }
  
  private ElasticFeaturestoreItemDTO.Highlights getHighlights(Map<String, HighlightField> map) {
    ElasticFeaturestoreItemDTO.Highlights highlights = new ElasticFeaturestoreItemDTO.Highlights();
    for(Map.Entry<String, HighlightField> e : map.entrySet()) {
      if(e.getKey().endsWith(".keyword")) {
        continue;
      }
      if(e.getKey().equals(FeaturestoreXAttrsConstants.NAME)
        || e.getKey().equals(FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.NAME))) {
        highlights.setName(e.getValue().fragments()[0].toString());
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.DESCRIPTION))) {
        highlights.setDescription(e.getValue().fragments()[0].toString());
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.FG_FEATURES))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeature(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(
        FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(
          FeaturestoreXAttrsConstants.TD_FEATURES, FeaturestoreXAttrsConstants.FG_FEATURES))) {
        for(Text t : e.getValue().fragments()) {
          highlights.addFeature(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(FeaturestoreXAttrsConstants.getTagsElasticKey())) {
        for(Text t : e.getValue().fragments()) {
          highlights.addTagKey(t.toString());
        }
        continue;
      }
      if(e.getKey().equals(FeaturestoreXAttrsConstants.getTagsElasticValue())) {
        for(Text t : e.getValue().fragments()) {
          highlights.addTagValue(t.toString());
        }
        continue;
      }
      
      if(e.getKey().startsWith(FeaturestoreXAttrsConstants.ELASTIC_XATTR + ".")) {
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
  
  private DatasetAccessController.DatasetDetails inputWrapper(ElasticFeaturestoreHit hit) {
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
  
  private DatasetAccessController.ProjectsCollector collectorWrapper(ElasticFeaturestoreItemDTO.Base item) {
    return new DatasetAccessController.ProjectsCollector() {
      @Override
      public void addAccessProject(Project project) {
        item.addAccessProject(project.getId(), project.getName());
      }
    };
  }
}
