package se.kth.hopsworks.rest;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
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
import org.apache.lucene.search.PhraseQuery;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.FilterBuilder;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.prefixFilter;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.hasParentQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import org.elasticsearch.search.SearchHit;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author vangelis
 */
@Path("/elastic")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticService {

  private final static Logger logger = Logger.getLogger(ElasticService.class.
          getName());

  @EJB
  private NoCacheResponse noCacheResponse;

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
    final Settings settings = ImmutableSettings.settingsBuilder()
            .put("client.transport.sniff", true) //being able to inspect other nodes 
            .put("cluster.name", "hopsworks")
            .build();

    //initialize the client
    Client client
            = new TransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress("193.10.66.125",
                            9300));

    //check if the indices are up and running
    if (!this.indexExists(client, Constants.META_PROJECT_INDEX) || !this.
            indexExists(client, Constants.META_DATASET_INDEX)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    }

    //hit the indices - execute the queries
    SearchResponse response = client.prepareSearch(Constants.META_PROJECT_INDEX,
            Constants.META_DATASET_INDEX).
            setTypes(Constants.META_PROJECT_PARENT_TYPE,
                    Constants.META_DATASET_TYPE)
            .setQuery(this.getProjectDatasetQueryCombo(searchTerm))
            //.setQuery(this.getDatasetComboQuery(searchTerm))
            .addHighlightedField("name")
            .execute().actionGet();

    if (response.status().getStatus() == 200) {
      //logger.log(Level.INFO, "Matched number of documents: {0}", response.
      //getHits().
      //totalHits());

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
              = new GenericEntity<List<ElasticHit>>(elasticHits) {
              };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    //something went wrong so throw an exception
    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Searches for content inside a specific project. Hits 'projects' index
   * <p/>
   * @param projectName
   * @param searchTerm
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("projectsearch/{projectName}/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response projectSearch(
          @PathParam("projectName") String projectName,
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectName == null || searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    final Settings settings = ImmutableSettings.settingsBuilder()
            .put("client.transport.sniff", true) //being able to retrieve other nodes 
            .put("cluster.name", "hopsworks").build();

    Client client
            = new TransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress("193.10.66.125",
                            9300));
    
    //check if the indices are up and running
    if (!this.indexExists(client, Constants.META_PROJECT_INDEX)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_INDEX_NOT_FOUND);
    } else if (!this.typeExists(client, Constants.META_PROJECT_INDEX,
            Constants.META_PROJECT_CHILD_TYPE)) {

      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ResponseMessages.ELASTIC_TYPE_NOT_FOUND);
    }

    //Query
    //filter results by parent
    QueryBuilder hasParentPart = hasParentQuery(
            Constants.META_PROJECT_PARENT_TYPE,
            matchQuery(Constants.META_NAME_FIELD, projectName));

    QueryBuilder totalQuery = this.getChildComboQuery(searchTerm);

    QueryBuilder intersection = boolQuery()
            .must(hasParentPart)
            .should(totalQuery);

    //hit the indices - execute the queries
    SearchResponse response
            = client.prepareSearch(Constants.META_PROJECT_INDEX).
            setTypes(Constants.META_PROJECT_CHILD_TYPE)
            .setQuery(intersection)
            .addHighlightedField("name")
            .execute().actionGet();

    if (response.status().getStatus() == 200) {
      logger.log(Level.INFO, "Matched number of documents: {0}", response.
              getHits().
              totalHits());

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
              = new GenericEntity<List<ElasticHit>>(elasticHits) {
              };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    this.clientShutdown(client);
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Combines a prefix query with a filtered query (i.e. 'namePMatch'
   * and 'projectFilter')
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getProjectComboQuery(String searchTerm) {
    //build the project query predicates
    QueryBuilder namePMatch = prefixQuery("name", searchTerm);
    FilterBuilder projectFilter = boolFilter()
            .should(prefixFilter("name", searchTerm))
            .should(prefixFilter("EXTENDED_METADATA", searchTerm));

    QueryBuilder projectsQuery = QueryBuilders.filteredQuery(namePMatch,
            projectFilter);

    return projectsQuery;
  }

  /**
   * A boolean query with a 'must' and two 'should' filter predicates. It uses a
   * prefix and fuzzy matches respectively
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getProjectBoolQuery(String searchTerm) {

    //the same as the above only shorter
    QueryBuilder boolQuery = boolQuery()
            .must(prefixQuery("name", searchTerm))
            .should(fuzzyQuery("name", searchTerm))
            .should(fuzzyQuery("EXTENDED_METADATA", searchTerm));

    return boolQuery;
  }

  /**
   * Applies several match filters on the description, name and
   * extended_metadata
   * fields of the dataset.
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getDatasetComboQuery(String searchTerm) {

    //look for active records (not deleted - operation = 0)
    QueryBuilder operationQuery = matchQuery("operation", 0);

    //a phrase query to match the dataset description
    QueryBuilder phraseQuery = matchPhraseQuery(
            Constants.META_DESCRIPTION_FIELD, searchTerm);

    //build the dataset query predicates
    QueryBuilder nameQuery = prefixQuery(Constants.META_NAME_FIELD, searchTerm);

    //apply phrase filter on user metadata
    QueryBuilder metadataPhraseQuery = matchPhraseQuery(
            Constants.META_DATA_FIELD,
            searchTerm);

    //apply terms filter on user metadata as well
    QueryBuilder metadataTermsQuery = termsQuery(Constants.META_DATA_FIELD,
            searchTerm);

    //aggregate the results
    QueryBuilder datasetQuery = QueryBuilders.boolQuery()
            //.must(operationQuery)
            .should(phraseQuery)
            .should(nameQuery)
            .should(metadataPhraseQuery)
            .should(metadataTermsQuery);

    return datasetQuery;
  }

  /**
   * Gathers the query filters applied on projects and datasets and returns a
   * query combining all these
   * <p>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getProjectDatasetQueryCombo(String searchTerm) {

    //get project and dataset results
    QueryBuilder projects = this.getProjectComboQuery(searchTerm);
    QueryBuilder datasets = this.getDatasetComboQuery(searchTerm);

    //combine the results - take the union of the two sets
    QueryBuilder union = QueryBuilders.boolQuery()
            .should(projects)
            .should(datasets);

    return union;
  }

  /**
   * Performs a matchprasequery on the description field of the dataset. Name
   * and extended_metadata fields are taken care of by the project query
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getChildComboQuery(String searchTerm) {

    //look for active records (not deleted - operation = 0)
    QueryBuilder operationQuery = matchQuery("operation", 0);

    //TODO: add query on the description field, as soon as there is one
    //build the dataset query predicates
    QueryBuilder nameQuery = prefixQuery(Constants.META_NAME_FIELD, searchTerm);
    
    //apply fuzzy filter on the name
    QueryBuilder nameFuzzy = fuzzyQuery(Constants.META_NAME_FIELD, searchTerm);
    
    //apply phrase filter on the name. Allow the user to keep typing and constantly getting results
    QueryBuilder namePhrase = matchPhraseQuery(Constants.META_NAME_FIELD, searchTerm);

    //apply phrase filter on user metadata
    QueryBuilder metadataPhraseQuery = matchPhraseQuery(
            Constants.META_DATA_FIELD,
            searchTerm);

    //apply terms filter on user metadata
    QueryBuilder metadataTermsQuery = termsQuery(Constants.META_DATA_FIELD,
            searchTerm);

    //apply prefix filter on user metadata
    QueryBuilder metadataPrefixQuery = prefixQuery(Constants.META_DATA_FIELD,
            searchTerm);

    //aggregate the results
    QueryBuilder datasetQuery = QueryBuilders.boolQuery()
            //.must(operationQuery)
            .should(nameQuery)
            .should(nameFuzzy)
            .should(namePhrase);
            //.should(metadataPhraseQuery)
            //.should(metadataTermsQuery)
            //.should(metadataPrefixQuery);

    return datasetQuery;
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
            Constants.META_PROJECT_INDEX, Constants.META_DATASET_INDEX));

    client.close();
  }
  
  /**
   * Boots up a previously closed index
   */
  private void bootIndices(Client client){
    
    client.admin().indices().open(new OpenIndexRequest(Constants.META_PROJECT_INDEX, Constants.META_DATASET_INDEX));
  }
}
