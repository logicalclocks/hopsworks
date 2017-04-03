package io.hops.hopsworks.api.elastic;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import org.elasticsearch.search.SearchHit;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.Settings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

@Path("/elastic")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticService {

  private final static Logger logger = Logger.getLogger(ElasticService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private Settings settings;

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private DatasetFacade datasetFacade;

  /**
   * Searches for content composed of projects and datasets. Hits two elastic
   * indices: 'project' and 'dataset'
   * <p/>
   * @param searchTerm
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("globalsearch/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response globalSearch(
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    //some necessary client settings
    Client client = getClient();

    //check if the index are up and running
    if (!this.indexExists(client, Settings.META_INDEX)) {

      logger.log(Level.INFO, ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    }

    logger.log(Level.INFO, "Found elastic index, now executing the query.");

    /*
     * If projects contain a searchable field then the client can hit both
     * indices (projects, datasets) with a single query. Right now the single
     * query fails because of the lack of a searchable field in the projects.
     * ADDED MANUALLY A SEARCHABLE FIELD IN THE RIVER. MAKES A PROJECT
     * SEARCHABLE BY DEFAULT. NEEDS REFACTORING
     */
    //hit the indices - execute the queries
    SearchRequestBuilder srb = client.prepareSearch(Settings.META_INDEX);
    srb = srb.setTypes(Settings.META_PROJECT_TYPE,
            Settings.META_DATASET_TYPE);
    srb = srb.setQuery(this.globalSearchQuery(searchTerm.toLowerCase()));
    srb = srb.addHighlightedField("name");
    logger.
            log(Level.INFO, "Global search Elastic query is: {0}", srb.
                    toString());
    ListenableActionFuture<SearchResponse> futureResponse = srb.execute();
    SearchResponse response = futureResponse.actionGet();

    if (response.status().getStatus() == 200) {
      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }

      this.clientShutdown(client);
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }
    logger.log(Level.WARNING, "Elasticsearch error code: {0}",
            response.status().getStatus());
    //something went wrong so throw an exception
    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  private Client getClient() throws AppException {
    final org.elasticsearch.common.settings.Settings settings
            = org.elasticsearch.common.settings.Settings.settingsBuilder()
                    .put("client.transport.sniff", true) //being able to retrieve other nodes 
                    .put("cluster.name", "hops").build();

    return TransportClient.builder().settings(settings).build()
            .addTransportAddress(new InetSocketTransportAddress(
                    new InetSocketAddress(getElasticIpAsString(),
                            this.settings.getElasticPort())));
  }

  /**
   * Searches for content inside a specific project. Hits 'project' index
   * <p/>
   * @param projectName
   * @param searchTerm
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("projectsearch/{projectId}/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response projectSearch(
          @PathParam("projectId") Integer projectId,
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    if (projectId == null || searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    Client client = getClient();
    //check if the index are up and running
    if (!this.indexExists(client, Settings.META_INDEX)) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    } else if (!this.typeExists(client, Settings.META_INDEX,
            Settings.META_INODE_TYPE)) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_TYPE_NOT_FOUND);
    }

    Project project = projectFacade.find(projectId);

    SearchRequestBuilder srb = client.prepareSearch(Settings.META_INDEX);
    srb = srb.setTypes(Settings.META_INODE_TYPE, Settings.META_DATASET_TYPE);
    srb = srb.setQuery(projectSearchQuery(project, searchTerm.toLowerCase()));
    srb = srb.addHighlightedField("name");

    logger.log(Level.INFO, "Project Elastic query is: {0}", srb.toString());
    ListenableActionFuture<SearchResponse> futureResponse = srb.execute();
    SearchResponse response = futureResponse.actionGet();

    if (response.status().getStatus() == 200) {

      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }

      this.clientShutdown(client);
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Searches for content inside a specific dataset. Hits 'dataset' index
   * <p/>
   * @param projectId
   * @param datasetName
   * @param searchTerm
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("datasetsearch/{projectId}/{datasetName}/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response datasetSearch(
          @PathParam("projectId") Integer projectId,
          @PathParam("datasetName") String datasetName,
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (datasetName == null || searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    Client client = getClient();
    //check if the indices are up and running
    if (!this.indexExists(client, Settings.META_INDEX)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    } else if (!this.typeExists(client, Settings.META_INDEX,
            Settings.META_INODE_TYPE)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_TYPE_NOT_FOUND);
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

    Dataset dataset = datasetFacade.findByNameAndProjectId(project, dsName);
    final int datasetId = dataset.geInodeId();

    //hit the indices - execute the queries
    SearchRequestBuilder srb = client.prepareSearch(Settings.META_INDEX);
    srb = srb.setTypes(Settings.META_INODE_TYPE);
    srb = srb.setQuery(this.datasetSearchQuery(datasetId, searchTerm.toLowerCase()));
    //TODO: https://github.com/elastic/elasticsearch/issues/14999 
    //srb = srb.addHighlightedField("name");
    srb = srb.setRouting(String.valueOf(project.getId()));

    logger.log(Level.INFO, "Dataset Elastic query is: {0}", srb.toString());
    ListenableActionFuture<SearchResponse> futureResponse = srb.execute();
    SearchResponse response = futureResponse.actionGet();

    if (response.status().getStatus() == 200) {
      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }

      this.clientShutdown(client);
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {};
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }
    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Global search on datasets and projects.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder globalSearchQuery(String searchTerm) {
    //FIXME: consider metadata search as well
    QueryBuilder nameDescQuery = getAllQuery(searchTerm);

    QueryBuilder query = boolQuery()
            .must(nameDescQuery);

    return query;
  }

  /**
   * Project specific search.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder projectSearchQuery(Project project, String searchTerm) {
    
    List<Dataset> datasets = datasetFacade.findByProject(project);
    BoolQueryBuilder datasetsQuery = boolQuery();
    for(Dataset dataset: datasets){
      datasetsQuery.should(matchQuery(Settings.META_DATASET_ID_FIELD, dataset.geInodeId()));
      datasetsQuery.should(matchQuery(Settings.META_ID, dataset.geInodeId()));
    }
    
    QueryBuilder searchQuery = getAllQuery(searchTerm);
    return boolQuery().must(datasetsQuery).must(searchQuery);
  }

  /**
   * Dataset specific search.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder datasetSearchQuery(int datasetId, String searchTerm) {
    //FIXME: consider metadata search as well
    QueryBuilder dataset = matchQuery(Settings.META_DATASET_ID_FIELD, datasetId);
    
    QueryBuilder query = getAllQuery(searchTerm);

    QueryBuilder cq = boolQuery()
            .must(dataset)
            .must(query);
    return cq;
  }

  private QueryBuilder getAllQuery(String searchTerm) {
    return boolQuery().should(wildcardQuery("_all", String.format("*%s*", searchTerm)));
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
            metadataQuery);

    return nestedQuery;
  }

  /**
   * Checks if a given index exists in elastic
   * <p/>
   * @param client
   * @param indexName
   * @return
   */
  private boolean indexExists(Client client, String indexName) {
    AdminClient admin = client.admin();
    IndicesAdminClient indices = admin.indices();

    IndicesExistsRequestBuilder indicesExistsRequestBuilder = indices.
            prepareExists(indexName);

    IndicesExistsResponse response = indicesExistsRequestBuilder
            .execute()
            .actionGet();

    return response.isExists();
  }

  /**
   * Checks if a given data type exists. It is a given that the index exists
   * <p/>
   * @param client
   * @param typeName
   * @return
   */
  private boolean typeExists(Client client, String indexName, String typeName) {
    AdminClient admin = client.admin();
    IndicesAdminClient indices = admin.indices();

    ActionFuture<TypesExistsResponse> action = indices.typesExists(
            new TypesExistsRequest(
                    new String[]{indexName}, typeName));

    TypesExistsResponse response = action.actionGet();

    return response.isExists();
  }

  /**
   * Shuts down the client and clears the cache
   * <p/>
   * @param client
   */
  private void clientShutdown(Client client) {

    client.admin().indices().clearCache(new ClearIndicesCacheRequest(
            Settings.META_INDEX));

    client.close();
  }

  /**
   * Boots up a previously closed index
   */
  private void bootIndices(Client client) {

    client.admin().indices().open(new OpenIndexRequest(
            Settings.META_INDEX));
  }

  private String getElasticIpAsString() throws AppException {
    String addr = settings.getElasticIp();

    // Validate the ip address pulled from the variables
    if (Ip.validIp(addr) == false) {
      try {
        InetAddress.getByName(addr);
      } catch (UnknownHostException ex) {
        logger.log(Level.SEVERE, ResponseMessages.ELASTIC_SERVER_NOT_AVAILABLE);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_AVAILABLE);

      }
    }

    return addr;
  }

}
