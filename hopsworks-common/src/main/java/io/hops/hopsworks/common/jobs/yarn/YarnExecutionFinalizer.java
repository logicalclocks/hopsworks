package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.JobsHistoryFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;

@Stateless
@DependsOn("Settings")
public class YarnExecutionFinalizer {

  private static final Logger LOG = Logger.getLogger(YarnExecutionFinalizer.class.getName());

  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private JobsHistoryFacade jobsHistoryFacade;
  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfs;

  @Asynchronous
  public void copyLogsAndFinalize(Execution exec, YarnApplicationState appState) {
    YarnClient newClient = YarnClient.createYarnClient();
    newClient.init(settings.getConfiguration());
    ApplicationId applicationId = ConverterUtils.toApplicationId(exec.getAppId());
    YarnMonitor monitor = new YarnMonitor(applicationId, newClient);
    monitor = monitor.start();
    exec = updateState(JobState.AGGREGATING_LOGS, exec);
    copyLogs(exec, monitor);
    monitor.close();
    finalize(exec, JobState.getJobState(appState));

  }

  /**
   * Update the current state of the Execution entity to the given state.
   * <p/>
   * @param newState
   */
  private Execution updateState(JobState newState, Execution execution) {
    return executionFacade.updateState(execution, newState);
  }

  private void copyLogs(Execution exec, YarnMonitor monitor) {
    DistributedFileSystemOps udfso = dfs.getDfsOps(exec.getHdfsUser());

    String defaultOutputPath;
    switch (exec.getJob().getJobType()) {
      case SPARK:
      case PYSPARK:
      case TFSPARK:
        defaultOutputPath = Settings.SPARK_DEFAULT_OUTPUT_PATH;
        break;
      case FLINK:
        defaultOutputPath = Settings.FLINK_DEFAULT_OUTPUT_PATH;
        break;
      case ADAM:
        defaultOutputPath = Settings.ADAM_DEFAULT_OUTPUT_PATH;
        break;
      case YARN:
        defaultOutputPath = Settings.YARN_DEFAULT_OUTPUT_PATH;
      default:
        defaultOutputPath = "Logs/";
    }
    String stdOutFinalDestination = Utils.getHdfsRootPath(exec.getJob().getProject().getName()) + defaultOutputPath;
    String stdErrFinalDestination = Utils.getHdfsRootPath(exec.getJob().getProject().getName()) + defaultOutputPath;
    
    String stdOutPath = settings.getAggregatedLogPath(exec.getHdfsUser(), exec.getAppId());
    try {
      if (stdOutFinalDestination != null && !stdOutFinalDestination.isEmpty()) {
        stdOutFinalDestination = stdOutFinalDestination + exec.getAppId() + File.separator + "stdout.log";
        String[] desiredLogTypes = {"out"};
        YarnLogUtil.copyAggregatedYarnLogs(udfso, stdOutPath, stdOutFinalDestination, desiredLogTypes, monitor);
      }
      if (stdErrFinalDestination != null && !stdErrFinalDestination.isEmpty()) {
        stdErrFinalDestination = stdErrFinalDestination + exec.getAppId() + File.separator + "stderr.log";
        String[] desiredLogTypes = {"err", ".log"};
        YarnLogUtil.copyAggregatedYarnLogs(udfso, stdOutPath, stdErrFinalDestination, desiredLogTypes, monitor);
      }
    } catch (IOException | InterruptedException | YarnException ex) {
      LOG.severe("error while aggregation logs" + ex.toString());
    }
    updateExecutionSTDPaths(stdOutFinalDestination, stdErrFinalDestination, exec);
  }

  /**
   * Removes the marker file for streaming jobs if it exists, after a non FINISHED/SUCCEEDED job.
   *
   * @param udfso
   */
  private void removeMarkerFile(Execution exec) {
    String marker = Settings.getJobMarkerFile(exec.getJob(), exec.getAppId());
    try {
      DistributedFileSystemOps dfso = dfs.getDfsOps();
      if (dfso.exists(marker)) {
        dfso.rm(new org.apache.hadoop.fs.Path(marker), false);
      }
    } catch (IOException ex) {
      LOG.log(Level.WARNING, "Could not remove marker file for job:{0}, with appId:{1}, {2}", new Object[]{
        exec.getJob().getName(), exec.getAppId(), ex.getMessage()});
    }
  }

  public void finalize(Execution exec, JobState jobState) {
    long executionStop = System.currentTimeMillis();
    exec = executionFacade.updateExecutionStop(exec, executionStop);
    updateJobHistoryApp(exec.getExecutionDuration(), exec);
    try {
      removeAllNecessary(exec);
    } catch (IOException ex) {
      LOG.log(Level.WARNING,
          "Exception while cleaning after job:{0}, with appId:{1}, some cleanning is probably needed {2}", new Object[]{
            exec.getJob().getName(), exec.getAppId(), ex.getMessage()});
    }
    if (exec.getJob().getJobType().equals(JobType.FLINK)) {
      cleanCerts(exec);
    }
    updateState(jobState, exec);
  }

  private void updateJobHistoryApp(long executiontime, Execution execution) {
    jobsHistoryFacade.updateJobHistory(execution, executiontime);
  }

  private Execution updateExecutionSTDPaths(String stdoutPath, String stderrPath, Execution exec) {
    exec = executionFacade.updateStdErrPath(exec, stderrPath);
    exec = executionFacade.updateStdOutPath(exec, stdoutPath);
    return exec;
  }

  public void removeAllNecessary(Execution exec) throws IOException {
    List<String> filesToRemove = exec.getFilesToRemove();
    String appDir = "hdfs://" + settings.getHdfsTmpCertDir() + "/" + exec.getHdfsUser() + File.separator + exec.
        getAppId();
    filesToRemove.add(appDir);
    String tensorboardFile = "hdfs://" + File.separator + Settings.DIR_ROOT + File.separator + exec.getJob().
        getProject().getName() + File.separator + Settings.PROJECT_STAGING_DIR + File.separator + ".tensorboard."
        + exec.getAppId();
    filesToRemove.add(tensorboardFile);
    String certsAppDir = Paths.get(settings.getFlinkKafkaCertDir(), exec.getAppId()).toString();
    filesToRemove.add(certsAppDir);
    FileSystem fs = FileSystem.get(settings.getConfiguration());
    for (String s : filesToRemove) {
      if (s.startsWith("hdfs:") && fs.exists(new Path(s))) {
        fs.delete(new Path(s), true);
      } else {
        org.apache.commons.io.FileUtils.deleteQuietly(new File(s));
      }
    }
    removeMarkerFile(exec);
  }

  private void cleanCerts(Execution exec) {
    //Remove local files required for the job (Kafka certs etc.)
    //Search for other jobs using Kafka in the same project. If any active
    //ones are found

    Collection<ProjectServices> projectServices = exec.getJob().getProject().getProjectServicesCollection();
    Iterator<ProjectServices> iter = projectServices.iterator();
    boolean removeKafkaCerts = true;
    while (iter.hasNext()) {
      ProjectServices projectService = iter.next();
      //If the project is of type KAFKA
      if (projectService.getProjectServicesPK().getService() == ProjectServiceEnum.KAFKA) {
        List<Execution> execs = executionFacade.findForProjectByType(exec.getJob().getProject(), JobType.FLINK);
        if (execs != null) {
          execs.addAll(executionFacade.findForProjectByType(exec.getJob().getProject(), JobType.SPARK));
        }
        //Find if this project has running jobs
        if (execs != null && !execs.isEmpty()) {
          for (Execution exe : execs) {
            if (!exe.getState().isFinalState()) {
              removeKafkaCerts = false;
              break;
            }
          }
        }
      }
    }
    if (removeKafkaCerts) {
      String k_certName = exec.getHdfsUser() + "__kstore.jks";
      String t_certName = exec.getHdfsUser() + "__tstore.jks";
      File k_cert = new File(settings.getHopsworksDomainDir() + "/domain1/config/" + k_certName);
      File t_cert = new File(settings.getHopsworksDomainDir() + "/domain1/config/" + t_certName);
      if (k_cert.exists()) {
        k_cert.delete();
      }
      if (t_cert.exists()) {
        t_cert.delete();
      }
    }
  }
}
