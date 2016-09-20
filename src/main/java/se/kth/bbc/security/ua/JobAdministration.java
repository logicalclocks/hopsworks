package se.kth.bbc.security.ua;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.impl.YarnClientImpl;
import org.apache.hadoop.yarn.exceptions.YarnException;
import se.kth.bbc.jobs.jobhistory.YarnApplicationstate;
import se.kth.bbc.jobs.jobhistory.YarnApplicationstateFacade;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.spark.SparkJob;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.hopsworks.hdfs.fileoperations.UserGroupInformationService;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.util.Settings;

/**
 * AdminUI for administering yarn jobs.
 * <p>
 */
@ManagedBean
@ViewScoped
public class JobAdministration implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(JobAdministration.class.
          getName());
//  @EJB
//  private JobDescriptionFacade jobFacade;
//  @EJB
//  private ExecutionFacade exeFacade;
  @EJB
  private YarnApplicationstateFacade yarnFacade;
  @EJB
  private Settings settings;
  @EJB
  private UserGroupInformationService ugiService;
  @EJB
  private HdfsUsersController hdfsUsersBean;

  private Configuration conf;

  private List<YarnApplicationstate> jobs;

  private List<YarnApplicationstate> filteredJobs;

  private Map<String, String> error = new HashMap<>();

  public List<YarnApplicationstate> getAllJobs() {
    this.jobs = yarnFacade.findAll();
    return this.jobs;
  }

  public void setFilteredJobs(List<YarnApplicationstate> filteredJobs) {
    this.filteredJobs = filteredJobs;
  }

  public List<YarnApplicationstate> getFilteredJobs() {
    return filteredJobs;
  }

  public void killJob(final String appId) {
    error.put(appId, "Trying to kill job");
    YarnClient client = YarnClient.createYarnClient();
    setConfiguration(settings.getHadoopDir());
    client.init(conf);
    client.start();
    //Find applicationId and kill it
    error.put(appId, "Application was not found");
    YarnApplicationState state = null;
    try {
      for (ApplicationReport appReport : client.getApplications()) {
        String currAppId = appReport.getApplicationId().toString();
        if (currAppId != null && currAppId.equals(appId)) {
          state = appReport.getYarnApplicationState();
          if (state == YarnApplicationState.FINISHED || state
                  == YarnApplicationState.KILLED) {
            error.put(appId, "Job is already " + state);
          } else {
            ApplicationId appIdToKill = appReport.getApplicationId();
            client.killApplication(appIdToKill);
            state = client.getApplicationReport(appIdToKill).getYarnApplicationState();
            error.put(appId, "Job killed successfully");
            break;
          }
        }
      }
      //Wait for the DB to be updated
      Thread.sleep(1000);
      jobs = yarnFacade.findAll();
      if (filteredJobs != null) {
        //Update filtered job
        for (YarnApplicationstate yarnApplicationState : filteredJobs) {
          if (yarnApplicationState.getApplicationid().equals(appId) && state
                  != null) {
            yarnApplicationState.setAppsmstate(state.name());
          }
        }
      }
      client.close();
    } catch (YarnException | IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, "Error while trying to kill job with appId:"+
              appId, ex.getMessage());
    } finally {
      try {
        client.close();
      } catch (IOException ex) {
        logger.log(Level.SEVERE, "Error while trying to close yarn client",
                ex.getMessage());
      }
    }

  }

  public String getError(String appId) {
    if (error.containsKey(appId)) {
      return error.get(appId);
    }
    return null;
  }

  public void setError(Map<String, String> error) {
    this.error = error;
  }

  private void setConfiguration(String hadoopDir)
          throws IllegalStateException {
    //Get the path to the Yarn configuration file from environment variables
    String yarnConfDir = System.getenv(Settings.ENV_KEY_YARN_CONF_DIR);
//      If not found in environment variables: warn and use default,
    if (yarnConfDir == null) {
      logger.log(Level.WARNING,
              "Environment variable "
              + Settings.ENV_KEY_YARN_CONF_DIR
              + " not found, using settings: {0}", Settings.getYarnConfDir(
                      hadoopDir));
      yarnConfDir = Settings.getYarnConfDir(hadoopDir);

    }

    Path confPath = new Path(yarnConfDir);
    File confFile = new File(confPath + File.separator
            + Settings.DEFAULT_YARN_CONFFILE_NAME);
    if (!confFile.exists()) {
      logger.log(Level.SEVERE,
              "Unable to locate Yarn configuration file in {0}. Aborting exectution.",
              confFile);
      throw new IllegalStateException("No Yarn conf file");
    }

    //Also add the hadoop config
    String hadoopConfDir = System.getenv(Settings.ENV_KEY_HADOOP_CONF_DIR);
    //If not found in environment variables: warn and use default
    if (hadoopConfDir == null) {
      logger.log(Level.WARNING,
              "Environment variable "
              + Settings.ENV_KEY_HADOOP_CONF_DIR
              + " not found, using default {0}",
              (hadoopDir + "/" + Settings.HADOOP_CONF_RELATIVE_DIR));
      hadoopConfDir = hadoopDir + "/" + Settings.HADOOP_CONF_RELATIVE_DIR;
    }
    confPath = new Path(hadoopConfDir);
    File hadoopConf = new File(confPath + "/"
            + Settings.DEFAULT_HADOOP_CONFFILE_NAME);
    if (!hadoopConf.exists()) {
      logger.log(Level.SEVERE,
              "Unable to locate Hadoop configuration file in {0}. Aborting exectution.",
              hadoopConf);
      throw new IllegalStateException("No Hadoop conf file");
    }

    //And the hdfs config
    File hdfsConf = new File(confPath + "/"
            + Settings.DEFAULT_HDFS_CONFFILE_NAME);
    if (!hdfsConf.exists()) {
      logger.log(Level.SEVERE,
              "Unable to locate HDFS configuration file in {0}. Aborting exectution.",
              hdfsConf);
      throw new IllegalStateException("No HDFS conf file");
    }

    //Set the Configuration object for the returned YarnClient
    conf = new Configuration();
    conf.addResource(new Path(confFile.getAbsolutePath()));
    conf.addResource(new Path(hadoopConf.getAbsolutePath()));
    conf.addResource(new Path(hdfsConf.getAbsolutePath()));

    YarnRunner.Builder.addPathToConfig(conf, confFile);
    YarnRunner.Builder.addPathToConfig(conf, hadoopConf);
    YarnRunner.Builder.setDefaultConfValues(conf);
  }

}
