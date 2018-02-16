/*
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
 *
 */

package io.hops.hopsworks.api.dela;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetDTO;
import io.hops.hopsworks.api.hopssite.dto.LocalDatasetHelper;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.dela.cluster.ClusterDatasetController;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/delacluster")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Dela Cluster Service",
  description = "Dela Cluster Service")
public class DelaClusterService {

  private final static Logger LOG = Logger.getLogger(DelaClusterService.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ClusterDatasetController clusterDatasetCtrl;

  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private DistributedFsService dfs;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response getPublicDatasets(@Context SecurityContext sc, @Context HttpServletRequest req) throws AppException {
    List<Dataset> clusterDatasets = clusterDatasetCtrl.getPublicDatasets();
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    List<LocalDatasetDTO> localDS;
    try {
      localDS = LocalDatasetHelper.parse(datasetCtrl, dfso, clusterDatasets);
    } finally {
      dfs.closeDfsClient(dfso);
    }
    GenericEntity<List<LocalDatasetDTO>> datasets = new GenericEntity<List<LocalDatasetDTO>>(localDS) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(datasets).build();
  }
}
