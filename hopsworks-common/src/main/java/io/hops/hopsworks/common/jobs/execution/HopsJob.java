/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.jobs.execution;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.security.UserGroupInformation;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.yarn.ServiceProperties;
import io.hops.hopsworks.common.jobs.yarn.YarnJobsMonitor;
import org.apache.hadoop.yarn.client.api.YarnClient;

/**
 * Contains the execution logic of a Hops job. This class takes care of the main
 * flow control of the application. The job is started by calling the execute()
 * method. Note that this method is blocking. Before this, however, the client
 * must have requested an Execution id by calling the appropriate method on
 * this class. HopsJob then sets up and runs the actual job and finally
 * allows for some cleanup. This class takes care of execution time tracking as
 * well.
 * <p/>
 * Three abstract methods are provided for overriding:
 * <ul>
 * <li>setupJob() - This method is called after an Execution id has been
 * procured. </li>
 * <li>runJob() - This method is called after setupJob() finishes. </li>
 * <li>cleanup() - This method is called after runJob() finishes. </li>
 * </ul>
 * <p/>
 * The calls to each of these methods are blocking.
 */
public abstract class HopsJob {

  protected YarnJobsMonitor jobsMonitor;
  private static final Logger logger = Logger.getLogger(HopsJob.class.getName());
  protected Execution execution;
  private boolean initialized = false;

  //Service provider providing access to facades
  protected final AsynchronousJobExecutor services;
  protected ServiceProperties serviceProps;
  protected final Jobs jobs;
  protected final Users user;
  protected final String hadoopDir;
  protected final UserGroupInformation hdfsUser;

  /**
   * Create a HopsJob instance.
   * <p/>
   * @param jobs The Jobs to be executed.
   * @param services A service provider giving access to several execution
   * services.
   * @param user The user executing this job.
   * @param hadoopDir base Hadoop installation directory
   * @param jobsMonitor
   * @throws NullPointerException If either of the given arguments is null.
   */
  protected HopsJob(Jobs jobs,
          AsynchronousJobExecutor services, Users user, String hadoopDir,
          YarnJobsMonitor jobsMonitor) throws
          NullPointerException {
    //Check validity
    if (jobs == null) {
      throw new NullPointerException("Cannot run a null Job.");
    } else if (services == null) {
      throw new NullPointerException("Cannot run without a service provider.");
    } else if (user == null) {
      throw new NullPointerException("A job cannot be run by a null user!");
    }
    //We can safely proceed
    this.jobs = jobs;
    this.services = services;
    this.user = user;
    this.hadoopDir = hadoopDir;
    this.jobsMonitor = jobsMonitor;
    try {
      //if HopsJob is created in a doAs UserGroupInformation.getCurrentUser()
      //will return the proxy user, if not it will return the superuser.  
      hdfsUser = UserGroupInformation.getCurrentUser();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new IllegalArgumentException(
              "Exception while trying to retrieve hadoop User Group Information: "
              + ex.getMessage());
    }
    logger.log(Level.INFO, "Instantiating Hops job as user: {0}", hdfsUser);
  }


  /**
   * Update the current state of the Execution entity to the given state.
   * <p/>
   * @param newState
   */
  protected final void updateState(JobState newState) {
    execution = services.getExecutionFacade().updateState(execution, newState);
  }
  
  /**
   * Execute the job and keep track of its execution time. The execution flow is
   * outlined in the class documentation. Internally, this method calls
   * setupJob(), runJob() and cleanup() in that order.
   * This method is blocking.
   * <p/>
   * @throws IllegalStateException If no Execution id has been requested yet.
   */
  public final void execute() throws IllegalStateException {
    if (!initialized) {
      throw new IllegalStateException(
              "Cannot execute before acquiring an Execution id.");
    }
    try {
      this.hdfsUser.doAs(new PrivilegedExceptionAction<Void>() {
        @Override
        public Void run() {
          DistributedFileSystemOps dfso = null;
          DistributedFileSystemOps udfso = null;
          YarnClientWrapper yarnClientWrapper = null;
          try {
            execution = services.getExecutionFacade().updateExecutionStart(execution, System.currentTimeMillis());
            dfso = services.getFsService().getDfsOps();
            udfso = services.getFileOperations(hdfsUser.getUserName());
            yarnClientWrapper = services.getYarnClientService()
                .getYarnClient(hdfsUser.getUserName());
            boolean proceed = setupJob(dfso, yarnClientWrapper.getYarnClient());
            if (!proceed) {
              execution = services.getExecutionFacade().updateExecutionStop(execution, System.currentTimeMillis());
              services.getExecutionFacade().updateState(execution, JobState.INITIALIZATION_FAILED);
              cleanup();
              return null;
            } else {
              updateState(JobState.STARTING_APP_MASTER);
            }
            runJob(udfso, dfso);
            return null;
          } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception while trying to get hdfsUser name for execution " + execution, e);
          } finally {
            if (dfso != null) {
              dfso.close();
            }
            if (null != udfso) {
              services.getFsService().closeDfsClient(udfso);
            }
            if (yarnClientWrapper != null) {
              services.getYarnClientService().closeYarnClient(yarnClientWrapper);
            }
          }
          return null;
        }
      });
    } catch (IOException | InterruptedException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  public final void stop(String appid) throws IllegalStateException {
    stopJob(appid);
  }

  /**
   * Called before runJob, should setup the job environment to allow it to be
   * run.
   * <p/>
   * @param dfso
   * @return False if execution should be aborted. Cleanup() is still executed
   * in that case.
   */
  protected abstract boolean setupJob(DistributedFileSystemOps dfso,
      YarnClient yarnClient);

  /**
   * Takes care of the execution of the job. Called by execute() after
   * setupJob(), if that method indicated to be successful.
   * Note that this method should update the Execution object correctly and
   * persistently, since this object is used to check the status of (running)
   * jobs.
   *
   * @param udfso
   * @param dfso
   */
  protected abstract void runJob(DistributedFileSystemOps udfso,
          DistributedFileSystemOps dfso);

  protected abstract void stopJob(String appid);

  /**
   * Called after runJob() completes, allows the job to perform some cleanup, if
   * necessary.
   */
  protected abstract void cleanup();

  /**
   * Request a unique execution id by creating an Execution object. Creates
   * and persists an Execution object. The object is in the state INITIALIZING.
   * Upon success, returns the created Execution object to allow tracking.
   * This method must be called before attempting to run the actual execution.
   * <p/>
   * @return Unique Execution object associated with this job.
   */
  public final Execution requestExecutionId() {
    execution = services.getExecutionFacade().create(jobs, user, null, null, null, null, 0, hdfsUser.
        getUserName());
    initialized = (execution.getId() != null);
    return execution;
  }
  
  /**
   * Check whether the HopsJob was initialized correctly, {@literal i.e.} if an
   * Execution id has been acquired.
   * <p/>
   * @return
   */
  public final boolean isInitialized() {
    return initialized;
  }

  /**
   * Write an Exception message to the application logs.
   * <p/>
   * @param message
   * @param e
   * @throws java.io.IOException
   */
  protected abstract void writeToLogs(String message, Exception e) throws IOException;
  
  protected abstract void writeToLogs(String message) throws IOException;
}
