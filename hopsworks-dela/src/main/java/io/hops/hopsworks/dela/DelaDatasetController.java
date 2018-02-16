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

package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import org.apache.hadoop.fs.Path;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaDatasetController {

  private Logger logger = Logger.getLogger(DelaDatasetController.class.getName());

  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;

  public Dataset uploadToHops(Dataset dataset, String publicDSId) {
    dataset.setPublicDsState(Dataset.SharedState.HOPS);
    dataset.setPublicDsId(publicDSId);
    dataset.setEditable(DatasetPermissions.OWNER_ONLY);
    datasetFacade.merge(dataset);
    datasetCtrl.logDataset(dataset, OperationType.Update);
    return dataset;
  }
  
  public Dataset unshareFromHops(Dataset dataset) {
    dataset.setPublicDsState(Dataset.SharedState.PRIVATE);
    dataset.setPublicDsId(null);
    dataset.setEditable(DatasetPermissions.GROUP_WRITABLE_SB);
    datasetFacade.merge(dataset);
    datasetCtrl.logDataset(dataset, OperationType.Update);
    return dataset;
  }
  
  public Dataset download(Project project, Users user, String publicDSId, String name)
    throws ThirdPartyException {
    Dataset dataset;
    try {
      dataset = createDataset(user, project, name, "");
    } catch (IOException | AppException e) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), e.getMessage(),
        ThirdPartyException.Source.LOCAL, "");
    }
    dataset.setPublicDsState(Dataset.SharedState.HOPS);
    dataset.setPublicDsId(publicDSId);
    dataset.setEditable(DatasetPermissions.OWNER_ONLY);
    datasetFacade.merge(dataset);
    datasetCtrl.logDataset(dataset, OperationType.Update);
    return dataset;
  }

  public Dataset updateDescription(Dataset dataset, String description) {
    dataset.setDescription(description);
    datasetFacade.merge(dataset);
    datasetCtrl.logDataset(dataset, OperationType.Update);
    return dataset;
  }

  public void delete(Project project, Dataset dataset) throws ThirdPartyException {
    if (dataset.isShared()) {
      //remove the entry in the table that represents shared ds
      //but leave the dataset in hdfs b/c the user does not have the right to delete it.
      hdfsUsersBean.unShareDataset(project, dataset);
      return;
    }
    try {
      Path path = datasetCtrl.getDatasetPath(dataset);
      boolean result = datasetCtrl.deleteDatasetDir(dataset, path, dfs.getDfsOps());
      if (!result) {
        throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "dataset delete",
          ThirdPartyException.Source.LOCAL, "exception");
      }
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "dataset delete",
        ThirdPartyException.Source.LOCAL, "exception");
    }
  }

  public Dataset createDataset(Users user, Project project, String name, String description)
    throws IOException, AppException {
    int templateId = -1;
    boolean defaultDataset = false;
    boolean searchable = true;

    datasetCtrl.createDataset(user, project, name, description, templateId, searchable, defaultDataset, 
      dfs.getDfsOps());
    Dataset dataset = datasetFacade.findByNameAndProjectId(project, name);
    return dataset;
  }
  
  public List<Dataset> getLocalPublicDatasets() {
    return datasetFacade.findAllDatasetsByState(Dataset.SharedState.HOPS.state, false);
  }
}
