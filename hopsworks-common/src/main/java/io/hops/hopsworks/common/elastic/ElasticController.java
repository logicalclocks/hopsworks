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

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWith;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.proxies.client.NotFoundClientProtocolException;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.common.util.Settings;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private ElasticClient elasticClient;
  @EJB
  private KibanaClient kibanaClient;
  
  private static final Logger LOG = Logger.getLogger(ElasticController.class.getName());
  

  public List<ElasticHit> globalSearch(String searchTerm)
      throws ServiceException, ElasticException {
    //some necessary client settings
    RestHighLevelClient client = getClient();

    //check if the index are up and running
    if (!this.indexExists(client, Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.META_INDEX);
    }

    LOG.log(Level.INFO, "Found elastic index, now executing the query.");
    
    SearchResponse response = executeSearchQuery(client,
        globalSearchQuery(searchTerm.toLowerCase()));
    
    if (response.status().getStatus() == 200) {
      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          ElasticHit eHit = new ElasticHit(hit);
          eHit.setLocalDataset(true);
          long inode_id = Long.parseLong(hit.getId());
          Dataset dsl = datasetController.getDatasetByInodeId(inode_id);
          if (dsl != null  && dsl.isPublicDs()) {
            Dataset ds = dsl;
            eHit.setPublicId(ds.getPublicDsId());
          }
          elasticHits.add(eHit);
        }
      }

      return elasticHits;
    }
    
    //we need to further check the status if it is a problem with
    // elasticsearch rather than a bad query
    throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR,
        Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }

  public List<ElasticHit> projectSearch(Integer projectId, String searchTerm)
      throws ServiceException, ElasticException {
    RestHighLevelClient client = getClient();

    //check if the index are up and running
    if (!this.indexExists(client, Settings.META_INDEX)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_NOT_FOUND,
        Level.SEVERE, "index: " + Settings.META_INDEX);
    }
    
    SearchResponse response = executeSearchQuery(client,
        projectSearchQuery(projectId, searchTerm.toLowerCase()));

    if (response.status().getStatus() == 200) {
      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();
        ElasticHit eHit;
        for (SearchHit hit : hits) {
          eHit = new ElasticHit(hit);
          eHit.setLocalDataset(true);
          elasticHits.add(eHit);
        }
      }

      projectSearchInSharedDatasets(client, projectId, searchTerm, elasticHits);
      return elasticHits;
    }
  
    //we need to further check the status if it is a probelm with
    // elasticsearch rather than a bad query
    throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR,
        Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }

  public List<ElasticHit> datasetSearch(Integer projectId, String datasetName, String searchTerm)
      throws ServiceException, ElasticException {
    RestHighLevelClient client = getClient();
    //check if the indices are up and running
    if (!this.indexExists(client, Settings.META_INDEX)) {
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
    
    SearchResponse response = executeSearchQuery(client,
        datasetSearchQuery(datasetId, searchTerm.toLowerCase()));
    
    if (response.status().getStatus() == 200) {
      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();
        ElasticHit eHit;
        for (SearchHit hit : hits) {
          eHit = new ElasticHit(hit);
          eHit.setLocalDataset(true);
          elasticHits.add(eHit);
        }
      }
      return elasticHits;
    }
  
    //we need to further check the status if it is a probelm with
    // elasticsearch rather than a bad query
    throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR,
        Level.INFO,"Error while executing query, code: "+  response.status().getStatus());
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public boolean deleteIndex(String index)
      throws ElasticException {
    boolean acked =
        false;
    try {
      acked = getClient().indices().delete(new DeleteIndexRequest(index),
          RequestOptions.DEFAULT).isAcknowledged();
    } catch (IOException e) {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR,
          Level.INFO,"Error while deleting an index", e.getMessage(), e);
    }
    if (acked) {
      LOG.log(Level.INFO, "Acknowledged deletion of elastic index:{0}", index);
    } else {
      LOG.log(Level.SEVERE, "Elastic index:{0} deletion could not be acknowledged", index);
    }
    return acked;
  }

  public boolean indexExists(String index)
      throws ElasticException {
    boolean exists = indexExists(getClient(), index);
    if (exists) {
      LOG.log(Level.FINE, "Elastic index found:{0}", index);
    } else {
      LOG.log(Level.FINE, "Elastic index:{0} could not be found", index);
    }
    return exists;
  }

  public void createIndex(String index)
      throws ServiceException, ElasticException {
  
    boolean acked = false;
    try {
      acked = getClient().indices().create(new CreateIndexRequest(index), RequestOptions.DEFAULT).isAcknowledged();
    } catch (IOException e) {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR,
          Level.INFO,"Error while creating an index", e.getMessage(), e);
    }
    if (!acked) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_INDEX_CREATION_ERROR,  Level.SEVERE,
        "Elastic index:{0} creation could not be acknowledged. index: " + index);
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
  
  public void deleteIndexPattern(Project project, String pattern)
      throws ElasticException {
    try {
      JSONObject resp = kibanaClient.deleteAsDataOwner(project,
          KibanaClient.KibanaType.IndexPattern, pattern);
      LOG.log(Level.INFO, "Deletion of kibana index pattern:{0} Response {1}",
          new Object[]{pattern, resp.toString()});
    }catch (ElasticException ex){
      if(ex.getCause() instanceof NotFoundClientProtocolException){
        LOG.log(Level.INFO, "Index pattern:{0} was already deleted", pattern);
        return;
      }
      throw ex;
    }
  }
  
  public JSONObject updateKibana(Project project, Users user,
      KibanaClient.KibanaType type, String id, String data)
      throws ElasticException {
    return kibanaClient.postWithOverwrite(user, project, type, id, data);
  }
  
  public void deleteProjectIndices(Project project)
      throws ElasticException {
    //Get all project indices
    Map<String, Long> indices = getIndices(project.getName() +
      "_(((logs|serving|" + Settings.ELASTIC_BEAMSDKWORKER_INDEX_PATTERN + "|" +
      Settings.ELASTIC_BEAMJOBSERVER_INDEX_PATTERN + ")-\\d{4}.\\d{2}.\\d{2})|(" +
        Settings.ELASTIC_KAGENT_INDEX_PATTERN + "))");
    for (String index : indices.keySet()) {
      if (!deleteIndex(index)) {
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
    
    deleteIndex(ElasticUtils.getAllKibanaTenantIndex(project.toLowerCase()));
  }
  
  /**
   * Get all indices. If pattern parameter is provided, only indices matching the pattern will be returned.
   * @param regex
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Map<String, Long> getIndices(String regex)
      throws ElasticException {
    
    try {
      RestHighLevelClient client = getClient();
      Response response = client.getLowLevelClient().performRequest(new Request(
          "GET", "/_cat/indices?h=i,creation" +
          ".date&format=json"));
      JSONArray jsonArray =
          new JSONArray(EntityUtils.toString(response.getEntity()));
  
      Map<String, Long> indicesMap = new HashMap<>();
      Pattern pattern = null;
      if (regex != null) {
        pattern = Pattern.compile(regex);
      }
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject index = jsonArray.getJSONObject(i);
        String indexName = index.getString("i");
        Long creationDate = index.getLong("creation.date");
    
        if (pattern == null || pattern.matcher(indexName).matches()) {
          indicesMap.put(indexName, creationDate);
        }
      }
      return indicesMap;
    }catch (IOException e){
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR,
          Level.INFO,"Error while getting all indices", e.getMessage(), e);
    }
  }

  private RestHighLevelClient getClient() throws ElasticException {
    return elasticClient.getClient();
  }
  
  private void projectSearchInSharedDatasets(RestHighLevelClient client, Integer projectId,
      String searchTerm, List<ElasticHit> elasticHits) throws ServiceException {
    Project project = projectFacade.find(projectId);
    Collection<DatasetSharedWith> datasetSharedWithCollection = project.getDatasetSharedWithCollectionCollection();
    for (DatasetSharedWith ds : datasetSharedWithCollection) {
      long datasetId = ds.getDataset().getInode().getId();
      executeProjectSearchQuery(client, searchSpecificDataset(datasetId, searchTerm), elasticHits);
      executeProjectSearchQuery(client, datasetSearchQuery(datasetId, searchTerm), elasticHits);
    
    }
    
  }

  private void executeProjectSearchQuery(RestHighLevelClient client, QueryBuilder query,
      List<ElasticHit> elasticHits) throws ServiceException {
    
    SearchResponse response = executeSearchQuery(client, query);
    
    if (response.status().getStatus() == 200) {
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }
    }
  }
  
  private SearchResponse executeSearchQuery(RestHighLevelClient client, QueryBuilder query)
      throws ServiceException {
    return executeSearchQuery(client, Settings.META_INDEX, query);
  }
  
  private SearchResponse executeSearchQuery(RestHighLevelClient client,
      String index, QueryBuilder query)
      throws ServiceException {
    //hit the indices - execute the queries
    SearchRequest searchRequest = new SearchRequest(index);
    SearchSourceBuilder sb = new SearchSourceBuilder();
    sb.query(query);
    searchRequest.source(sb);
    LOG.log(Level.INFO, "Search Elastic query is: {0}", searchRequest);
  
    try {
      return client.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_SERVER_NOT_FOUND,
          Level.SEVERE,"Error while executing search", e.getMessage());
    }
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

    QueryBuilder metadataQuery = queryStringQuery(String.format("*%s*",
        searchTerm))
        .lenient(Boolean.TRUE)
        .field(Settings.META_DATA_FIELDS);
    QueryBuilder nestedQuery = nestedQuery(Settings.META_DATA_NESTED_FIELD,
        metadataQuery, ScoreMode.Avg);

    return nestedQuery;
  }

  /**
   * Checks if a given index exists in elastic
   * <p/>
   * @param client
   * @param indexName
   * @return
   */
  private boolean indexExists(RestHighLevelClient client, String indexName)
      throws ElasticException {
    try {
      return client.indices().exists(new GetIndexRequest(indexName),
          RequestOptions.DEFAULT);
    } catch (IOException e) {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR,
          Level.INFO,"Error while checking index existence", e.getMessage(), e);
    }
  }
}


