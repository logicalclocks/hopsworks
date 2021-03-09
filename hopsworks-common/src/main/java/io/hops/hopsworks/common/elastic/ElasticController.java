/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.elastic;

import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.javatuples.Pair;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

/**
 *
 * <p>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticController {

  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private KibanaClient kibanaClient;
  @EJB
  private ElasticClientController elasticClientCtrl;
  
  private static final Logger LOG = Logger.getLogger(ElasticController.class.getName());
  

  public SearchHit[] globalSearchHighLevel(String searchTerm) throws ServiceException, ElasticException {
    //check if the index are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.META_INDEX);
    }
  
    LOG.log(Level.FINE, "Found elastic index, now executing the query.");
  
    SearchResponse response = executeSearchQuery(globalSearchQuery(searchTerm.toLowerCase()));

    if (response.status().getStatus() == 200) {
      if (response.getHits().getHits().length > 0) {
        return response.getHits().getHits();
      }
      return new SearchHit[0];
    }
    //we need to further check the status if it is a problem with
    // elasticsearch rather than a bad query
    throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR,
      Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }
  
  public SearchHit[] projectSearchHighLevel(Integer projectId, String searchTerm) throws ServiceException,
    ElasticException {
    //check if the index are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.META_INDEX);
    }
    
    SearchResponse response = executeSearchQuery(projectSearchQuery(projectId, searchTerm.toLowerCase()));
    if (response.status().getStatus() == 200) {
      SearchHit[] hits = new SearchHit[0];
      if (response.getHits().getHits().length > 0) {
        hits = response.getHits().getHits();
      }
      projectSearchInSharedDatasets(projectId, searchTerm, hits);
      return hits;
    }
    //we need to further check the status if it is a probelm with
    // elasticsearch rather than a bad query
    throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR,
      Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }
  
  public SearchHit[] datasetSearchHighLevel(Integer projectId, String datasetName, String searchTerm)
    throws ServiceException, ElasticException {
    //check if the indices are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.META_INDEX);
    }
    
    String dsName = datasetName;
    Project project;
    if (datasetName.contains(Settings.SHARED_FILE_SEPARATOR)) {
      String[] sharedDS = datasetName.split(Settings.SHARED_FILE_SEPARATOR);
      dsName = sharedDS[1];
      project = projectFacade.findByName(sharedDS[0]);
    } else {
      project = projectFacade.find(projectId);
    }
    
    Dataset dataset = datasetController.getByProjectAndDsName(project,null, dsName);
    final long datasetId = dataset.getInodeId();
    SearchResponse response = executeSearchQuery(datasetSearchQuery(datasetId, searchTerm.toLowerCase()));
    if (response.status().getStatus() == 200) {
      if (response.getHits().getHits().length > 0) {
        return response.getHits().getHits();
      }
      return new SearchHit[0];
    }
    //we need to further check the status if it is a problem with
    // elasticsearch rather than a bad query
    throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR,
      Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }
  
  /**
   *
   * @param docType
   * @param searchTerm
   * @param from
   * @param size
   * @return even if you passed as type ALL, expect result to contain FEATUREGROUP, TRAININGDATASET, FEATURE
   * @throws ElasticException
   * @throws ServiceException
   */
  public Map<FeaturestoreDocType, SearchResponse> featurestoreSearch(FeaturestoreDocType docType,
    String searchTerm, int from, int size)
    throws ElasticException, ServiceException {
    //check if the indices are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.FEATURESTORE_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.FEATURESTORE_INDEX);
    }
    
    Map<FeaturestoreDocType, SearchResponse> result = new HashMap<>();
    switch(docType) {
      case FEATUREGROUP: {
        QueryBuilder fgQB = featuregroupQueryB(searchTerm);
        SearchResponse response = executeSearchQuery(fgQB, featuregroupHighlighter(), from, size);
        checkResponse(fgQB, response);
        result.put(FeaturestoreDocType.FEATUREGROUP, response);
      } break;
      case TRAININGDATASET: {
        QueryBuilder tdQB = trainingdatasetQueryB(searchTerm);
        SearchResponse response = executeSearchQuery(tdQB, trainingDatasetHighlighter(), from, size);
        checkResponse(tdQB, response);
        result.put(FeaturestoreDocType.TRAININGDATASET, response);
      } break;
      case FEATURE: {
        QueryBuilder fQB = featureQueryB(searchTerm);
        //TODO Alex - v2 use actual from size of features
        SearchResponse response = executeSearchQuery(fQB, featureHighlighter(), 0, 10000);
        checkResponse(fQB, response);
        result.put(FeaturestoreDocType.FEATURE, response);
      } break;
      case ALL: {
        List<Pair<QueryBuilder, HighlightBuilder>> qbs = new LinkedList<>();
        QueryBuilder fgQB = featuregroupQueryB(searchTerm);
        qbs.add(Pair.with(fgQB, featuregroupHighlighter()));
        QueryBuilder tdQB = trainingdatasetQueryB(searchTerm);
        qbs.add(Pair.with(tdQB, trainingDatasetHighlighter()));
        QueryBuilder fQB = featureQueryB(searchTerm);
        qbs.add(Pair.with(fQB, featureHighlighter()));
        
        MultiSearchResponse response = executeSearchQuery(qbs, from, size);
        
        checkResponse(fgQB, response.getResponses()[0].getResponse());
        result.put(FeaturestoreDocType.FEATUREGROUP, response.getResponses()[0].getResponse());
        checkResponse(fgQB, response.getResponses()[1].getResponse());
        result.put(FeaturestoreDocType.TRAININGDATASET, response.getResponses()[1].getResponse());
        checkResponse(fgQB, response.getResponses()[2].getResponse());
        result.put(FeaturestoreDocType.FEATURE, response.getResponses()[2].getResponse());
      } break;
    }
    return result;
  }
  
  /**
   *
   * @param searchTerm
   * @param docProjectIds - pe specific FEATUREGROUP, TRAININGDATASET, FEATURE. No ALL allowed
   * @param from
   * @param size
   * @return
   * @throws ElasticException
   * @throws ServiceException
   */
  public Map<FeaturestoreDocType, SearchResponse> featurestoreSearch(String searchTerm,
    Map<FeaturestoreDocType, Set<Integer>> docProjectIds, int from, int size)
    throws ElasticException, ServiceException {
    //check if the indices are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.FEATURESTORE_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.FEATURESTORE_INDEX);
    }
  
    QueryBuilder fgQB = null;
    QueryBuilder tdQB = null;
    QueryBuilder fQB = null;
    List<Pair<QueryBuilder, HighlightBuilder>> qbs = new LinkedList<>();
    if(docProjectIds.containsKey(FeaturestoreDocType.FEATUREGROUP)) {
      fgQB = addProjectToQuery(featuregroupQueryB(searchTerm),
        docProjectIds.get(FeaturestoreDocType.FEATUREGROUP));
      qbs.add(Pair.with(fgQB, featuregroupHighlighter()));
    }
    if(docProjectIds.containsKey(FeaturestoreDocType.TRAININGDATASET)) {
      tdQB = addProjectToQuery(trainingdatasetQueryB(searchTerm),
        docProjectIds.get(FeaturestoreDocType.TRAININGDATASET));
      qbs.add(Pair.with(tdQB, trainingDatasetHighlighter()));
    }
    if(docProjectIds.containsKey(FeaturestoreDocType.FEATURE)) {
      fQB = addProjectToQuery(featureQueryB(searchTerm),
        docProjectIds.get(FeaturestoreDocType.FEATURE));
      qbs.add(Pair.with(fQB, featureHighlighter()));
    }
    
    MultiSearchResponse response = executeSearchQuery(qbs, from, size);
    
    Map<FeaturestoreDocType, SearchResponse> result = new HashMap<>();
    int idx = 0;
    if(docProjectIds.containsKey(FeaturestoreDocType.FEATUREGROUP)) {
      checkResponse(fgQB, response.getResponses()[idx].getResponse());
      result.put(FeaturestoreDocType.FEATUREGROUP, response.getResponses()[idx].getResponse());
      idx++;
    }
    if(docProjectIds.containsKey(FeaturestoreDocType.TRAININGDATASET)) {
      checkResponse(tdQB, response.getResponses()[idx].getResponse());
      result.put(FeaturestoreDocType.TRAININGDATASET, response.getResponses()[idx].getResponse());
      idx++;
    }
    if(docProjectIds.containsKey(FeaturestoreDocType.FEATURE)) {
      checkResponse(fQB, response.getResponses()[idx].getResponse());
      result.put(FeaturestoreDocType.FEATURE, response.getResponses()[idx].getResponse());
      idx++;
    }
    return result;
  }
  
  private QueryBuilder addProjectToQuery(QueryBuilder qb, Set<Integer> projectIds) {
    return boolQuery()
      .must(termsQuery(Settings.FEATURESTORE_PROJECT_ID_FIELD, projectIds))
      .must(qb);
  }
  
  private void checkResponse(QueryBuilder qb,SearchResponse response) throws ElasticException {
    if (response == null || response.status().getStatus() != 200) {
      //we need to further check the status if it is a problem with
      // elasticsearch rather than a bad query
      LOG.log(Level.FINE,"error while executing query:{0} response is:{1}",
        new Object[]{qb, (response == null ? null : response.status().getStatus())});
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR,
        Level.WARNING, "Error while executing elastic query");
    }
  }
    
  public void createIndexPattern(Project project, Users user, String pattern)
      throws ProjectException, ElasticException {
    JSONObject resp = kibanaClient.createIndexPattern(user, project,
        KibanaClient.KibanaType.IndexPattern, pattern);
    
    if (!(resp.has("updated_at") || (resp.has("statusCode") && resp.get("statusCode").toString().equals("409")))) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_KIBANA_CREATE_INDEX_ERROR, Level.SEVERE, null,
          "project: " + project.getName() + ", resp: " + resp.toString(2), null);
    }
  }
  
  public void deleteProjectIndices(Project project) throws ElasticException {
    //Get all project indices
    String[] indices = elasticClientCtrl.mngIndicesGet(project.getName() +
      "_(((logs|serving|" + Settings.ELASTIC_BEAMSDKWORKER_INDEX_PATTERN + "|" +
      Settings.ELASTIC_BEAMJOBSERVER_INDEX_PATTERN + ")-\\d{4}.\\d{2}.\\d{2}))");
    for (String index : indices) {
      try {
        elasticClientCtrl.mngIndexDelete(index);
      } catch(ElasticException e) {
        LOG.log(Level.SEVERE, "Could not delete project index:{0}", index);
      }
    }
  }

  /**
   * Deletes index patterns, visualizations, saved searches and dashboards for a project.
   *
   * @param project
   */
  public void deleteProjectSavedObjects(String project)
      throws ElasticException {
    if(!settings.isKibanaMultiTenancyEnabled()){
      throw new UnsupportedOperationException("Only multitenant kibana setup " +
          "supported.");
    }
    
    elasticClientCtrl.mngIndexDelete(ElasticUtils.getAllKibanaTenantIndex(project.toLowerCase()));
  }
  
  private SearchHit[] projectSearchInSharedDatasets(Integer projectId, String searchTerm,
    SearchHit[] elasticHits) throws ElasticException {
    Project project = projectFacade.find(projectId);
    Collection<DatasetSharedWith> datasetSharedWithCollection = project.getDatasetSharedWithCollection();
    for (DatasetSharedWith ds : datasetSharedWithCollection) {
      long datasetId = ds.getDataset().getInode().getId();
      elasticHits = executeProjectSearchQuery(searchSpecificDataset(datasetId, searchTerm), elasticHits);
      elasticHits = executeProjectSearchQuery(datasetSearchQuery(datasetId, searchTerm), elasticHits);
    }
    return elasticHits;
  }
  
  private SearchHit[] executeProjectSearchQuery(QueryBuilder query, SearchHit[] elasticHits)
    throws ElasticException {
    SearchResponse response = executeSearchQuery(query);
    if (response.status().getStatus() == 200) {
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();
        elasticHits = Stream.concat(Arrays.stream(elasticHits), Arrays.stream(hits)).toArray(SearchHit[]::new);
      }
    }
    return elasticHits;
  }
  
  private SearchResponse executeSearchQuery(QueryBuilder query)
    throws ElasticException {
    return executeSearchQuery(Settings.META_INDEX, query);
  }
  
  private SearchResponse executeSearchQuery(String index, QueryBuilder query)
    throws ElasticException {
    //hit the indices - execute the queries
    SearchRequest searchRequest = new SearchRequest(index);
    SearchSourceBuilder sb = new SearchSourceBuilder();
    sb.query(query);
    searchRequest.source(sb);
    return elasticClientCtrl.baseSearch(searchRequest);
  }
  
  private SearchResponse executeSearchQuery(QueryBuilder query, HighlightBuilder highlighter, int from, int size)
    throws ElasticException {
    //hit the indices - execute the queries
    SearchRequest searchRequest = new SearchRequest(Settings.FEATURESTORE_INDEX);
    SearchSourceBuilder sb = new SearchSourceBuilder()
      .query(query)
      .highlighter(highlighter)
      .from(from)
      .size(size);
    searchRequest.source(sb);
    return elasticClientCtrl.baseSearch(searchRequest);
  }
  
  private MultiSearchResponse executeSearchQuery(List<Pair<QueryBuilder, HighlightBuilder>> searchQB,
                                                 int from, int size)
    throws ElasticException {
    //hit the indices - execute the queries
    MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
    for(Pair<QueryBuilder, HighlightBuilder> qb : searchQB) {
      SearchRequest searchRequest = new SearchRequest(Settings.FEATURESTORE_INDEX);
      SearchSourceBuilder sb = new SearchSourceBuilder()
        .query(qb.getValue0())
        .highlighter(qb.getValue1())
        .from(from)
        .size(size);
      searchRequest.source(sb);
      multiSearchRequest.add(searchRequest);
    }
    return elasticClientCtrl.multiSearch(multiSearchRequest);
  }
  
  private QueryBuilder searchSpecificDataset(Long datasetId, String searchTerm) {
    QueryBuilder dataset = matchQuery(Settings.META_ID, datasetId);
    QueryBuilder nameDescQuery = getNameDescriptionMetadataQuery(searchTerm);
    return boolQuery()
        .must(dataset)
        .must(nameDescQuery);
  }

  /**
   * Global search on datasets and projects.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder globalSearchQuery(String searchTerm) {
    QueryBuilder nameDescQuery = getNameDescriptionMetadataQuery(searchTerm);
    QueryBuilder onlyDatasetsAndProjectsQuery = termsQuery(Settings.META_DOC_TYPE_FIELD,
        Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_PROJECT);
    QueryBuilder query = boolQuery()
        .must(onlyDatasetsAndProjectsQuery)
        .must(nameDescQuery);

    return query;
  }

  /**
   * Project specific search.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder projectSearchQuery(Integer projectId, String searchTerm) {
    QueryBuilder projectIdQuery = termQuery(Settings.META_PROJECT_ID_FIELD, projectId);
    QueryBuilder nameDescQuery = getNameDescriptionMetadataQuery(searchTerm);
    QueryBuilder onlyDatasetsAndInodes = termsQuery(Settings.META_DOC_TYPE_FIELD,
        Settings.DOC_TYPE_DATASET, Settings.DOC_TYPE_INODE);

    QueryBuilder query = boolQuery()
        .must(projectIdQuery)
        .must(onlyDatasetsAndInodes)
        .must(nameDescQuery);

    return query;
  }

  /**
   * Dataset specific search.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder datasetSearchQuery(long datasetId, String searchTerm) {
    QueryBuilder datasetIdQuery = termQuery(Settings.META_DATASET_ID_FIELD, datasetId);
    QueryBuilder query = getNameDescriptionMetadataQuery(searchTerm);
    QueryBuilder onlyInodes = termQuery(Settings.META_DOC_TYPE_FIELD,
        Settings.DOC_TYPE_INODE);

    QueryBuilder cq = boolQuery()
        .must(datasetIdQuery)
        .must(onlyInodes)
        .must(query);
    return cq;
  }
  
  private QueryBuilder featuregroupQueryB(String searchTerm) {
    QueryBuilder termQuery = boolQuery()
      .should(getNameQuery(searchTerm))
      .should(getDescriptionQuery(searchTerm))
      .should(getMetadataQuery(searchTerm));
    
    QueryBuilder query = boolQuery()
      .must(termQuery("doc_type", FeaturestoreDocType.FEATUREGROUP.toString().toLowerCase()))
      .must(termQuery);
    return query;
  }
  
  private QueryBuilder trainingdatasetQueryB(String searchTerm) {
    QueryBuilder termQuery = boolQuery()
      .should(getNameQuery(searchTerm))
      .should(getDescriptionQuery(searchTerm))
      .should(getMetadataQuery(searchTerm));
    
    QueryBuilder query = boolQuery()
      .must(termQuery("doc_type", FeaturestoreDocType.TRAININGDATASET.toString().toLowerCase()))
      .must(termQuery);
    return query;
  }
  
  private QueryBuilder featureQueryB(String searchTerm) {
    String key1 = FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.FG_FEATURES) + ".*";
    QueryBuilder query = boolQuery()
      .must(termQuery("doc_type", FeaturestoreDocType.FEATUREGROUP.toString().toLowerCase()))
      .must(getXAttrQuery(key1, searchTerm));
    return query;
  }
  
  private HighlightBuilder featuregroupHighlighter() {
    HighlightBuilder hb = new HighlightBuilder();
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.NAME));
    hb.field(new HighlightBuilder.Field(
      FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.DESCRIPTION)));
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.ELASTIC_XATTR + ".*"));
    return hb;
  }
  
  private HighlightBuilder trainingDatasetHighlighter() {
    HighlightBuilder hb = new HighlightBuilder();
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.NAME));
    hb.field(new HighlightBuilder.Field(
      FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.DESCRIPTION)));
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.ELASTIC_XATTR + ".*"));
    return hb;
  }
  
  private HighlightBuilder featureHighlighter() {
    HighlightBuilder hb = new HighlightBuilder();
    hb.field(new HighlightBuilder.Field(
      FeaturestoreXAttrsConstants.getFeaturestoreElasticKey(FeaturestoreXAttrsConstants.FG_FEATURES) + ".*"));
    return hb;
  }
  
  /**
   * Creates the main query condition. Applies filters on the texts describing a
   * document i.e. on the description
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getNameDescriptionMetadataQuery(String searchTerm) {

    QueryBuilder nameQuery = getNameQuery(searchTerm);
    QueryBuilder descriptionQuery = getDescriptionQuery(searchTerm);
    QueryBuilder metadataQuery = getMetadataQuery(searchTerm);

    QueryBuilder textCondition = boolQuery()
        .should(nameQuery)
        .should(descriptionQuery)
        .should(metadataQuery);

    return textCondition;
  }

  /**
   * Creates the query that is applied on the name field.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getNameQuery(String searchTerm) {

    //prefix name match
    QueryBuilder namePrefixMatch = prefixQuery(Settings.META_NAME_FIELD,
        searchTerm);

    QueryBuilder namePhraseMatch = matchPhraseQuery(Settings.META_NAME_FIELD,
        searchTerm);

    QueryBuilder nameFuzzyQuery = fuzzyQuery(
        Settings.META_NAME_FIELD, searchTerm);

    QueryBuilder wildCardQuery = wildcardQuery(Settings.META_NAME_FIELD,
        String.format("*%s*", searchTerm));

    QueryBuilder nameQuery = boolQuery()
        .should(namePrefixMatch)
        .should(namePhraseMatch)
        .should(nameFuzzyQuery)
        .should(wildCardQuery);

    return nameQuery;
  }

  /**
   * Creates the query that is applied on the text fields of a document. Hits
   * the description fields
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getDescriptionQuery(String searchTerm) {

    //do a prefix query on the description field in case the user starts writing
    //a full sentence
    QueryBuilder descriptionPrefixMatch = prefixQuery(
        Settings.META_DESCRIPTION_FIELD, searchTerm);

    //a phrase query to match the dataset description
    QueryBuilder descriptionMatch = termsQuery(
        Settings.META_DESCRIPTION_FIELD, searchTerm);

    //add a phrase match query to enable results to popup while typing phrases
    QueryBuilder descriptionPhraseMatch = matchPhraseQuery(
        Settings.META_DESCRIPTION_FIELD, searchTerm);

    //add a fuzzy search on description field
    QueryBuilder descriptionFuzzyQuery = fuzzyQuery(
        Settings.META_DESCRIPTION_FIELD, searchTerm);

    QueryBuilder wildCardQuery = wildcardQuery(Settings.META_DESCRIPTION_FIELD,
        String.format("*%s*", searchTerm));

    QueryBuilder descriptionQuery = boolQuery()
        .should(descriptionPrefixMatch)
        .should(descriptionMatch)
        .should(descriptionPhraseMatch)
        .should(descriptionFuzzyQuery)
        .should(wildCardQuery);

    return descriptionQuery;
  }

  /**
   * Creates the query that is applied on the text fields of a document. Hits
   * the xattr fields
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getMetadataQuery(String searchTerm) {
    return getXAttrQuery(Settings.META_DATA_FIELDS, searchTerm);
  }
  
  private QueryBuilder getXAttrQuery(String key, String searchTerm) {
    QueryBuilder metadataQuery = queryStringQuery(String.format("*%s*", searchTerm))
      .lenient(Boolean.TRUE)
      .field(key);
    QueryBuilder nestedQuery = nestedQuery(Settings.META_DATA_NESTED_FIELD, metadataQuery, ScoreMode.Avg);
  
    return nestedQuery;
  }
}


