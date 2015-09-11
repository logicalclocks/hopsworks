package se.kth.hopsworks.rest;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.MultiSearchResponse.Item;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
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

//    SearchResponse response = client.prepareSearch("project", "datasets")
//            .setTypes("parent", "dataset")
//            .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
//            .setQuery(QueryBuilders.termQuery("multi", "test")) // Query
//            .setPostFilter(FilterBuilders.rangeFilter("age").from(12).to(18)) // Filter
//            .setFrom(0).setSize(60).setExplain(true)
//            .execute()
//            .actionGet();
//
//    SearchHit[] results = response.getHits().getHits();

                
    Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "hopsworks").build();
    TransportClient transportClient = new TransportClient(settings);
            
    Client client = transportClient
                        .addTransportAddress(new InetSocketTransportAddress("193.10.66.125", 9300));

    SearchRequestBuilder srb1 = client.prepareSearch()
            .setQuery(QueryBuilders.filteredQuery(
                            QueryBuilders.boolQuery()
                            .should(QueryBuilders.fuzzyQuery("name", searchTerm)),
                            null));

    SearchRequestBuilder srb2 = client.prepareSearch()
            .setQuery(QueryBuilders.filteredQuery(
                            QueryBuilders.boolQuery()
                            .should(QueryBuilders.
                                    fuzzyQuery("EXTENDED_METADATA", searchTerm)),
                            null));

    MultiSearchResponse sr = client.prepareMultiSearch()
            .add(srb1)
            .add(srb2)
            .execute().actionGet();

    Item responses[] = sr.getResponses();
    System.out.println("RESULTS " + responses.length);

    for (Item item : responses) {
      if (item.getResponse() != null) {
        SearchHit[] results = item.getResponse().getHits().getHits();
        
        System.out.println("found " + results.length);
        for (SearchHit shit : results) {
          System.out.println("found " + shit);
          System.out.println(shit.toString());
        }
      }else{
        System.out.println("nothing found");
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            responses).build();
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
  public Response detachTemplateFromInode(
          @PathParam("projectName") String projectName,
          @PathParam("searchTerm") String searchTerm,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectName == null || searchTerm == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("Empty response for now.");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
}
