package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.impl.YarnClientImpl;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.execution.HopsJob;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

public abstract class YarnJob extends HopsJob {
  
  private static final Logger LOG = Logger.getLogger(YarnJob.class.getName());

  protected YarnRunner runner;

  protected YarnMonitor monitor = null;
  private final Configuration conf = new Configuration();

  private String stdOutFinalDestination, stdErrFinalDestination;
  protected List<LocalResourceDTO> projectLocalResources;
  protected Map<String, String> jobSystemProperties;

  protected final String jobUser;

  /**
   * Constructor for job interacting with the Kafka service.
   *
   * @param job
   * @param user
   * @param services
   * @param jobUser
   * @param hadoopDir
   * @param nameNodeIpPort
   * @param jobsMonitor
   * @throws IllegalArgumentException If the JobDescription does not contain a
   * YarnJobConfiguration object.
   */
  public YarnJob(JobDescription job, AsynchronousJobExecutor services,
      Users user, String jobUser, String hadoopDir, String nameNodeIpPort, YarnJobsMonitor jobsMonitor) {
    super(job, services, user, hadoopDir, nameNodeIpPort, jobsMonitor);
    if (!(job.getJobConfig() instanceof YarnJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must be a YarnJobConfiguration object. Received class: "
          + job.getJobConfig().getClass());
    }
    LOG.log(Level.INFO, "Instantiating Yarn job as user: {0}", hdfsUser);
    this.jobSystemProperties = new HashMap<>();
    this.projectLocalResources = new ArrayList<>();
    this.jobUser = jobUser;
  }

  public final void setStdOutFinalDestination(String stdOutFinalDestination) {
    this.stdOutFinalDestination = stdOutFinalDestination;
  }

  public final void setStdErrFinalDestination(String stdErrFinalDestination) {
    this.stdErrFinalDestination = stdErrFinalDestination;
  }

  protected final String getStdOutFinalDestination() {
    return this.stdOutFinalDestination;
  }

  protected final String getStdErrFinalDestination() {
    return this.stdErrFinalDestination;
  }

  /**
   * Start the YARN application master.
   * 
   * @param udfso
   * @param dfso
   * @return True if the AM was started, false otherwise.
   * @throws IllegalStateException If the YarnRunner has not been set yet.
   */
  protected final boolean startApplicationMaster(DistributedFileSystemOps udfso,
      DistributedFileSystemOps dfso) throws IllegalStateException {
    if (runner == null) {
      throw new IllegalStateException(
          "The YarnRunner has not been initialized yet.");
    }
    try {
      updateState(JobState.STARTING_APP_MASTER);
      monitor = runner.startAppMaster(jobDescription.getProject(),
          dfso, user.getUsername());
      execution = services.getExecutionFacade().updateFilesToRemove(execution, runner.getFilesToRemove());
      execution = services.getExecutionFacade().updateAppId(execution, monitor.getApplicationId().toString());
      return true;
    } catch (AccessControlException ex) {
      LOG.log(Level.SEVERE, "Permission denied:- {0}", ex.getMessage());
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    } catch (YarnException | IOException | URISyntaxException e) {
      LOG.log(Level.SEVERE, "Failed to start application master for execution " + execution + 
          ". Aborting execution", e);
      writeLog("Failed to start application master for execution " + execution + ". Aborting execution", e, udfso);
      try {
        services.getYarnExecutionFinalizer().removeAllNecessary(execution);
      } catch (IOException ex) {
        LOG.log(Level.WARNING, "Failed to remove files for failed execution {0}", execution);
        writeLog("Failed to remove files for failed execution " + execution, ex, udfso);
      }
      updateState(JobState.APP_MASTER_START_FAILED);
      return false;
    }
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    //Check if this job is using Kakfa, and include certificate
    //in local resources
    serviceProps = new ServiceProperties(services.getSettings().getHopsworksMasterPasswordSsl(),
        services.getSettings().getHopsworksMasterPasswordSsl(), jobDescription.getProject().getId(),
        jobDescription.getProject().getName(), services.getSettings().getRestEndpoint(), jobDescription.getName(),
        new ElasticProperties(services.getSettings().getElasticRESTEndpoint()));

    if (jobDescription.getProject().getConda()) {
      serviceProps.initAnaconda(services.getSettings().getAnacondaProjectDir(jobDescription.getProject().getName())
          + File.separator + "bin" + File.separator + "python");
    }
    Collection<ProjectServices> projectServices = jobDescription.getProject().
        getProjectServicesCollection();
    if (projectServices != null && !projectServices.isEmpty()) {
      Iterator<ProjectServices> iter = projectServices.iterator();
      while (iter.hasNext()) {
        ProjectServices projectService = iter.next();
        //If the project is of type KAFKA
        if (projectService.getProjectServicesPK().getService()
            == ProjectServiceEnum.KAFKA && (jobDescription.getJobType()
            == JobType.FLINK || jobDescription.getJobType() == JobType.SPARK)
            && jobDescription.getJobConfig() instanceof YarnJobConfiguration
            && jobDescription.getJobConfig().getKafka() != null) {
          serviceProps.initKafka();
          //Set Kafka specific properties to serviceProps
          serviceProps.getKafka().setBrokerAddresses(services.getSettings().getKafkaConnectStr());
          serviceProps.getKafka().setRestEndpoint(services.getSettings().getRestEndpoint());
          serviceProps.getKafka().setTopics(jobDescription.getJobConfig().getKafka().getTopics());
          serviceProps.getKafka().setProjectConsumerGroups(jobDescription.getProject().getName(), 
              jobDescription.getJobConfig().getKafka().getConsumergroups());
          return true;
        }
      }
    }
    return true;
  }

  final EnumSet<YarnApplicationState> finalAppState = EnumSet.of(
      YarnApplicationState.FINISHED, YarnApplicationState.FAILED,
      YarnApplicationState.KILLED);
  
  protected void writeLog(String message, Exception exception, DistributedFileSystemOps udfso) {

    Date date = new Date();
    String dateString = date.toString();
    dateString = dateString.replace(" ", "_").replace(":", "-");
    stdErrFinalDestination = stdErrFinalDestination + jobDescription.getName() + dateString + "/stderr.log";
    YarnLogUtil.writeLog(udfso, stdErrFinalDestination, message, exception);
    services.getExecutionFacade().updateStdErrPath(execution, stdErrFinalDestination);
  }

  @Override
  protected void writeToLogs(String message, Exception e) throws IOException {
    writeLog(message, e, services.getFileOperations(jobUser));
  }

  @Override
  protected void writeToLogs(String message) throws IOException {
    writeToLogs(message, null);
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
      DistributedFileSystemOps dfso) {
    // Try to start the AM
    boolean proceed = startApplicationMaster(udfso, dfso);

    if (!proceed) {
      return;
    }
    jobsMonitor.addToMonitor(execution.getAppId(), execution, monitor);
    
  }

  @Override
  //DOESN'T WORK FOR NOW
  protected void stopJob(String appid) {
    YarnClient yarnClient = null;
    try {
      yarnClient = new YarnClientImpl();
      yarnClient.init(conf);
      yarnClient.start();
      ApplicationId applicationId = ConverterUtils.toApplicationId(appid);
      yarnClient.killApplication(applicationId);
    } catch (YarnException | IOException e) {
      LOG.log(Level.SEVERE,"Could not close yarn client for killing yarn job");
    } finally {
      if (yarnClient != null) {
        try {
          yarnClient.close();
        } catch (IOException ex) {
          LOG.log(Level.WARNING,
              "Could not close yarn client for killing yarn job");
        }
      }
    }
  }
}
