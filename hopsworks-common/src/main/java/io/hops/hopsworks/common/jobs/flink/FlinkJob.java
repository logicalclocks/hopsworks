package io.hops.hopsworks.common.jobs.flink;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServices;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJob;
import io.hops.hopsworks.common.util.Settings;

/**
 * Orchestrates the execution of a Flink job: run job, update history object.
 * <p>
 */
public class FlinkJob extends YarnJob {

  private static final Logger LOG = Logger.getLogger(
      FlinkJob.class.getName());
  private final FlinkJobConfiguration jobconfig;
  private final String flinkDir;
  private final String glassfishDomainDir;
  private final String flinkConfDir;
  private final String flinkConfFile;
  private final String flinkUser;
  private final String JOBTYPE_STREAMING = "Streaming";

  /**
   *
   * @param job
   * @param services
   * @param user
   * @param hadoopDir
   * @param flinkDir
   * @param flinkConfDir
   * @param flinkConfFile
   * @param nameNodeIpPort
   * @param flinkUser
   * @param jobUser
   * @param glassfishDomainsDir
   */
  public FlinkJob(JobDescription job, AsynchronousJobExecutor services,
      Users user, final String hadoopDir,
      final String flinkDir, final String flinkConfDir,
      final String flinkConfFile, final String nameNodeIpPort,
      String flinkUser, String jobUser, final String glassfishDomainsDir) {
    super(job, services, user, jobUser, hadoopDir, nameNodeIpPort);
    if (!(job.getJobConfig() instanceof FlinkJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must contain a FlinkJobConfiguration object. Received: "
          + job.getJobConfig().getClass());
    }
    this.jobconfig = (FlinkJobConfiguration) job.getJobConfig();
    this.jobconfig.setFlinkConfDir(flinkConfDir);
    this.jobconfig.setFlinkConfFile(flinkConfFile);
    this.flinkDir = flinkDir;
    this.glassfishDomainDir = glassfishDomainsDir;
    this.flinkConfDir = flinkConfDir;
    this.flinkConfFile = flinkConfFile;
    this.flinkUser = flinkUser;
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso) {
    super.setupJob(dfso);
    
    //Then: actually get to running.
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled Flink Job");
    }

    FlinkYarnRunnerBuilder flinkBuilder = new FlinkYarnRunnerBuilder(
        jobconfig.getJarPath(), jobconfig.getMainClass());
    //https://ci.apache.org/projects/flink/flink-docs-release-0.10/setup/yarn_setup.html
    /*
     * If you do not want to keep the Flink YARN client running all the time,
     * its also possible to start a detached YARN session. The parameter for
     * that is called -d or --detached. In that case, the Flink YARN client
     * will only submit Flink to the cluster and then close itself.
     */
    flinkBuilder.setDetachedMode(true);
    flinkBuilder.setName(jobconfig.getAppName());
    flinkBuilder.setConfigurationDirectory(jobconfig.getFlinkConfDir());
    flinkBuilder.setConfigurationFilePath(new Path(
        jobconfig.getFlinkConfFile()));
    //Flink specific conf object
    flinkBuilder.setFlinkLoggingConfigurationPath(new Path(
        jobconfig.getFlinkConfDir()));

    flinkBuilder.setTaskManagerMemory(jobconfig.getTaskManagerMemory());
    flinkBuilder.setTaskManagerSlots(jobconfig.getSlots());
    flinkBuilder.setTaskManagerCount(jobconfig.getNumberOfTaskManagers());
    if (jobconfig.getFlinkjobtype().equals(JOBTYPE_STREAMING)) {
      flinkBuilder.setStreamingMode(true);
    }
    flinkBuilder.setParallelism(jobconfig.getParallelism());
    flinkBuilder.setJobManagerMemory(jobconfig.getAmMemory());
    flinkBuilder.setJobManagerQueue(jobconfig.getAmQueue());
    flinkBuilder.setAppJarPath(jobconfig.getJarPath());
    //Set Kafka params
    flinkBuilder.setServiceProps(serviceProps);
    flinkBuilder.addExtraFiles(Arrays.asList(jobconfig.getLocalResources()));
    //Set project specific resources, i.e. Kafka certificates
    flinkBuilder.addExtraFiles(projectLocalResources);
    if (jobconfig.getArgs() != null && !jobconfig.getArgs().isEmpty()) {
      String[] jobArgs = jobconfig.getArgs().trim().split(" ");
      flinkBuilder.addAllJobArgs(jobArgs);
    }
    if (jobSystemProperties != null && !jobSystemProperties.isEmpty()) {
      for (Map.Entry<String, String> jobSystemProperty : jobSystemProperties.
          entrySet()) {
//        //If the properties are the Kafka certificates, append glassfish path
//        if (jobSystemProperty.getKey().equals(Settings.KAFKA_K_CERTIFICATE)
//                || jobSystemProperty.getKey().equals(
//                        Settings.KAFKA_T_CERTIFICATE)) {
//          flinkBuilder.addSystemProperty(jobSystemProperty.getKey(),
//                  "/srv/glassfish/domain1/config/" + jobSystemProperty.
//                          getValue());
//        } else {
        flinkBuilder.addSystemProperty(jobSystemProperty.getKey(),
            jobSystemProperty.getValue());
//        }
      }
    }
    try {
      runner = flinkBuilder.
          getYarnRunner(jobDescription.getProject().getName(),
              flinkUser, jobUser, hadoopDir, flinkDir, flinkConfDir,
              flinkConfFile, nameNodeIpPort, glassfishDomainDir + "/domain1/config/");

    } catch (IOException e) {
      LOG.log(Level.SEVERE,
          "Failed to create YarnRunner.", e);
      writeToLogs(new IOException("Failed to start Yarn client.", e));
      return false;
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(hadoopDir,
        jobDescription.
            getProject().
            getName())
        + Settings.FLINK_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(hadoopDir,
        jobDescription.
            getProject().
            getName())
        + Settings.FLINK_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    return true;
  }

  @Override
  protected void cleanup() {
    LOG.log(Level.INFO, "Job finished performing cleanup...");
    if (monitor != null) {
      monitor.close();
      monitor = null;
    }
    //Remove local files required for the job (Kafka certs etc.)
    //Search for other jobs using Kafka in the same project. If any active
    //ones are found

    Collection<ProjectServices> projectServices = jobDescription.getProject().
        getProjectServicesCollection();
    Iterator<ProjectServices> iter = projectServices.iterator();
    boolean removeKafkaCerts = true;
    while (iter.hasNext()) {
      ProjectServices projectService = iter.next();
      //If the project is of type KAFKA
      if (projectService.getProjectServicesPK().getService()
          == ProjectServiceEnum.KAFKA) {
        List<Execution> execs = services.getExecutionFacade().
            findForProjectByType(jobDescription.getProject(), JobType.FLINK);
        if (execs != null) {
          execs.addAll(services.getExecutionFacade().
              findForProjectByType(jobDescription.getProject(),
                  JobType.SPARK));
        }
        //Find if this project has running jobs
        if (execs != null && !execs.isEmpty()) {
          for (Execution exec : execs) {
            if (!exec.getState().isFinalState()) {
              removeKafkaCerts = false;
              break;
            }
          }
        }
      }
    }
    if (removeKafkaCerts) {
      String k_certName = jobDescription.getProject().getName() + "__"
          + jobDescription.getProject().getOwner().getUsername()
          + "__kstore.jks";
      String t_certName = jobDescription.getProject().getName() + "__"
          + jobDescription.getProject().getOwner().getUsername()
          + "__tstore.jks";
      File k_cert = new File(glassfishDomainDir + "/domain1/config/"
          + k_certName);
      File t_cert = new File(glassfishDomainDir + "/domain1/config/"
          + t_certName);
      if (k_cert.exists()) {
        k_cert.delete();
      }
      if (t_cert.exists()) {
        t_cert.delete();
      }
    }

  }

  @Override
  protected void stopJob(String appid) {
    //Stop flink cluster first
    try {
      Runtime rt = Runtime.getRuntime();
      Process pr = rt.exec(this.hadoopDir + "/bin/yarn application -kill "
          + appid);
    } catch (IOException ex1) {
      LOG.log(Level.SEVERE, "Unable to stop flink cluster with appID:"
          + appid, ex1);
    }
  }

}
