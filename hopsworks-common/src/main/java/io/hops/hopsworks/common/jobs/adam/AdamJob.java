/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.jobs.adam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.spark.SparkJob;
import io.hops.hopsworks.common.jobs.spark.SparkYarnRunnerBuilder;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.yarn.client.api.YarnClient;

public class AdamJob extends SparkJob {

  private static final Logger LOG = Logger.getLogger(AdamJob.class.getName());

  private final AdamJobConfiguration jobconfig;
  private final Jobs jobDescription;
  private final String adamJarPath;
  
  public AdamJob(Jobs job,
      AsynchronousJobExecutor services, Users user, String hadoopDir,
      String jobUser, String adamJarPath, YarnJobsMonitor jobsMonitor, Settings settings) {
    super(job, services, user, hadoopDir, jobUser, jobsMonitor, settings);
    if (!(job.getJobConfig() instanceof AdamJobConfiguration)) {
      throw new IllegalArgumentException(
          "JobDescription must contain a AdamJobConfiguration object. Received: "
          + job.getJobConfig().getClass());
    }
    this.jobDescription = job;
    this.jobconfig = (AdamJobConfiguration) job.getJobConfig();
    this.adamJarPath = adamJarPath;
  }

  @Override
  protected void runJob(DistributedFileSystemOps udfso,
      DistributedFileSystemOps dfso) {
    //Try to start the AM
    boolean proceed = startApplicationMaster(udfso, dfso);
    //If success: monitor running job
    if (!proceed) {
      return;
    }
    jobsMonitor.addToMonitor(execution.getAppId(), execution, monitor);
   
  }

  @Override
  protected boolean setupJob(DistributedFileSystemOps dfso, YarnClient yarnClient) {
    //Get to starting the job
    List<String> missingArgs = checkIfRequiredPresent(jobconfig); //thows an IllegalArgumentException if not ok.
    if (!missingArgs.isEmpty()) {
      try {
        writeToLogs(
            "Cannot execute ADAM command because some required arguments are missing: "
            + missingArgs);
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Failed to write logs for failed application.", ex);
      }
      return false;
    }

    //Then: submit ADAM job
    if (jobconfig.getAppName() == null || jobconfig.getAppName().isEmpty()) {
      jobconfig.setAppName("Untitled ADAM Job");
    }
    jobconfig.setMainClass(Settings.ADAM_MAINCLASS);
    jobconfig.setAppPath(adamJarPath);
    jobDescription.setJobConfig(jobconfig);
    
    runnerbuilder = new SparkYarnRunnerBuilder(jobDescription);
    super.setupJob(dfso, yarnClient);
    //Set some ADAM-specific property values   
    runnerbuilder.addSystemProperty("spark.serializer",
        "org.apache.spark.serializer.KryoSerializer");
    runnerbuilder.addSystemProperty("spark.kryo.registrator",
        "org.bdgenomics.adam.serialization.ADAMKryoRegistrator");
    runnerbuilder.addSystemProperty("spark.kryoserializer.buffer", "4m");
    runnerbuilder.addSystemProperty("spark.kryo.referenceTracking", "true");

    runnerbuilder.addAllJobArgs(constructArgs(jobconfig));

    //Add ADAM jar to local resources
    runnerbuilder.addExtraFile(new LocalResourceDTO(adamJarPath.substring(
        adamJarPath.
            lastIndexOf("/") + 1), adamJarPath,
        LocalResourceVisibility.PUBLIC.toString(),
        LocalResourceType.FILE.toString(), null));
    //Set the job name
    runnerbuilder.setJobName(jobconfig.getAppName());

    try {
      runner = runnerbuilder.
              getYarnRunner(jobDescription.getProject().getName(),
                      jobUser, services, services.getFileOperations(hdfsUser.getUserName()), yarnClient, settings);
    } catch (IOException e) {
      LOG.log(Level.SEVERE,
          "Failed to create YarnRunner.", e);
      try {
        writeToLogs("Failed to start Yarn client", e);
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Failed to write logs for failed application.", e);
      }
      return false;
    }

    String stdOutFinalDestination = Utils.getHdfsRootPath(jobDescription.getProject().getName())
        + Settings.ADAM_DEFAULT_OUTPUT_PATH;
    String stdErrFinalDestination = Utils.getHdfsRootPath(jobDescription.getProject().getName())
        + Settings.ADAM_DEFAULT_OUTPUT_PATH;
    setStdOutFinalDestination(stdOutFinalDestination);
    setStdErrFinalDestination(stdErrFinalDestination);
    return true;
  }

  /**
   * Check if all required arguments have been filled in.
   * <p/>
   * @return A list of missing argument names. If the list is empty, all
   * required arguments are present.
   */
  private List<String> checkIfRequiredPresent(AdamJobConfiguration ajc) throws
      IllegalArgumentException {
    List<String> missing = new ArrayList<>();

    for (AdamArgumentDTO arg : ajc.getSelectedCommand().getArguments()) {
      if (arg.isRequired() && (arg.getValue() == null || arg.getValue().
          isEmpty())) {
        //Required argument is missing
        missing.add(arg.getName());
      }
    }
    return missing;
  }

  private List<String> constructArgs(AdamJobConfiguration ajc) {
    List<String> adamargs = new ArrayList<>();
    //First: add command
    adamargs.add(ajc.getSelectedCommand().getCommand());
    //Loop over arguments
    for (AdamArgumentDTO arg : ajc.getSelectedCommand().getArguments()) {
      adamargs.add(arg.getValue());
    }
    //Loop over options
    for (AdamOptionDTO opt : ajc.getSelectedCommand().getOptions()) {
      if (opt.isFlag()) {
        //flag: just add the name of the flag
        if (opt.getSet()) {
          adamargs.add(opt.toAdamOption().getCliVal());
        }
      } else if (opt.getValue() != null && !opt.getValue().isEmpty()) {
        //Not a flag: add the name of the option
        adamargs.add(opt.toAdamOption().getCliVal());
        adamargs.add(opt.getValue());
      }
    }
    return adamargs;
  }

  @Override
  protected void cleanup() {
    //Nothing to be done, really.
  }

  @Override
  protected void stopJob(String appid) {
    super.stopJob(appid);
  }

}
