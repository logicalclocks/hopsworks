/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jobs;

import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateTerminated;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.jobs.ExecutionJWT;
import io.hops.hopsworks.common.jobs.JobsMonitor;
import io.hops.hopsworks.common.jobs.execution.ExecutionUpdateController;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeJobsMonitor implements JobsMonitor {
  
  private static final Logger LOGGER = Logger.getLogger(KubeJobsMonitor.class.getName());
  
  @EJB
  KubeClientService kubeClientService;
  @EJB
  private JobsJWTManager jobsJWTManager;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private ExecutionUpdateController executionUpdateController;
  @EJB
  private PayaraClusterManager payaraClusterManager;
  @EJB
  private YarnLogUtil yarnLogUtil;
  @Resource
  private TimerService timerService;
  private Timer timer;
  
  @PostConstruct
  public void init() {
    //number of milliseconds that must elapse between timer expiration notifications
    long intervalDuration = 5000L; // 5 sec
    timer = timerService.createIntervalTimer(0, intervalDuration, new TimerConfig("Kube Jobs Monitor",
      false));
  }
  
  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }
  
  @Timeout
  public synchronized void monitorKubeJobs(Timer timer) {
    if (!payaraClusterManager.amIThePrimary()) {
      return;
    }
    LOGGER.log(Level.FINE, "Running KubeJobsMonitor timer");
    try {
      // Get all non-finished executions of type Python or Docker, if they don't exist in kubernetes, set them to failed
      List<Execution> pendingExecutions = executionFacade.findByTypesAndStates(
        Stream.of(JobType.DOCKER, JobType.PYTHON).collect(Collectors.toSet()),
        JobState.getKubeRunningStates());
      
      Map<String, String> label = new HashMap<>();
      label.put("deployment-type", "job");
      List<Pod> pods = kubeClientService.getPods(label);
      // Go through all fetched jobs and update state accordingly
      for (Pod pod : pods) {
        int executionId = Integer.parseInt(pod.getMetadata().getLabels().get("execution"));
        if (executionFacade.findById(executionId).isPresent()) {
          Execution execution = executionFacade.findById(executionId).get();
          pendingExecutions.remove(execution);
          LOGGER.log(Level.FINEST, "Execution: " + execution + ", with state:" + execution.getState());
          if (execution.getState() == JobState.FINISHED
            || execution.getState() == JobState.FAILED
            || execution.getState() == JobState.KILLED) {
            cleanUpExecution(execution, pod);
          } else {
            //Get the app container
            for (ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
              if (containerStatus.getName().equals(JobType.PYTHON.getName().toLowerCase())
                || containerStatus.getName().equals(JobType.DOCKER.getName().toLowerCase())) {
                ContainerState containerState = containerStatus.getState();
                if (containerState.getTerminated() != null) {
                  ContainerStateTerminated containerStateTerminated = containerState.getTerminated();
                  String reason = containerStateTerminated.getReason();
                  String message = containerStateTerminated.getMessage();
                  Integer exitCode = containerStateTerminated.getExitCode();
                  JobState jobState = KubeJobType.getAsJobState(reason);
                  execution = updateState(jobState, execution);
                  cleanUpExecution(execution, pod);
                  // Exitcode 0 is successful execution and logs comes from the container
                  // Exitcode 1 is application failure and logs comes from container
                  if (exitCode != 0 &&
                    exitCode != 1 &&
                    !Strings.isNullOrEmpty(reason)) {
                    LOGGER.log(Level.FINEST, "reason: " + reason + ", pod: " + pod);
                    // Write log in Logs dataset
                    DistributedFileSystemOps udfso = null;
                    try {
                      udfso = dfs.getDfsOps(execution.getHdfsUser());
                      String logMessage = "Job was terminated with" +
                        " docker exit code: " + exitCode +
                        ", Reason: " + reason;
                      if (!Strings.isNullOrEmpty(message)) {
                        logMessage += ", Message: " + message;
                      }
                      if (exitCode == 137) { //137 is the exit code for when a docker container was killed due to oom
                        logMessage += "\n\nTry increasing the memory for the job.";
                      }
                      yarnLogUtil.writeLog(udfso, execution.getStderrPath(), logMessage);
                    } finally {
                      if (udfso != null) {
                        dfs.closeDfsClient(udfso);
                      }
                    }
                  }
                } else if (containerState.getWaiting() != null) {
                  String reason = containerState.getWaiting().getReason();
                  String message = containerState.getWaiting().getMessage();
                  // The timeout cannot be set individually per job, it has to be done in kubelet. This is a more
                  // flexible way to fail a waiting app and get the logs
                  if (!Strings.isNullOrEmpty(reason) &&
                    !reason.equals("ContainerCreating") &&
                    execution.getExecutionDuration() > Settings.PYTHON_JOB_KUBE_WAITING_TIMEOUT_MS) {
                    LOGGER.log(Level.FINEST, "reason: " + containerState + ", pod: " + pod);
                    JobState jobState = KubeJobType.getAsJobState(reason);
                    execution = updateState(jobState, execution);
                    cleanUpExecution(execution, pod);
                    // Write log in Logs dataset
                    DistributedFileSystemOps udfso = null;
                    try {
                      udfso = dfs.getDfsOps(execution.getHdfsUser());
                      yarnLogUtil.writeLog(udfso, execution.getStderrPath(),
                        "Job failed with: " + reason + " - " + message);
                    } finally {
                      if (udfso != null) {
                        dfs.closeDfsClient(udfso);
                      }
                    }
                  }
                } else {
                  updateState(JobState.RUNNING, execution);
                }
              }
            }
          }
          LOGGER.log(Level.FINEST, "Execution: " + execution + ", state:" + execution.getState());
        } else {
          LOGGER.log(Level.WARNING, "Execution with id: " + executionId + " not found");
          kubeClientService.deleteJob(pod.getMetadata().getNamespace(), pod.getMetadata().getLabels()
            .get("job-name"));
        }
      }
      if (!pendingExecutions.isEmpty()) {
        for (Execution execution : pendingExecutions) {
          // If execution in pending but job was not submitted yet to kubernetes as it is asynchronous, the job would
          // be marked as failed. Therefore we give some margin to kubernetes to actually start the job.
          if (execution.getExecutionDuration() > 20000) {
            updateState(JobState.FAILED, execution);
            updateFinalStatus(JobFinalStatus.FAILED, execution);
            updateExecutionStop(System.currentTimeMillis(), execution);
            updateProgress(1, execution);
          }
        }
      }
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "KubeJobsMonitor exception", ex);
    }
  }
  
  private void cleanUpExecution(Execution execution, Pod pod) {
    
    LOGGER.log(Level.FINEST, "Execution: " + execution + ", with state:" + execution.getState() + ", pod: " + pod);
    if (execution.getExecutionStop() < 1) {
      execution = updateExecutionStop(System.currentTimeMillis(), execution);
      execution = updateProgress(1, execution);
      execution = executionUpdateController.
        updateFinalStatusAndSendAlert(KubeJobType.getAsJobFinalStatus(execution.getState()), execution);
    }
    jobsJWTManager.cleanJWT(new ExecutionJWT(execution));
    kubeClientService.deleteJob(pod.getMetadata().getNamespace(), pod.getMetadata().getLabels()
      .get("job-name"));
  }
  
  @Override
  public Execution updateProgress(float progress, Execution execution) {
    return executionUpdateController.updateProgress(progress, execution);
  }
  
  @Override
  public Execution updateState(JobState newState, Execution execution) {
    return executionUpdateController.updateState(newState, execution);
  }
  
  public Execution updateExecutionStop(Long executionStop, Execution execution) {
    return executionUpdateController.updateExecutionStop(executionStop, execution);
  }
  
  private Execution updateFinalStatus(JobFinalStatus finalStatus, Execution execution) {
    return executionUpdateController.updateFinalStatusAndSendAlert(finalStatus, execution);
  }
  
  private enum KubeJobType {
    FAILED("Failed"), // taken from kubernetes fabric8 API
    COMPLETED("Completed"); //taken from kubernetes fabric8 API
    
    private final String name;
    
    KubeJobType(String name) {
      this.name = name;
    }
    
    @Override
    public String toString() {
      return name;
    }
    
    public static JobState getAsJobState(String kubeJobType) {
      if (!Strings.isNullOrEmpty(kubeJobType) && kubeJobType.equals(KubeJobType.COMPLETED.name)) {
        return JobState.FINISHED;
      }
      return JobState.FAILED;
    }
    
    public static JobFinalStatus getAsJobFinalStatus(JobState jobState) {
      if (jobState.equals(JobState.FAILED)) {
        return JobFinalStatus.FAILED;
      } else if (jobState.equals(JobState.KILLED)) {
        return JobFinalStatus.KILLED;
      }
      return JobFinalStatus.SUCCEEDED;
    }
  }
}
