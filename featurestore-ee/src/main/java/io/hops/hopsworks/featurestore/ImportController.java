/*
 *Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hops.hopsworks.common.featurestore.ImportControllerIface;
import io.hops.hopsworks.common.featurestore.importjob.FeaturegroupImportJobDTO;
import io.hops.hopsworks.common.featurestore.importjob.ImportType;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.util.logging.Level;

@Stateless
@EnterpriseStereotype
public class ImportController implements ImportControllerIface {

  @EJB
  private Settings settings;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private DistributedFsService dfs;

  private Gson featuregroupImportJobGson = new GsonBuilder()
      .setPrettyPrinting()
      .addSerializationExclusionStrategy(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes field) {
          return field.getName().equals("amCores") || field.getName().equals("amMemory") ||
              field.getName().equals("executorCores") || field.getName().equals("executorMemory") ||
              field.getName().equals("maxExecutors");
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
          return false;
        }
      }).create();


  public Jobs createImportJob(Users user, Project project, FeaturegroupImportJobDTO featuregroupImportJobDTO)
      throws FeaturestoreException {

    String jsonJobConfigFilePath = settings.getBaseFeaturestoreJobImportDir(project)
        + Settings.FEATURESTORE_IMPORT_CONF + Path.SEPARATOR + featuregroupImportJobDTO.getFeaturegroup() + ".json";

    // Create the job if it doesn't exists.
    Jobs job = jobFacade.findByProjectAndName(project, featuregroupImportJobDTO.getFeaturegroup());
    if (job == null) {
      job = createImportJob(user, project, featuregroupImportJobDTO, jsonJobConfigFilePath);
    }

    // Create or update the json file to pass to the import job
    try {
      String jobConfig = featuregroupImportJobGson.toJson(featuregroupImportJobDTO);
      featurestoreUtils.writeToHDFS(project, user, new Path(jsonJobConfigFilePath), jobConfig);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.IMPORT_CONF_ERROR, Level.WARNING,
          "", e.getMessage(), e);
    }

    return job;
  }

  private Jobs createImportJob(Users user, Project project,
                               FeaturegroupImportJobDTO featuregroupImportJobDTO, String jsonJobConfigPath)
      throws FeaturestoreException {
    SparkJobConfiguration sparkJobConfiguration = new SparkJobConfiguration();
    sparkJobConfiguration.setAppName(featuregroupImportJobDTO.getFeaturegroup());
    sparkJobConfiguration.setAppPath(settings.getFeaturestoreImportJobPath());
    sparkJobConfiguration.setExecutorCores(featuregroupImportJobDTO.getExecutorCores());
    sparkJobConfiguration.setExecutorMemory(featuregroupImportJobDTO.getExecutorMemory());
    sparkJobConfiguration.setAmVCores(featuregroupImportJobDTO.getAmCores());
    sparkJobConfiguration.setAmMemory(featuregroupImportJobDTO.getAmMemory());
    sparkJobConfiguration.setMainClass(Settings.SPARK_PY_MAINCLASS);

    if (featuregroupImportJobDTO.getType() == ImportType.REDSHIFT) {
      sparkJobConfiguration.setJars(getRedshiftJDBCDriverPath(project));
    }

    return jobFacade.put(user, project, sparkJobConfiguration, null);
  }

  // We cannot distribute the Redshift JDBC driver. Users are supposed to upload it in the Resource directory
  // under the name RedshiftJDBC42-no-awssdk.jar
  private String getRedshiftJDBCDriverPath(Project project) throws FeaturestoreException {
    String driverPath = Utils.getProjectPath(project.getName()) + Path.SEPARATOR
        + Settings.BaseDataset.RESOURCES.getName() + Path.SEPARATOR
        + Settings.REDSHIFT_JDBC_NAME;
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      if (!dfso.exists(driverPath)) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.MISSING_REDSHIFT_DRIVER, Level.FINE);
      }
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.MISSING_REDSHIFT_DRIVER, Level.SEVERE,
          "Could not check if Redshift driver was present", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(dfso);
    }

    return driverPath;
  }

}
