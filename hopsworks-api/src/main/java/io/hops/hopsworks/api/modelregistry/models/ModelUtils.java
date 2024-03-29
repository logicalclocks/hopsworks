/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.modelregistry.models;

import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.explicit.ModelLinkController;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;

import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.provenance.ModelLink;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelUtils {
  private static final Logger LOGGER = Logger.getLogger(ModelUtils.class.getName());

  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private FeaturestoreFacade fsFacade;
  @EJB
  private FeatureViewFacade fvFacade;
  @EJB
  private FeatureViewController fvCtrl;
  @EJB
  private TrainingDatasetFacade tdFacade;
  @EJB
  private ModelLinkController modelLinkCtrl;

  public String getModelsDatasetPath(Project userProject, Project modelRegistryProject) {
    String modelsPath = Utils.getProjectPath(userProject.getName()) + Settings.HOPS_MODELS_DATASET + "/";
    if (!modelRegistryProject.equals(userProject)) {
      modelsPath = Utils.getProjectPath(userProject.getName()) +
          modelRegistryProject.getName() + "::" + Settings.HOPS_MODELS_DATASET + "/";
    }
    return modelsPath;
  }

  public void validateModelName(ModelDTO modelDTO) {
    if (!modelDTO.getName().matches("[a-zA-Z0-9_]+")) {
      throw new IllegalArgumentException("Model name must conform to regex: [a-zA-Z0-9_]+");
    }
  }

  public Project getModelsProjectAndCheckAccess(ModelDTO modelDTO, Project project)
    throws ProjectException, GenericException, DatasetException {
    Project modelProject;
    if (modelDTO.getProjectName() == null) {
      modelProject = project;
    } else {
      modelProject = projectFacade.findByName(modelDTO.getProjectName());
      if (modelProject == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.INFO, "model project not found");
      }
    }
    Dataset modelDataset = datasetCtrl.getByName(modelProject, Settings.HOPS_MODELS_DATASET);
    if (!accessCtrl.hasAccess(project, modelDataset)) {
      throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.INFO, "models endpoint");
    }
    return modelProject;
  }

  public Project getExperimentProjectAndCheckAccess(ModelDTO modelDTO, Project project)
    throws ProjectException, GenericException {
    Project experimentProject;
    if (modelDTO.getExperimentProjectName() == null) {
      experimentProject = project;
    } else {
      experimentProject = projectFacade.findByName(modelDTO.getExperimentProjectName());
      if (experimentProject == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.INFO,
          "experiment project not found for model");
      }
    }
    if (!experimentProject.getId().equals(project.getId())) {
      String usrMsg = "writing to shared experiment is not yet allowed";
      throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.INFO, usrMsg, usrMsg);
    }
    return experimentProject;
  }

  public ModelsController.Accessor getModelsAccessor(Users user, Project userProject, Project modelProject,
                                                   Project experimentProject)
    throws DatasetException {
    DistributedFileSystemOps udfso = null;
    try {
      String hdfsUser = hdfsUsersController.getHdfsUserName(experimentProject, user);
      udfso = dfs.getDfsOps(hdfsUser);
      return new ModelsController.Accessor(user, userProject, modelProject, experimentProject, udfso, hdfsUser);
    } catch (Throwable t) {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OPERATION_ERROR, Level.INFO);
    }
  }

  public String getModelFullPath(Project modelRegistryProject, String modelName, Integer modelVersion) {
    return Utils.getProjectPath(modelRegistryProject.getName()) +
        Settings.HOPS_MODELS_DATASET + "/" + modelName + "/" + modelVersion;
  }

  public String getModelFullPath(ModelVersion modelVersion) {
    return getModelFullPath(modelVersion.getModel().getProject(), modelVersion.getModel().getName(),
        modelVersion.getVersion());
  }
  
  public String[] getModelNameAndVersion(String mlId) {
    int splitIndex = mlId.lastIndexOf("_");
    return new String[]{mlId.substring(0, splitIndex), mlId.substring(splitIndex + 1)};
  }

  public ModelLink createExplicitProvenanceLink(ModelVersion model, ModelDTO modelDTO) {
    FeatureView fv = null;
    Integer tdVersion = null;
    if(modelDTO.getTrainingDataset() != null) {
      if(modelDTO.getTrainingDataset().getId() != null) {
        TrainingDataset td = tdFacade.find(modelDTO.getTrainingDataset().getId());
        if(td == null) {
          LOGGER.info("training dataset not found - cannot create model provenance link");
        }
        return modelLinkCtrl.createParentLink(model, td);
      }
      Pair<String, Integer> fvNameVersion = splitTdName(modelDTO.getTrainingDataset().getName());
      if(fvNameVersion == null) {
        LOGGER.info("training dataset name is wrong - cannot create model provenance link");
        return null;
      }
      fv = getFeatureView(modelDTO.getTrainingDataset().getFeaturestoreId(),
          fvNameVersion.getValue0(), fvNameVersion.getValue1());
      tdVersion = modelDTO.getTrainingDataset().getVersion();
    } else if(modelDTO.getFeatureView() != null) {
      if(modelDTO.getFeatureView().getId() != null) {
        fv = fvFacade.find(modelDTO.getFeatureView().getId());
      } else {
        fv = getFeatureView(modelDTO.getFeatureView().getFeaturestoreId(),
            modelDTO.getFeatureView().getName(), modelDTO.getFeatureView().getVersion());
      }
      tdVersion = modelDTO.getTrainingDatasetVersion();
    }
    if(fv == null) {
      return null;
    }
    if(tdVersion == null) {
      LOGGER.info("training dataset version is missing - cannot create model provenance link");
      return null;
    }
    try {
      TrainingDataset trainingDataset = tdFacade.findByFeatureViewAndVersion(fv, modelDTO.getTrainingDatasetVersion());
      return modelLinkCtrl.createParentLink(model, trainingDataset);
    } catch (FeaturestoreException e) {
      LOGGER.info("training dataset not found - cannot create model provenance link");
      return null;
    }
  }

  private Pair<String, Integer> splitTdName(String tdName) {
    int split = tdName.lastIndexOf("_");
    if(split == -1) {
      return null;
    }
    try {
      String fvName = tdName.substring(0, split);
      int fvVersion;
      try {
        fvVersion = Integer.parseInt(tdName.substring(split + 1));
      } catch (NumberFormatException e) {
        return null;
      }
      return Pair.with(fvName, fvVersion);
    } catch (IndexOutOfBoundsException e) {
      return null;
    }
  }

  private FeatureView getFeatureView(Integer fsId, String fvName, Integer fvVersion) {
    Featurestore fvFS = fsFacade.findById(fsId);
    if(fvFS == null) {
      LOGGER.info("feature view featurestore not found - cannot create model provenance link");
      return null;
    }
    try {
      return fvCtrl.getByNameVersionAndFeatureStore(fvName, fvVersion, fvFS);
    } catch (FeaturestoreException e) {
      LOGGER.info("feature view not found - cannot create model provenance link");
      return null;
    }
  }
}