/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore;

import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasetjob.TrainingDatasetJobControllerIface;
import io.hops.hopsworks.common.featurestore.trainingdatasetjob.TrainingDatasetJobDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
public class TrainingDatasetJobController implements TrainingDatasetJobControllerIface {
  
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private Settings settings;
  
  private static JAXBContext trainingDatasetJobArgsJaxbContext = null;
  private static Marshaller trainingDatasetJobArgsMarshaller = null;
  
  /**
   * Verifies uniqueness of user provided feature list. Verifies that featuregroups version dict is provided if a
   * feature appears in multiple feature groups. Verifies all features exist.
   *
   * @param featureList
   * @param featurestore
   * @param featuregroupsVersionDict
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void verifyFeatureExistence(List<String> featureList, Featurestore featurestore,
    String featuregroupsVersionDict) throws FeaturestoreException {
    
    List<FeaturegroupDTO> featuregroupDTOs = featuregroupController.getFeaturegroupsForFeaturestore(featurestore);
    HashMap<String, Integer> featureCount = new HashMap<>();
    featureList.forEach(f -> featureCount.put(f, 0));
    
    // check for string duplicates
    if(featureList.size() != featureCount.size()){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_DUPLICATE_FEATURE,
        Level.WARNING);
    }
    
    // check if feature appears in more than one feature group
    for (FeaturegroupDTO group : featuregroupDTOs) {
      List<FeatureDTO> features = group.getFeatures();
      
      for (FeatureDTO f : features) {
        String featureName = f.getName();
        String prependedFeatureName = group.getName() + "_" + group.getVersion() + "." + featureName;
        String fullFeaturePath = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject()) + "." +
          prependedFeatureName;
        
        if (featureCount.containsKey(featureName)) {
          int count = featureCount.get(featureName) + 1;
          if (count == 2) {
            throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_FEATUREGROUP_DUPLICATE,
              Level.WARNING,
              String.format("Multiple featuregroups contain requested feature %s. Please prepend the featuregroup " +
                  "name and its version: [featurgroupname]_[version].[featurename]", featureName));
          }
          featureCount.put(featureName, count);
        } else if (featureCount.containsKey(prependedFeatureName)) {
          int count = featureCount.get(prependedFeatureName) + 1;
          if (count == 2) {
            throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_FAILURE,
              Level.WARNING,
              String.format("Feature %s does exist twice, despite prepended table name", prependedFeatureName));
          }
          featureCount.put(prependedFeatureName, count);
        } else if (featureCount.containsKey(fullFeaturePath)) {
          int count = featureCount.get(fullFeaturePath) + 1;
          if (count == 2) {
            throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_FAILURE,
              Level.WARNING,
              String.format("Feature %s does exist twice, despite prepended table name and database", fullFeaturePath));
          }
          featureCount.put(fullFeaturePath, count);
        }
      }
    }
    
    for(Map.Entry<String, Integer> entry : featureCount.entrySet()) {
      if(entry.getValue() == 0){
        String featurestoreName = featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject());
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST,
          Level.WARNING,
          String.format("Feature %s does not exist in featurestore %s", entry.getKey(), featurestoreName));
      }
    }
  }
  
  /**
   * Verifies if there is a training dataset with the given name and version in the featurestore already.
   *
   * @param project
   * @param featurestore
   * @param trainingDatasetVersion
   * @param trainingDataset
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public TrainingDatasetDTO verifyDatasetVersion(Project project, Featurestore featurestore, int
    trainingDatasetVersion, String trainingDataset) throws FeaturestoreException{
    return trainingDatasetController.getTrainingDatasetByFeaturestoreAndName(project, featurestore, trainingDataset,
      trainingDatasetVersion);
  }
  
  /**
   * Validates the job specification to make sure user provided either a feature list or a sql query string.
   *
   * @param features
   * @param sqlQuery
   * @return
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Boolean validateJobSpecification(List<String> features, String sqlQuery) throws FeaturestoreException {
    if (features == null && sqlQuery == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_MISSPECIFICATION,
        Level.WARNING, String.format("Both arguments 'features' and 'sql_query' are None, please provide one of the " +
        "two"));
    } else if (features != null && sqlQuery != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_MISSPECIFICATION,
        Level.WARNING, String.format("Both arguments 'features' and 'sql_query' are specified, please provide only " +
        "one of the two"));
    } else if (features == null && sqlQuery != null) {
      return true;
    } else if (features != null && sqlQuery == null) {
      return false;
    }
    throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_MISSPECIFICATION,
      Level.WARNING, String.format("Arguments 'features' and 'sql_query' are not specified correctly, please provide" +
      " only one of the two"));
  }
  
  /**
   * Writes JSON input for featurestore Util Job to HDFS as a JSON file
   *
   * @param user           user making the request
   * @param project        project of the user
   * @param trainingDatasetJobDTO     the JSON DTO
   * @return HDFS path where the JSON file was written
   * @throws FeaturestoreException
   * @throws JAXBException
   */
  public String writeUtilArgsToHdfs(Users user, Project project, TrainingDatasetJobDTO trainingDatasetJobDTO)
    throws FeaturestoreException, JAXBException {
    if(trainingDatasetJobArgsMarshaller == null){
      try {
        trainingDatasetJobArgsJaxbContext =
          JAXBContextFactory.createContext(new Class[]{TrainingDatasetJobDTO.class}, null);
        trainingDatasetJobArgsMarshaller = trainingDatasetJobArgsJaxbContext.createMarshaller();
        trainingDatasetJobArgsMarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
        trainingDatasetJobArgsMarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      } catch (JAXBException e) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_INITIALIZATION_ERROR,
          Level.SEVERE, "Error initialization feature store controller");
      }
    }
    String hdfsPath = settings.getBaseFeaturestoreTrainingDatasetJobDir(project)
      + Settings.FEATURESTORE_TRAININGDATASET_JOB_CONF + Path.SEPARATOR + trainingDatasetJobDTO.getTrainingDataset()
      + ".json";
    StringWriter sw = new StringWriter();
    trainingDatasetJobArgsMarshaller.marshal(trainingDatasetJobDTO, sw);
    
    try {
      featurestoreUtils.writeToHDFS(project, user, new Path(hdfsPath), sw.toString());
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_CONF_ERROR, Level.WARNING,
        "", e.getMessage(), e);
    }
    return "hdfs://" + hdfsPath;
  }
  
  /**
   * Create Job for creating training dataset in featurestore if the job does not exist yet.
   *
   * @param user
   * @param project
   * @param trainingDatasetJobDTO
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Jobs createTrainingDatasetJob(Users user, Project project, TrainingDatasetJobDTO trainingDatasetJobDTO)
    throws FeaturestoreException, JAXBException {
    
    Boolean sql = validateJobSpecification(trainingDatasetJobDTO.getFeatures(), trainingDatasetJobDTO.getSqlQuery());
    
    String featurestoreName = trainingDatasetJobDTO.getFeaturestore();
    if (featurestoreName == null) {
      // take project featurestore
      featurestoreName = featurestoreController.getOfflineFeaturestoreDbName(project);
    }
    FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithName(project,
      featurestoreName);
    Featurestore featurestore = featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId());
  
    TrainingDatasetDTO dataset = null;
    if (!trainingDatasetJobDTO.getOverwrite()) {
      try {
        dataset = verifyDatasetVersion(project, featurestore, trainingDatasetJobDTO
          .getTrainingDatasetVersion(), trainingDatasetJobDTO.getTrainingDataset());
      } catch (FeaturestoreException e) {
        // There is no dataset with same name and version, hence just continue
      }
      if (dataset != null){
        // There is a dataset with the specified version already
        throw new FeaturestoreException(
          RESTCodes.FeaturestoreErrorCode.TRAININGDATASETJOB_TRAININGDATASET_VERSION_EXISTS,
          Level.WARNING, String.format("Training dataset %s, version %s exists in %s. Set overwrite to true or " +
            "increment version", trainingDatasetJobDTO.getTrainingDataset(),
          trainingDatasetJobDTO.getTrainingDatasetVersion(),
          featurestoreController.getOfflineFeaturestoreDbName(featurestore.getProject())));
      }
    }
  
    writeUtilArgsToHdfs(user, project, trainingDatasetJobDTO);
  
    if (!sql) {
      verifyFeatureExistence(trainingDatasetJobDTO.getFeatures(), featurestore,
        trainingDatasetJobDTO.getFeaturegroupsVersionDict());
    }
    
    // Create the job if it doesn't exists.
    Jobs job = jobFacade.findByProjectAndName(project, trainingDatasetJobDTO.getTrainingDataset());
    if (job == null) {
      job = createJob(user, project, trainingDatasetJobDTO);
    }
    
    return job;
  }
  
  private Jobs createJob(Users user, Project project,
    TrainingDatasetJobDTO trainingDatasetJobDTO) {
    SparkJobConfiguration sparkJobConfiguration = new SparkJobConfiguration();
    sparkJobConfiguration.setAppName(trainingDatasetJobDTO.getTrainingDataset());
    String jobPath = settings.getFeaturestoreTrainingDatasetJobPath(trainingDatasetJobDTO.getSqlQuery());
    sparkJobConfiguration.setAppPath(jobPath);
    sparkJobConfiguration.setExecutorCores(trainingDatasetJobDTO.getExecutorCores());
    sparkJobConfiguration.setExecutorMemory(trainingDatasetJobDTO.getExecutorMemory());
    sparkJobConfiguration.setAmVCores(trainingDatasetJobDTO.getAmCores());
    sparkJobConfiguration.setAmMemory(trainingDatasetJobDTO.getAmMemory());
    sparkJobConfiguration.setDynamicAllocationMaxExecutors(trainingDatasetJobDTO.getMaxExecutors());
    sparkJobConfiguration.setMainClass(Settings.SPARK_PY_MAINCLASS);
    return jobFacade.put(user, project, sparkJobConfiguration, null);
  }
  
}
