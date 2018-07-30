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

package io.hops.hopsworks.common.dao.jobs;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.jobs.jobhistory.JobDetailDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobHeuristicDTO;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;

@Stateless
public class JobsHistoryFacade extends AbstractFacade<JobsHistory> {

  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private DistributedFsService fileOperations;
  @EJB
  private ProjectFacade projectFacade;

  private static final Logger logger = Logger.getLogger(JobsHistoryFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  public JobsHistoryFacade() {
    super(JobsHistory.class);
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public JobsHistory findByAppId(String appId) {
    TypedQuery<JobsHistory> q = em.createNamedQuery("JobsHistory.findByAppId",
            JobsHistory.class);
    q.setParameter("appId", appId);

    List<JobsHistory> results = q.getResultList();
    if (results.isEmpty()) {
      return null;
    } else if (results.size() == 1) {
      return results.get(0);
    }

    return null;
  }

  public List<JobsHistory> findByProjectId(int projectId) {
    TypedQuery<JobsHistory> q = em.createNamedQuery(
            "JobsHistory.findByProjectId",
            JobsHistory.class);
    q.setParameter("projectId", projectId);

    return q.getResultList();
  }

  /**
   * Stores an instance in the database.
   * <p/>
   * @param user
   * @param jobDesc
   * @param executionId
   * @param appId
   */
  public void persist(Users user, Jobs jobDesc, int executionId,
          String appId) {
    SparkJobConfiguration configuration = (SparkJobConfiguration) jobDesc.
            getJobConfig();
    String inodePath = configuration.getAppPath();
    String patternString = "hdfs://(.*)\\s";
    Pattern p = Pattern.compile(patternString);
    Matcher m = p.matcher(inodePath);
    String[] parts = inodePath.split("/");
    String pathOfInode = inodePath.replace("hdfs://" + parts[2], "");

    Inode inode = inodeFacade.getInodeAtPath(pathOfInode);
    String jarFile = inode.getInodePK().getName();
    String blocks = checkArguments(configuration.getArgs());

    this.persist(jobDesc.getId(), jarFile, executionId, appId, jobDesc,
            blocks, configuration, user.getEmail());
  }

  public void persist(int jobId, String jarFile, int executionId, String appId,
          Jobs jobDesc, String inputBlocksInHdfs,
          SparkJobConfiguration configuration, String userEmail) {
    JobsHistory exist = em.find(JobsHistory.class, executionId);
    if (exist == null) {
      JobsHistory file = new JobsHistory(executionId, jobId, jarFile, appId,
              jobDesc,
              inputBlocksInHdfs, configuration, userEmail);
      em.persist(file);
      em.flush();
    }
  }

  /**
   * Updates a JobHistory instance with the duration and the application Id
   * <p/>
   * @param exec
   * @param duration
   * @return JobsHistory
   */
  public JobsHistory updateJobHistory(Execution exec, long duration) {
    JobsHistory obj = em.find(JobsHistory.class, exec.getId());
    if (obj != null) {
      obj.setAppId(exec.getAppId());
      obj.setExecutionDuration(duration);
      obj.setState(exec.getState());
      obj.setFinalStatus(exec.getFinalStatus());
      em.merge(obj);
    }
    return obj;
  }

  /**
   * Check the input arguments of a job. If it is a file then the method returns
   * the number of blocks that the file consists of.
   * Otherwise the method returns 0 block files.
   * <p/>
   * @param arguments
   * @return Number of Blocks
   */
  private String checkArguments(String arguments) {
    String blocks = "0";
    DistributedFileSystemOps dfso = null;
    if (arguments == null) {
      arguments = "-";
    }
    if (arguments.startsWith("hdfs://")) {
      try {
        dfso = fileOperations.getDfsOps();
        blocks = dfso.getFileBlocks(arguments);
      } catch (IOException ex) {
        Logger.getLogger(JobsHistoryFacade.class.getName()).log(Level.SEVERE,
                "Failed to find file at HDFS.", ex);
      } finally {
        if (dfso != null) {
          dfso.close();
        }
      }
    } else {
      blocks = "0";
    }

    return blocks;
  }

  /**
   * Given the initial arguments for the user, this method tries to find similar
   * jobs in the history.
   *
   * @param jobDetails
   * @return JobHeuristicDTO
   */
  public JobHeuristicDTO searchHeuristicRusults(JobDetailDTO jobDetails) {
    Project project = projectFacade.find(jobDetails.getProjectId());
    String projectName = project.getName();
    String userEmail = project.getOwner().getEmail();

    int blocks = Integer.parseInt(checkArguments(jobDetails.getInputArgs()));
    List<JobsHistory> resultsForAnalysis = searchForVeryHighSimilarity(
            jobDetails, userEmail, projectName);

    if (!resultsForAnalysis.isEmpty()) {
      JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis,
              jobDetails);
      jhDTO.setInputBlocks(blocks);
      jhDTO.addSimilarAppId(resultsForAnalysis);
      jhDTO.setDegreeOfSimilarity("VERY HIGH");
      return jhDTO;
    }

    resultsForAnalysis = searchForHighSimilarity(jobDetails, userEmail,
            projectName);

    if (!resultsForAnalysis.isEmpty()) {
      JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis,
              jobDetails);
      jhDTO.setInputBlocks(blocks);
      jhDTO.addSimilarAppId(resultsForAnalysis);
      jhDTO.setDegreeOfSimilarity("HIGH");
      return jhDTO;
    }

    resultsForAnalysis = searchForMediumSimilarity(jobDetails, userEmail,
            projectName);

    if (!resultsForAnalysis.isEmpty()) {
      JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis,
              jobDetails);
      jhDTO.setInputBlocks(blocks);
      jhDTO.addSimilarAppId(resultsForAnalysis);
      jhDTO.setDegreeOfSimilarity("MEDIUM");
      return jhDTO;
    }

    resultsForAnalysis = searchForLowSimilarity(jobDetails, userEmail,
            projectName);

    if (!resultsForAnalysis.isEmpty()) {
      JobHeuristicDTO jhDTO = analysisOfHeuristicResults(resultsForAnalysis,
              jobDetails);
      jhDTO.setInputBlocks(blocks);
      jhDTO.addSimilarAppId(resultsForAnalysis);
      jhDTO.setDegreeOfSimilarity("LOW");
      return jhDTO;

    } else {
      return new JobHeuristicDTO(0, "There are no results", "none", "NONE",
              blocks);
    }
  }

  // Very High Similarity -> Same jobType, className, jarFile, arguments and blocks
  // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
  private List<JobsHistory> searchForVeryHighSimilarity(JobDetailDTO jobDetails,
          String userEmail, String projectName) {
    if (jobDetails.isFilter()) {
      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithVeryHighSimilarityFilter", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("jarFile", jobDetails.getSelectedJar());
      q.setParameter("arguments", jobDetails.getInputArgs());
      q.setParameter("inputBlocksInHdfs", checkArguments(jobDetails.
              getInputArgs()));
      q.setParameter("projectName", projectName);
      q.setParameter("jobName", jobDetails.getJobName());
      q.setParameter("userEmail", userEmail);
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();

    } else {

      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithVeryHighSimilarity", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("jarFile", jobDetails.getSelectedJar());
      q.setParameter("arguments", jobDetails.getInputArgs());
      q.setParameter("inputBlocksInHdfs", checkArguments(jobDetails.
              getInputArgs()));
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();
    }
  }

  // High Similarity -> Same jobType, className, jarFile and arguments
  // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
  private List<JobsHistory> searchForHighSimilarity(JobDetailDTO jobDetails,
          String userEmail, String projectName) {
    if (jobDetails.isFilter()) {
      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithHighSimilarityFilter", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("jarFile", jobDetails.getSelectedJar());
      q.setParameter("arguments", jobDetails.getInputArgs());
      q.setParameter("projectName", projectName);
      q.setParameter("jobName", jobDetails.getJobName());
      q.setParameter("userEmail", userEmail);
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();
    } else {
      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithHighSimilarity", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("jarFile", jobDetails.getSelectedJar());
      q.setParameter("arguments", jobDetails.getInputArgs());
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();
    }
  }

  // Medium Similarity -> Same jobType, className and jarFile
  // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
  private List<JobsHistory> searchForMediumSimilarity(JobDetailDTO jobDetails,
          String userEmail, String projectName) {
    if (jobDetails.isFilter()) {
      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithMediumSimilarityFilter", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("jarFile", jobDetails.getSelectedJar());
      q.setParameter("projectName", projectName);
      q.setParameter("jobName", jobDetails.getJobName());
      q.setParameter("userEmail", userEmail);
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();
    } else {
      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithMediumSimilarity", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("jarFile", jobDetails.getSelectedJar());
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();
    }
  }

  // Low Similarity -> Same jobType, className
  // If Filter is true then we search by the above attributes + same job of a user (user + project + job)
  private List<JobsHistory> searchForLowSimilarity(JobDetailDTO jobDetails,
          String userEmail, String projectName) {
    if (jobDetails.isFilter()) {
      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithLowSimilarityFilter", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("projectName", projectName);
      q.setParameter("jobName", jobDetails.getJobName());
      q.setParameter("userEmail", userEmail);
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();
    } else {
      TypedQuery<JobsHistory> q = em.createNamedQuery(
              "JobsHistory.findWithLowSimilarity", JobsHistory.class);
      q.setParameter("jobType", jobDetails.getJobType());
      q.setParameter("className", jobDetails.getClassName());
      q.setParameter("finalStatus", JobFinalStatus.SUCCEEDED);

      return q.getResultList();
    }
  }

  private JobHeuristicDTO analysisOfHeuristicResults(
          List<JobsHistory> resultsForAnalysis, JobDetailDTO jobDetails) {
    String estimatedTime = estimateCompletionTime(resultsForAnalysis);
    int numberOfResults = resultsForAnalysis.size();
    String message = "Analysis of the results.";

    return new JobHeuristicDTO(numberOfResults, message, estimatedTime,
            jobDetails.getProjectId(), jobDetails.getJobName(), jobDetails.
            getJobType());
  }

  /**
   * Calculates the average of completion time for a bunch of Heuristic results
   *
   * @param resultsForAnalysis
   * @return
   */
  private String estimateCompletionTime(List<JobsHistory> resultsForAnalysis) {
    long milliseconds = 0;
    long avegareMs;
    Iterator<JobsHistory> itr = resultsForAnalysis.iterator();

    while (itr.hasNext()) {
      JobsHistory element = itr.next();
      milliseconds = milliseconds + element.getExecutionDuration();
    }
    avegareMs = milliseconds / resultsForAnalysis.size();

    String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(
            avegareMs),
            TimeUnit.MILLISECONDS.toMinutes(avegareMs) - TimeUnit.HOURS.
            toMinutes(TimeUnit.MILLISECONDS.toHours(avegareMs)),
            TimeUnit.MILLISECONDS.toSeconds(avegareMs) - TimeUnit.MINUTES.
            toSeconds(TimeUnit.MILLISECONDS.toMinutes(avegareMs)));

    return hms;
  }

}
