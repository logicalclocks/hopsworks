package io.hops.hopsworks.common.jobs;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.jobs.description.JobDescriptionFacade;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.util.Settings;

@Stateless
public class JobController {

  @EJB
  private JobDescriptionFacade jdFacade;
  @EJB
  private JobScheduler scheduler;
  @EJB
  private Settings settings;

  private static final Logger logger = Logger.getLogger(JobController.class.
          getName());

  public JobDescription createJob(Users user, Project project,
          JobConfiguration config) {
    JobDescription created = this.jdFacade.create(user, project, config);
    if (config.getSchedule() != null) {
      scheduler.scheduleJobPeriodic(created);
    }
    return created;
  }

  public boolean scheduleJob(int jobId) {
    boolean status = false;
    JobDescription job = this.jdFacade.findById(jobId);
    if (job != null) {
      scheduler.scheduleJobPeriodic(job);
      status = true;
    }
    return status;
  }

  /**
   * Returns aggregated log dir path for an application with the the given
   * appId.
   *
   * @param hdfsUser
   * @param appId
   * @return
   */
  public String getAggregatedLogPath(String hdfsUser, String appId) {
    String yarnConfDir = settings.getYarnConfDir();
    Path confPath = new Path(yarnConfDir);
    File confFile = new File(confPath + File.separator
            + Settings.DEFAULT_YARN_CONFFILE_NAME);
    if (!confFile.exists()) {
      logger.log(Level.SEVERE,
              "Unable to locate Yarn configuration file in {0}. Aborting log aggregation retry.",
              confFile);
      throw new IllegalStateException("No Yarn conf file");
    }
    Configuration conf = new Configuration();
    conf.addResource(new Path(confFile.getAbsolutePath()));
    String aggregatedLogPath = null;
    boolean logPathsAreAggregated = conf.getBoolean(
            YarnConfiguration.LOG_AGGREGATION_ENABLED,
            YarnConfiguration.DEFAULT_LOG_AGGREGATION_ENABLED);
    if (logPathsAreAggregated) {
      String[] nmRemoteLogDirs = conf.getStrings(
              YarnConfiguration.NM_REMOTE_APP_LOG_DIR,
              YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR);

      String[] nmRemoteLogDirSuffix = conf.getStrings(
              YarnConfiguration.NM_REMOTE_APP_LOG_DIR_SUFFIX,
              YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR_SUFFIX);
      aggregatedLogPath = nmRemoteLogDirs[0] + File.separator + hdfsUser
              + File.separator + nmRemoteLogDirSuffix[0] + File.separator
              + appId;
    }
    return aggregatedLogPath;
  }

}
