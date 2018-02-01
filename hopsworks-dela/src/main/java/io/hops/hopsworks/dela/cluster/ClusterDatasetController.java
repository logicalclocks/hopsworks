/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.dela.cluster;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dataset.DatasetController;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ClusterDatasetController {

  private Logger logger = Logger.getLogger(ClusterDatasetController.class.getName());

  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private DatasetFacade datasetFacade;

  public Dataset shareWithCluster(Dataset dataset) {
    if (dataset.isPublicDs()) {
      return dataset;
    }

    for (Dataset d : datasetFacade.findByInode(dataset.getInode())) {
      d.setPublicDsState(Dataset.SharedState.CLUSTER);
      datasetFacade.merge(d);
      datasetCtrl.logDataset(d, OperationType.Update);
    }
    return dataset;
  }

  public Dataset unshareFromCluster(Dataset dataset) {
    if (!dataset.isPublicDs()) {
      return dataset;
    }

    for (Dataset d : datasetFacade.findByInode(dataset.getInode())) {
      d.setPublicDsState(Dataset.SharedState.PRIVATE);
      datasetFacade.merge(d);
      datasetCtrl.logDataset(d, OperationType.Update);
    }
    return dataset;
  }

  public List<Dataset> getPublicDatasets() {
    return datasetFacade.findAllDatasetsByState(Dataset.SharedState.CLUSTER.state, false);
  }
}
