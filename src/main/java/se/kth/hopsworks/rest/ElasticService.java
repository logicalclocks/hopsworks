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
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
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
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
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

    client.admin().indices().prepareExists(Constants.META_PROJECT_INDEX);

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
//      logger.log(Level.INFO, "Matched number of documents: {0}", response.
//              getHits().
//              totalHits());

      //construct the response
      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit, hit.getSource(), "parent"));
        }
      }

      client.close();
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {
              };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    //something went wrong so throw an exception
    client.close();
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

    QueryBuilder hasParentPart = hasParentQuery("parent", matchQuery("name",
            projectName));

    FilterBuilder multiMatchPart = boolFilter()
            .should(termFilter("name", fuzzyQuery("name", searchTerm)))
            .should(termFilter("name", QueryBuilders.prefixQuery("name",
                                    searchTerm)))
            .should(termFilter("EXTENDED_METADATA", fuzzyQuery(
                                    "EXTENDED_METADATA", searchTerm)));

    QueryBuilder query = QueryBuilders.filteredQuery(hasParentPart,
            multiMatchPart);

    SearchResponse response = client.prepareSearch(Constants.META_PROJECT_INDEX)
            .setTypes(Constants.META_PROJECT_CHILD_TYPE)
            .setQuery(query).addHighlightedField("name")
            .execute().actionGet();

    if (response.status().getStatus() == 200) {
      System.out.println("Matched number of documents: " + response.getHits().
              totalHits());
      System.out.println("Maximum score: " + response.getHits().maxScore());

      for (SearchHit hit : response.getHits().getHits()) {
        System.out.println("hit: " + hit.getIndex() + ":" + hit.getType()
                + ":" + hit.getId());
      }

      client.close();

      JsonResponse json = new JsonResponse();
      json.setSuccessMessage("Elastic responded ok.");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();
    }

    client.close();

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
   * Performs a matchprasequery on the description field of the dataset. Name
   * and extended_metadata fields are taken care of by the project query
   * <p/>
   * @param searchTerm
   * @return
   */
  private QueryBuilder getDatasetComboQuery(String searchTerm) {

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
            .should(phraseQuery)
            .should(nameQuery)
            .should(metadataPhraseQuery)
            .should(metadataTermsQuery);

    return datasetQuery;
  }

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
}
