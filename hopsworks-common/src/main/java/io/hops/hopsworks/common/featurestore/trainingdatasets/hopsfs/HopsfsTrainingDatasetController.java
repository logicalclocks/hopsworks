/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.featurestore.trainingdatasets.hopsfs;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.featurestore.storageconnectors.hopsfs.FeaturestoreHopsfsConnectorDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.NamenodeTags;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.nio.file.Paths;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the hopsfs_training_dataset table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HopsfsTrainingDatasetController {

  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private Settings settings;

  /**
   * Converts a Hopsfs Training Dataset entity into a DTO representation
   *
   * @param trainingDataset the entity to convert
   * @return the converted DTO representation
   */
  public TrainingDatasetDTO convertHopsfsTrainingDatasetToDTO(TrainingDatasetDTO trainingDatasetDTO,
                                                              TrainingDataset trainingDataset)
      throws ServiceException {

    Service namenodeService;
    try {
      namenodeService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
          HopsworksService.NAMENODE.getNameWithTag(NamenodeTags.rpc));
    } catch (ServiceDiscoveryException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_NOT_FOUND,
          Level.SEVERE, "Could not find namenode service", e.getMessage(), e);
    }

    String hopsfsPath;
    if (Strings.isNullOrEmpty(trainingDataset.getConnectorPath())) {
      hopsfsPath =
              Utils.getDatasetPath(trainingDataset.getFeaturestoreConnector().getHopsfsConnector().getHopsfsDataset(),
                      settings).toString();
    } else {
      hopsfsPath = Paths.get(
              Utils.getDatasetPath(trainingDataset.getFeaturestoreConnector().getHopsfsConnector().getHopsfsDataset(),
                      settings).toString(),
              trainingDataset.getConnectorPath()).toString();
    }

    trainingDatasetDTO.setLocation(new Path(DistributedFileSystemOps.HOPSFS_SCHEME,
        namenodeService.getAddress() + ":" + namenodeService.getPort(), hopsfsPath).toString());

    FeaturestoreHopsfsConnectorDTO hopsfsConnectorDTO =
        new FeaturestoreHopsfsConnectorDTO(trainingDataset.getFeaturestoreConnector());
    trainingDatasetDTO.setStorageConnector(hopsfsConnectorDTO);
    return trainingDatasetDTO;
  }
}
