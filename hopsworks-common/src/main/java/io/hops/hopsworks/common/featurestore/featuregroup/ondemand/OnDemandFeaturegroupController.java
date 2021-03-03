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

package io.hops.hopsworks.common.featurestore.featuregroup.ondemand;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandOption;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the on_demand_feature_group table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OnDemandFeaturegroupController {
  @EJB
  private OnDemandFeaturegroupFacade onDemandFeaturegroupFacade;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private DistributedFsService distributedFsService;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private InodeController inodeController;

  /**
   * Persists an on demand feature group
   *
   *
   * @param project
   * @param user
   * @param featurestore
   * @param onDemandFeaturegroupDTO the user input data to use when creating the feature group
   * @return the created entity
   */
  public OnDemandFeaturegroup createOnDemandFeaturegroup(Featurestore featurestore,
                                                         OnDemandFeaturegroupDTO onDemandFeaturegroupDTO,
                                                         Project project, Users user)
      throws FeaturestoreException {
    //Verify User Input specific for on demand feature groups
    FeaturestoreConnector connector = getStorageConnector(onDemandFeaturegroupDTO.getStorageConnector().getId());

    // We allow users to read an entire S3 bucket for instance and they don't need to provide us with a query
    // However if you are running against a JDBC database, you need to provide a query
    boolean isJDBCType = (connector.getConnectorType() == FeaturestoreConnectorType.JDBC ||
        connector.getConnectorType() == FeaturestoreConnectorType.REDSHIFT ||
        connector.getConnectorType() == FeaturestoreConnectorType.SNOWFLAKE);
    if (Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getQuery()) && isJDBCType) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY, Level.FINE,
          "SQL Query cannot be empty");
    } else if (!Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getQuery()) && !isJDBCType) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY, Level.FINE,
          "SQL query not supported when specifying " + connector.getConnectorType() + " storage connectors");
    } else if (onDemandFeaturegroupDTO.getDataFormat() == null && !isJDBCType) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_ON_DEMAND_DATA_FORMAT, Level.FINE,
          "Data format required when specifying " + connector.getConnectorType() + " storage connectors");
    }

    //Persist on-demand featuregroup
    OnDemandFeaturegroup onDemandFeaturegroup = new OnDemandFeaturegroup();
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    onDemandFeaturegroup.setFeaturestoreConnector(connector);
    onDemandFeaturegroup.setQuery(onDemandFeaturegroupDTO.getQuery());
    onDemandFeaturegroup.setFeatures(convertOnDemandFeatures(onDemandFeaturegroupDTO, onDemandFeaturegroup));
    onDemandFeaturegroup.setInode(createFile(project, user, featurestore, onDemandFeaturegroupDTO));
    onDemandFeaturegroup.setDataFormat(onDemandFeaturegroupDTO.getDataFormat());
    onDemandFeaturegroup.setPath(onDemandFeaturegroupDTO.getPath());

    if (onDemandFeaturegroupDTO.getOptions() != null) {
      onDemandFeaturegroup.setOptions(
          onDemandFeaturegroupDTO.getOptions().stream()
              .map(o -> new OnDemandOption(onDemandFeaturegroup, o.getName(), o.getValue()))
              .collect(Collectors.toList()));
    }


    onDemandFeaturegroupFacade.persist(onDemandFeaturegroup);
    return onDemandFeaturegroup;
  }

  /**
   * Updates metadata of an on demand feature group in the feature store
   *
   * @param onDemandFeaturegroup the on-demand feature group to update
   * @param onDemandFeaturegroupDTO the metadata DTO
   */
  public void updateOnDemandFeaturegroupMetadata(OnDemandFeaturegroup onDemandFeaturegroup,
                                                 OnDemandFeaturegroupDTO onDemandFeaturegroupDTO) {
    // Update metadata in entity
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());

    // finally merge in database
    onDemandFeaturegroupFacade.updateMetadata(onDemandFeaturegroup);
  }

  /**
   * Removes a on demand feature group
   * @return the deleted entity
   */
  public void removeOnDemandFeaturegroup(Featurestore featurestore, Featuregroup featuregroup,
                                         Project project, Users user) throws FeaturestoreException {
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = null;

    // this is here for old feature groups that don't have a file
    onDemandFeaturegroupFacade.remove(featuregroup.getOnDemandFeaturegroup());

    try {
      udfso = distributedFsService.getDfsOps(username);
      udfso.rm(getFilePath(featurestore, featuregroup.getName(), featuregroup.getVersion()), false);
    } catch (IOException | URISyntaxException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_DELETE_ON_DEMAND_FEATUREGROUP,
          Level.SEVERE, "Error removing the placeholder file", e.getMessage(), e);
    } finally {
      distributedFsService.closeDfsClient(udfso);
    }
  }

  private List<OnDemandFeature> convertOnDemandFeatures(OnDemandFeaturegroupDTO onDemandFeaturegroupDTO,
                                                        OnDemandFeaturegroup onDemandFeaturegroup) {
    if (onDemandFeaturegroupDTO.getFeatures().isEmpty()) {
      throw new IllegalArgumentException("No features were provided for on demand feature group");
    }
    return onDemandFeaturegroupDTO.getFeatures().stream()
        .map(f ->
            new OnDemandFeature(onDemandFeaturegroup, f.getName(), f.getType(), f.getDescription(), f.getPrimary()))
        .collect(Collectors.toList());
  }

  private Inode createFile(Project project, Users user, Featurestore featurestore,
                           OnDemandFeaturegroupDTO onDemandFeaturegroupDTO) throws FeaturestoreException {
    String username = hdfsUsersController.getHdfsUserName(project, user);

    Path path = null;
    DistributedFileSystemOps udfso = null;
    try {
      path = getFilePath(featurestore, onDemandFeaturegroupDTO.getName(), onDemandFeaturegroupDTO.getVersion());

      udfso = distributedFsService.getDfsOps(username);
      udfso.touchz(path);
    } catch (IOException | URISyntaxException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_ON_DEMAND_FEATUREGROUP,
          Level.SEVERE, "Error creating the placeholder file", e.getMessage(), e);
    } finally {
      distributedFsService.closeDfsClient(udfso);
    }

    return inodeController.getInodeAtPath(path.toString());
  }

  private Path getFilePath(Featurestore featurestore, String name, Integer version) throws URISyntaxException {
    // for compatibility reason here we need to remove the authority
    return new Path (new URI(featurestoreFacade.getHiveDbHdfsPath(featurestore.getHiveDbId())).getPath(),
        name + "_" + version);
  }

  private FeaturestoreConnector getStorageConnector(Integer connectorId) throws FeaturestoreException {
    if(connectorId == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }

    return featurestoreConnectorFacade.findById(connectorId)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CONNECTOR_NOT_FOUND, Level.FINE,
        "Connector with id: " + connectorId + " was not found"));
  }
}
