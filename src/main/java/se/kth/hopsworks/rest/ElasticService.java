package se.kth.hopsworks.rest;

import java.util.LinkedList;
import java.util.List;
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
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.FilterBuilder;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.hasParentQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import org.elasticsearch.search.SearchHit;
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
   * <p>
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

    System.out.println("GLOBAL SEARCH");

    String datasetIndex = "datasets";
    String projectIndex = "project";

    String datasetType = "dataset";
    String projectType = "parent";

    //some necessary client settings
    final Settings settings = ImmutableSettings.settingsBuilder()
            .put("client.transport.sniff", true) //being able to retrieve other nodes 
            .put("cluster.name", "hopsworks").build();

    //initialize the client
    Client client
            = new TransportClient(settings)
            .addTransportAddress(new InetSocketTransportAddress("193.10.66.125",
                            9300));

    //build the project query predicates
    QueryBuilder namePMatch = prefixQuery("name", searchTerm);
    FilterBuilder projectFilter = boolFilter()
            .should(termFilter("name", searchTerm))
            .should(termFilter("EXTENDED_METADATA", searchTerm));

    //build the dataset query predicates
    QueryBuilder nameDSMatch = prefixQuery("name", searchTerm);
    FilterBuilder datasetFilter = boolFilter()
            .should(termFilter("name", searchTerm))
            .should(termFilter("EXTENDED_METADATA", searchTerm))
            .should(termFilter("description", searchTerm));

    //build the actual queries
    QueryBuilder projectsQuery = QueryBuilders.filteredQuery(namePMatch,
            projectFilter);
    QueryBuilder datasetsQuery = QueryBuilders.filteredQuery(nameDSMatch,
            datasetFilter);

    //execute the queries
    SearchResponse response = client.prepareSearch(projectIndex, datasetIndex).
            setTypes(projectType, datasetType)
            .setQuery(projectsQuery)
            .setQuery(datasetsQuery)
            .addHighlightedField("name")
            .execute().actionGet();

    if (response.status().getStatus() == 200) {
      System.out.println("Matched number of documents: " + response.getHits().
              totalHits());
      System.out.println("Maximum score: " + response.getHits().maxScore());

      List<ElasticHit> elasticHits = new LinkedList<>();
      if (response.getHits().getHits().length > 0) {
        SearchHit[] hits = response.getHits().getHits();

        for (SearchHit hit : hits) {
          elasticHits.add(new ElasticHit(hit));
        }
      }

      client.close();
      GenericEntity<List<ElasticHit>> searchResults
              = new GenericEntity<List<ElasticHit>>(elasticHits) {
              };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(searchResults).build();
    }

    //something went wrong so just throw an exception
    client.close();
    throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), ResponseMessages.ELASTIC_SERVER_NOT_FOUND);
  }

  /**
   * Searches for content inside a specific project. Hits 'projects' index
   * <p>
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

    String index = "project";
    String type = "child";

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

    SearchResponse response = client.prepareSearch(index).setTypes(type)
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
}
