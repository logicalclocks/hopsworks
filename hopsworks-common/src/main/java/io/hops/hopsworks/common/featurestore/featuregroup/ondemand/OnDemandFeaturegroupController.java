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
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
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
  private FeaturestoreJdbcConnectorFacade featurestoreJdbcConnectorFacade;
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
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
      getJdbcStorageConnector(onDemandFeaturegroupDTO.getStorageConnector().getId());

    if (Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getQuery())){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY,
          Level.FINE, "SQL Query cannot be empty");
    }

    // Create inode on the file system

    //Persist on-demand featuregroup
    OnDemandFeaturegroup onDemandFeaturegroup = new OnDemandFeaturegroup();
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    onDemandFeaturegroup.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);
    onDemandFeaturegroup.setQuery(onDemandFeaturegroupDTO.getQuery());
    onDemandFeaturegroup.setFeatures(convertOnDemandFeatures(onDemandFeaturegroupDTO, onDemandFeaturegroup));
    onDemandFeaturegroup.setInode(createFile(project, user, featurestore, onDemandFeaturegroupDTO));

    onDemandFeaturegroupFacade.persist(onDemandFeaturegroup);
    return onDemandFeaturegroup;
  }

  /**
   * Updates metadata of an on demand feature group in the feature store
   *
   * @param onDemandFeaturegroup the on-demand feature group to update
   * @param onDemandFeaturegroupDTO the metadata DTO
   * @throws FeaturestoreException
   */
  public void updateOnDemandFeaturegroupMetadata(OnDemandFeaturegroup onDemandFeaturegroup,
    OnDemandFeaturegroupDTO onDemandFeaturegroupDTO) throws FeaturestoreException {

    // Verify User Input specific for on demand feature groups
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
        getJdbcStorageConnector(onDemandFeaturegroupDTO.getStorageConnector().getId());

    // Update metadata in entity
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    onDemandFeaturegroup.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);

    // finally persist in database
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

  /**
   * Verifies the user input JDBC Connector Id for an on-demand feature group
   *
   * @param jdbcConnectorId the JDBC connector id to verify
   * @returns the jdbc connector with the given id if it passed the validation
   * @throws FeaturestoreException
   */
  private FeaturestoreJdbcConnector getJdbcStorageConnector(Integer jdbcConnectorId)
    throws FeaturestoreException {
    if(jdbcConnectorId == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreJdbcConnector featurestoreJdbcConnector = featurestoreJdbcConnectorFacade.find(jdbcConnectorId);
    if(featurestoreJdbcConnector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND, Level.FINE,
        "JDBC connector with id: " + jdbcConnectorId + " was not found");
    }
    return featurestoreJdbcConnector;
  }
}
