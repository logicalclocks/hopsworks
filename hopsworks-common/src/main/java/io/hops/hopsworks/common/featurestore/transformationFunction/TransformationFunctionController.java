/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.featurestore.transformationFunction;

import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnectorType;
import io.hops.hopsworks.persistence.entity.featurestore.transformationFunction.TransformationFunction;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import static io.hops.hopsworks.common.hdfs.Utils.getFeatureStoreEntityName;

/**
 * Class controlling the interaction with the transformer_function table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)

public class TransformationFunctionController {
  @EJB
  private TransformationFunctionFacade transformationFunctionFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;

  private static final String TRANSFORMATIONFUNCTIONS_FOLDER = "transformation_functions";
  private static final String TRANSFORMATION_FUNCTION_FILE_TYPE = ".json";

  public TransformationFunctionController() {
  }

  // for testing
  public TransformationFunctionController(TransformationFunctionFacade transformationFunctionFacade) {
    this.transformationFunctionFacade = transformationFunctionFacade;
  }

  public TransformationFunction register(Users user, Project project, Featurestore featurestore,
                                         TransformationFunctionDTO transformationFunctionDTO)
      throws FeaturestoreException, IOException {
    verifyTransformationFunctionInput(transformationFunctionDTO);
    create(user, project, featurestore, transformationFunctionDTO);
    return transformationFunctionFacade.register(
        transformationFunctionDTO.getName(),
        transformationFunctionDTO.getOutputType(),
        transformationFunctionDTO.getVersion(),
        featurestore,
        new Date(),
        user);
  }

  public void registerBuiltInTransformationFunctions(Users user, Project project,
    Featurestore featurestore)
    throws FeaturestoreException, IOException {
    List<TransformationFunctionDTO> builtInTransformationFunctionDTOs =
      this.generateBuiltInTransformationFunctionDTOs(featurestore);
    for (TransformationFunctionDTO transformationFunctionDTO : builtInTransformationFunctionDTOs) {
      this.register(user, project, featurestore, transformationFunctionDTO);
    }
  }

  public List<TransformationFunctionDTO> generateBuiltInTransformationFunctionDTOs(Featurestore featurestore)
    throws FeaturestoreException {
    int version = 1;
    String sourceCode;
    String outputType;
    List<TransformationFunctionDTO> builtInTransformationFunctionDTOs =
      new ArrayList<>();
    for (String name : FeaturestoreConstants.BUILT_IN_TRANSFORMATION_FUNCTION_NAMES) {
      switch (name) {
        case "min_max_scaler":
          sourceCode = FeaturestoreConstants.BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_MIN_MAX_SCALER;
          outputType = "DOUBLE";
          break;
        case "label_encoder":
          sourceCode = FeaturestoreConstants.BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_LABEL_ENCODER;
          outputType = "INT";
          break;
        case "standard_scaler":
          sourceCode = FeaturestoreConstants.BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_STANDARD_SCALER;
          outputType = "DOUBLE";
          break;
        case "robust_scaler":
          sourceCode = FeaturestoreConstants.BUILT_IN_TRANSFORMATION_FUNCTION_SOURCE_CODE_ROBUST_SCALER;
          outputType = "DOUBLE";
          break;
        default:
          throw new FeaturestoreException(
            RESTCodes.FeaturestoreErrorCode.ERROR_REGISTER_BUILTIN_TRANSFORMATION_FUNCTION, Level.FINE, "Provided name"
            + name + "does not match any built-in transformation function source code. Add a case to " +
            "generateBuiltInTransformationFunctionDTOs or provide an existing name.");
      }

      TransformationFunctionDTO transformationFunctionDTO = new TransformationFunctionDTO(
        name,
        outputType,
        version,
        sourceCode,
        featurestore.getId()
      );
      builtInTransformationFunctionDTOs.add(transformationFunctionDTO);
    }
    return builtInTransformationFunctionDTOs;
  }



  public String readContent(Users user, Project project, TransformationFunction transformationFunction)
      throws FeaturestoreException {
    DistributedFileSystemOps udfso = null;
    String path = getFullPath(transformationFunction.getFeaturestore(), transformationFunction.getName(),
        transformationFunction.getVersion()) + "/" + transformationFunction.getName()
        + TRANSFORMATION_FUNCTION_FILE_TYPE;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      return udfso.cat(path);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRANSFORMATION_FUNCTION_READ_ERROR,
          Level.WARNING, e.getMessage(), e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  private String create(Users user, Project project, Featurestore featurestore,
                       TransformationFunctionDTO transformationFunctionDTO)
      throws IOException, FeaturestoreException {

    // if version not provided, get latest and increment
    if (transformationFunctionDTO.getVersion() == null) {
      // returns ordered list by desc version
      List<TransformationFunction> transformationFnPrevious =
          transformationFunctionFacade.findByNameAndFeaturestoreOrderedDescVersion(
              transformationFunctionDTO.getName(), featurestore, 0).getItems();
      if (transformationFnPrevious != null && !transformationFnPrevious.isEmpty()) {
        transformationFunctionDTO.setVersion(transformationFnPrevious.get(0).getVersion() + 1);
      } else {
        transformationFunctionDTO.setVersion(1);
      }
    }

    // Check that transformation function doesn't already exists
    if (transformationFunctionFacade.findByNameVersionAndFeaturestore(
        transformationFunctionDTO.getName(),
        transformationFunctionDTO.getVersion(), featurestore).isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRANSFORMATION_FUNCTION_ALREADY_EXISTS,
          Level.FINE, "Transformation function: " + transformationFunctionDTO.getName() + ", version: " +
              transformationFunctionDTO.getVersion());
    }

    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));
      // Create the directory
      Path dirPath = getFullPath(featurestore, transformationFunctionDTO.getName(),
          transformationFunctionDTO.getVersion());
      if (!udfso.isDir(dirPath.toString())) {
        udfso.mkdirs(dirPath, FsPermission.getDefault());
      }

      String fileName = transformationFunctionDTO.getName() + TRANSFORMATION_FUNCTION_FILE_TYPE;
      Path filePath = new Path(dirPath, fileName);
      udfso.create(filePath, transformationFunctionDTO.getSourceCodeContent());

      return fileName;
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public void delete(Project project, Featurestore featurestore, Users user, Integer transformationFunctionId)
      throws FeaturestoreException {

    TransformationFunction transformationFunction =  transformationFunctionFacade.findById(transformationFunctionId)
        .orElseThrow(() ->
            new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRANSFORMATION_FUNCTION_DOES_NOT_EXIST,
        Level.FINE, "Could not find transformation function with ID" + transformationFunctionId));

    // Check if trying to delete built in transformation function
    if (FeaturestoreConstants.BUILT_IN_TRANSFORMATION_FUNCTION_NAMES.contains(transformationFunction.getName())
      && transformationFunction.getVersion() == 1) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_TRANSFORMERFUNCTION, Level.FINE,
        "Deleting built-in transformation function `" + transformationFunction.getName() + "` with version 1 is not " +
          "allowed. Create a new version instead.");
    }

    DistributedFileSystemOps udfso = null;
    try {
      udfso = dfs.getDfsOps(hdfsUsersController.getHdfsUserName(project, user));

      // Construct the directory path
      Path dirPath = getFullPath(featurestore, transformationFunction.getName(), transformationFunction.getVersion());

      // delete the record
      transformationFunctionFacade.delete(transformationFunction);

      // delete json files
      udfso.rm(dirPath, true);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_TRANSFORMERFUNCTION,
          Level.WARNING, "", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }

  public Path getFullPath(Featurestore featurestore, String name, Integer version) throws FeaturestoreException {
    return new Path(getTransformationFunctionsDirPath(featurestore), getFeatureStoreEntityName(name, version));
  }

  public String getTransformationFunctionsDirPath(Featurestore featurestore) throws FeaturestoreException {
    String connectorName =
        featurestore.getProject().getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
    FeaturestoreConnector featurestoreConnector = featurestoreConnectorFacade
        .findByFeaturestoreName(featurestore, connectorName)
        .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
            Level.FINE, "HOPSFS Connector: " + FeaturestoreConnectorType.HOPSFS.name()));
    Dataset trainingDatasetsFolder = featurestoreConnector.getHopsfsConnector().getHopsfsDataset();
    return inodeController.getPath(trainingDatasetsFolder.getInode())
        + "/" + TRANSFORMATIONFUNCTIONS_FOLDER;
  }

  /**
   * Verify transformation function input
   */
  private void verifyTransformationFunctionInput(TransformationFunctionDTO transformationFunctionDTO)
      throws FeaturestoreException {
    verifyVersion(transformationFunctionDTO.getVersion());
    verifyOutputType(transformationFunctionDTO.getOutputType());
  }

  /**
   * Verify user input transformation function version
   *
   * @param version the version to verify
   * @throws FeaturestoreException
   */
  private void verifyVersion(Integer version) throws FeaturestoreException {
    if(version <= 0) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.TRANSFORMATION_FUNCTION_VERSION, Level.FINE,
          " version cannot be negative or zero");
    }
  }

  /**
   * Verfiy user input output type
   *
   * @param outputType the output type to verify
   * @throws FeaturestoreException
   */
  private void verifyOutputType(String outputType) throws FeaturestoreException {
    if (!FeaturestoreConstants.TRANSFORMATION_FUNCTION_OUTPUT_TYPES.contains(outputType)) {
      throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.ILLEGAL_TRANSFORMATION_FUNCTION_OUTPUT_TYPE, Level.FINE, ", the recognized " +
          "transformation function output types are: " +
          StringUtils.join(FeaturestoreConstants.TRANSFORMATION_FUNCTION_OUTPUT_TYPES) + ". The provided " +
          "output type:" + outputType + " was not recognized.");
    }
  }
}