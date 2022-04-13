/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.opensearch;

import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.action.search.MultiSearchResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.opensearch.search.sort.NestedSortBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
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

import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.fuzzyQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.matchQuery;
import static org.opensearch.index.query.QueryBuilders.nestedQuery;
import static org.opensearch.index.query.QueryBuilders.prefixQuery;
import static org.opensearch.index.query.QueryBuilders.queryStringQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import static org.opensearch.index.query.QueryBuilders.termsQuery;
import static org.opensearch.index.query.QueryBuilders.wildcardQuery;
import static org.opensearch.index.query.QueryBuilders.existsQuery;

/**
 *
 * <p>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OpenSearchController {

  @EJB
  private Settings settings;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private KibanaClient kibanaClient;
  @EJB
  private OpenSearchClientController elasticClientCtrl;
  
  private static final Logger LOG = Logger.getLogger(OpenSearchController.class.getName());
  

  public SearchHit[] globalSearchHighLevel(String searchTerm) throws ServiceException, OpenSearchException {
    //check if the index are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.OPENSEARCH_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.META_INDEX);
    }
  
    LOG.log(Level.FINE, "Found opensearch index, now executing the query.");
  
    SearchResponse response = executeSearchQuery(globalSearchQuery(searchTerm.toLowerCase()));

    if (response.status().getStatus() == 200) {
      if (response.getHits().getHits().length > 0) {
        return response.getHits().getHits();
      }
      return new SearchHit[0];
    }
    //we need to further check the status if it is a problem with
    // opensearch rather than a bad query
    throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_QUERY_ERROR,
      Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }
  
  public SearchHit[] projectSearchHighLevel(Integer projectId, String searchTerm) throws ServiceException,
    OpenSearchException {
    //check if the index are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.OPENSEARCH_INDEX_NOT_FOUND,
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
    // opensearch rather than a bad query
    throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_QUERY_ERROR,
      Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }
  
  public SearchHit[] datasetSearchHighLevel(Integer projectId, String datasetName, String searchTerm)
    throws ServiceException, OpenSearchException {
    //check if the indices are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.OPENSEARCH_INDEX_NOT_FOUND,
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
    // opensearch rather than a bad query
    throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_QUERY_ERROR,
      Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }

  public SearchHit[] recentJupyterNotebookSearch(int count, int projectId) throws OpenSearchException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(Settings.META_DATA_NESTED_FIELD + ".");
    stringBuilder.append(Settings.META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME + ".");
    stringBuilder.append(Settings.META_USAGE_TIME);
    String usage_time_path = stringBuilder.toString();
    SortBuilder sortBuilder = SortBuilders.fieldSort(usage_time_path)
            .order(SortOrder.DESC)
            .setNestedSort(new NestedSortBuilder(Settings.META_DATA_NESTED_FIELD))
            .unmappedType("long");
    SearchResponse response = executeJupyterSearchQuery(
            getRecentNotebooks(usage_time_path, projectId), count, sortBuilder);
    return response.getHits().getHits();
  }

  /**
   *
   * @param docType
   * @param searchTerm
   * @param from
   * @param size
   * @return even if you passed as type ALL, expect result to contain FEATUREGROUP, TRAININGDATASET, FEATURE
   * @throws OpenSearchException
   * @throws ServiceException
   */
  public Map<FeaturestoreDocType, SearchResponse> featurestoreSearch(FeaturestoreDocType docType,
    String searchTerm, int from, int size)
    throws OpenSearchException, ServiceException {
    //check if the indices are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.FEATURESTORE_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.OPENSEARCH_INDEX_NOT_FOUND,
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
   * @throws OpenSearchException
   * @throws ServiceException
   */
  public Map<FeaturestoreDocType, SearchResponse> featurestoreSearch(String searchTerm,
    Map<FeaturestoreDocType, Set<Integer>> docProjectIds, int from, int size)
    throws OpenSearchException, ServiceException {
    //check if the indices are up and running
    if (!elasticClientCtrl.mngIndexExists(Settings.FEATURESTORE_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.OPENSEARCH_INDEX_NOT_FOUND,
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
  
  private void checkResponse(QueryBuilder qb,SearchResponse response) throws OpenSearchException {
    if (response == null || response.status().getStatus() != 200) {
      //we need to further check the status if it is a problem with
      // opensearch rather than a bad query
      LOG.log(Level.FINE,"error while executing query:{0} response is:{1}",
        new Object[]{qb, (response == null ? null : response.status().getStatus())});
      throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_QUERY_ERROR,
        Level.WARNING, "Error while executing opensearch query");
    }
  }
    
  public void createIndexPattern(Project project, Users user, String pattern)
      throws ProjectException, OpenSearchException {
    JSONObject resp = kibanaClient.createIndexPattern(user, project,
        KibanaClient.KibanaType.IndexPattern, pattern);
    
    if (!(resp.has("updated_at") || (resp.has("statusCode") && resp.get("statusCode").toString().equals("409")))) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_KIBANA_CREATE_INDEX_ERROR, Level.SEVERE, null,
          "project: " + project.getName() + ", resp: " + resp.toString(2), null);
    }
  }
  
  public void deleteProjectIndices(Project project) throws OpenSearchException {
    //Get all project indices
    String[] indices = elasticClientCtrl.mngIndicesGetByRegex(project.getName() +
      "_(((logs|serving)-\\d{4}.\\d{2}.\\d{2}))");
    for (String index : indices) {
      try {
        elasticClientCtrl.mngIndexDelete(index);
      } catch(OpenSearchException e) {
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
      throws OpenSearchException {
    if(!settings.isKibanaMultiTenancyEnabled()){
      throw new UnsupportedOperationException("Only multitenant kibana setup " +
          "supported.");
    }
    
    elasticClientCtrl.mngIndexDelete(OpenSearchUtils.getAllKibanaTenantIndex(project.toLowerCase()));
  }
  
  private SearchHit[] projectSearchInSharedDatasets(Integer projectId, String searchTerm,
    SearchHit[] elasticHits) throws OpenSearchException {
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
    throws OpenSearchException {
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
    throws OpenSearchException {
    return executeSearchQuery(Settings.META_INDEX, query);
  }
  
  private SearchResponse executeSearchQuery(String index, QueryBuilder query)
    throws OpenSearchException {
    //hit the indices - execute the queries
    SearchRequest searchRequest = new SearchRequest(index);
    SearchSourceBuilder sb = new SearchSourceBuilder();
    sb.query(query);
    searchRequest.source(sb);
    return elasticClientCtrl.baseSearch(searchRequest);
  }
  
  private SearchResponse executeSearchQuery(QueryBuilder query, HighlightBuilder highlighter, int from, int size)
    throws OpenSearchException {
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

  private SearchResponse executeJupyterSearchQuery(QueryBuilder query, int size, SortBuilder sort)
          throws OpenSearchException {
    //hit the indices - execute the queries
    SearchRequest searchRequest = new SearchRequest(Settings.META_INDEX);
    SearchSourceBuilder sb = new SearchSourceBuilder()
            .query(query)
            .size(size)
            .sort(sort);
    searchRequest.source(sb);
    return elasticClientCtrl.baseSearch(searchRequest);
  }
  
  private MultiSearchResponse executeSearchQuery(List<Pair<QueryBuilder, HighlightBuilder>> searchQB,
                                                 int from, int size)
    throws OpenSearchException {
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
    QueryBuilder featureQuery = termQuery(Settings.META_DOC_TYPE_FIELD,
      FeaturestoreDocType.FEATUREGROUP.toString().toLowerCase());
    
    String featureName = FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
      FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.NAME);
    String description = FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
      FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.DESCRIPTION);
    QueryBuilder baseQuery = boolQuery()
      .should(termFullTextQueryInt(featureName, searchTerm))
      .should(phraseFullTextQueryInt(description, searchTerm));
    
    QueryBuilder nestedQuery = nestedQuery(Settings.META_DATA_NESTED_FIELD, baseQuery, ScoreMode.Avg);
    return boolQuery()
      .must(featureQuery)
      .must(nestedQuery);
  }
  
  private HighlightBuilder featuregroupHighlighter() {
    HighlightBuilder hb = new HighlightBuilder();
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.NAME));
    hb.field(new HighlightBuilder.Field(
      FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(FeaturestoreXAttrsConstants.DESCRIPTION)));
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.OPENSEARCH_XATTR + ".*"));
    return hb;
  }
  
  private HighlightBuilder trainingDatasetHighlighter() {
    HighlightBuilder hb = new HighlightBuilder();
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.NAME));
    hb.field(new HighlightBuilder.Field(
      FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(FeaturestoreXAttrsConstants.DESCRIPTION)));
    hb.field(new HighlightBuilder.Field(FeaturestoreXAttrsConstants.OPENSEARCH_XATTR + ".*"));
    return hb;
  }
  
  private HighlightBuilder featureHighlighter() {
    HighlightBuilder hb = new HighlightBuilder();
    hb.field(new HighlightBuilder.Field(
      FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
        FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.NAME)));
    hb.field(new HighlightBuilder.Field(
      FeaturestoreXAttrsConstants.getFeaturestoreOpenSearchKey(
        FeaturestoreXAttrsConstants.FG_FEATURES, FeaturestoreXAttrsConstants.DESCRIPTION)));
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
   * Creates the query that is used to get the most recently used Jupyter notebooks.
   * <p/>
   * @return
   */
  private QueryBuilder getRecentNotebooks(String existsLocation, int projectId) {
    QueryBuilder existQuery = existsQuery(existsLocation);

    QueryBuilder boolQuery = boolQuery()
            .must(existQuery);

    QueryBuilder nestedQuery = nestedQuery(Settings.META_DATA_NESTED_FIELD, boolQuery, ScoreMode.None);

    QueryBuilder wildCardQuery = wildcardQuery(Settings.META_NAME_FIELD,"*ipynb");

    QueryBuilder termQuery = termQuery("project_id", projectId);

    QueryBuilder nameQuery = boolQuery()
            .must(nestedQuery)
            .must(wildCardQuery)
            .must(termQuery);

    return nameQuery;
  }

  /**
   * Creates the query that is applied on the name field.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getNameQuery(String searchTerm) {
    return termFullTextQueryInt(Settings.META_NAME_FIELD, searchTerm);
  }
  
  private QueryBuilder termFullTextQueryInt(String key, String searchTerm) {
    QueryBuilder namePrefixMatch = prefixQuery(key, searchTerm);
    QueryBuilder namePhraseMatch = matchPhraseQuery(key, searchTerm);
    QueryBuilder nameFuzzyQuery = fuzzyQuery(key, searchTerm);
    QueryBuilder wildCardQuery = wildcardQuery(key, String.format("*%s*", searchTerm));
  
    return boolQuery()
      .should(namePrefixMatch)
      .should(namePhraseMatch)
      .should(nameFuzzyQuery)
      .should(wildCardQuery);
  }

  /**
   * Creates the query that is applied on the text fields of a document. Hits
   * the description fields
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getDescriptionQuery(String searchTerm) {
    return phraseFullTextQueryInt(Settings.META_DESCRIPTION_FIELD, searchTerm);
  }
  
  private QueryBuilder phraseFullTextQueryInt(String key, String searchTerm) {
    //do a prefix query on the description field in case the user starts writing a full sentence
    QueryBuilder descriptionPrefixMatch = prefixQuery(key, searchTerm);
    //a phrase query to match the dataset description
    QueryBuilder descriptionMatch = termsQuery(key, searchTerm);
    //add a phrase match query to enable results to popup while typing phrases
    QueryBuilder descriptionPhraseMatch = matchPhraseQuery(key, searchTerm);
    //add a fuzzy search on description field
    QueryBuilder descriptionFuzzyQuery = fuzzyQuery(key, searchTerm);
    QueryBuilder wildCardQuery = wildcardQuery(key, String.format("*%s*", searchTerm));

    return boolQuery()
        .should(descriptionPrefixMatch)
        .should(descriptionMatch)
        .should(descriptionPhraseMatch)
        .should(descriptionFuzzyQuery)
        .should(wildCardQuery);
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
    QueryBuilder metadataQuery =  queryStringQuery(String.format("*%s*", searchTerm))
      .lenient(Boolean.TRUE)
      .field(key);
    QueryBuilder nestedQuery = nestedQuery(Settings.META_DATA_NESTED_FIELD, metadataQuery, ScoreMode.Avg);
  
    return nestedQuery;
  }
}


