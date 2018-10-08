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

package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetPermissions;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.HopsSecurityException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.exception.DelaException;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaDatasetController {

  private static final Logger LOGGER = Logger.getLogger(DelaDatasetController.class.getName());

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
    throws DelaException {
    Dataset dataset;
    try {
      dataset = createDataset(user, project, name, "");
    } catch (DatasetException | HopsSecurityException e) {
      throw new DelaException(RESTCodes.DelaErrorCode.THIRD_PARTY_ERROR, Level.SEVERE, DelaException.Source.LOCAL, null,
        e.getMessage(), e);
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

  public void delete(Project project, Dataset dataset) throws DelaException {
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
        throw new DelaException(RESTCodes.DelaErrorCode.DATASET_DELETE_ERROR, Level.SEVERE, DelaException.Source.LOCAL);
      }
    } catch (IOException ex) {
      throw new DelaException(RESTCodes.DelaErrorCode.DATASET_DELETE_ERROR, Level.SEVERE, DelaException.Source.LOCAL,
        null, ex.getMessage(), ex);
    }
  }

  public Dataset createDataset(Users user, Project project, String name, String description)
    throws DatasetException, HopsSecurityException {
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
  
  public Optional<Dataset> isPublicDatasetLocal(String publicDsId) {
    return datasetFacade.findByPublicDsId(publicDsId);
  }
  
  public FilePreviewDTO getLocalReadmeForPublicDataset(Dataset dataset) throws IOException, IllegalAccessException {
    if(!dataset.isPublicDs()) {
      throw new IllegalAccessException("dataset is not public");
    }
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    Path readmePath = new Path(datasetPath, Settings.README_FILE);
    FilePreviewDTO filePreviewDTO = datasetCtrl.getReadme(readmePath.toString(), dfs.getDfsOps());
    return filePreviewDTO;
  }
}
