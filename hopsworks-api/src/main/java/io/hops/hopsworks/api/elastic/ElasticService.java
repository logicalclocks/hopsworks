package io.hops.hopsworks.api.elastic;

import io.hops.hopsworks.common.elastic.ElasticHit;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.util.List;
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
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.AppException;

@Path("/elastic")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticService {

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private ElasticController elasticController;

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

    GenericEntity<List<ElasticHit>> searchResults
        = new GenericEntity<List<ElasticHit>>(elasticController.globalSearch(searchTerm)) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(searchResults).build();
  }

  /**
   * Searches for content inside a specific project. Hits 'project' index
   * <p/>
   * @param projectId
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

    GenericEntity<List<ElasticHit>> searchResults
        = new GenericEntity<List<ElasticHit>>(elasticController.projectSearch(projectId, searchTerm)) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(searchResults).build();
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

    GenericEntity<List<ElasticHit>> searchResults
        = new GenericEntity<List<ElasticHit>>(elasticController.datasetSearch(projectId, datasetName, searchTerm)) {};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
        entity(searchResults).build();
  }

}
