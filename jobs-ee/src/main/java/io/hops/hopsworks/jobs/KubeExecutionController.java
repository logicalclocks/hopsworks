/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jobs;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.kubernetes.api.model.JobBuilder;
import io.fabric8.kubernetes.api.model.JobSpec;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.JobLogDTO;
import io.hops.hopsworks.common.jobs.execution.AbstractExecutionController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.kafka.KafkaBrokers;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.tensorflow.TfLibMappingUtil;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FilenameUtils;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

/**
 * Takes care of booting the execution of a job.
 */
@Stateless
@KubeStereotype
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeExecutionController extends AbstractExecutionController implements ExecutionController {
  
  private static final Logger LOGGER = Logger.getLogger(KubeExecutionController.class.getName());
  
  private static final String SEPARATOR = "-";
  private static final String ANACONDA = "anaconda";
  private static final String CERTS = "certs";
  private static final String HADOOP = "hadoop";
  private static final String PYTHON = "python";
  private static final String PYTHON_NVIDIA = "python_nvidia";
  private static final String PYTHON_PREFIX = PYTHON + SEPARATOR;
  
  private static final String JWT = "jwt";
  private static final String JWT_SUFFIX = SEPARATOR + "jwt";
  // Flink is here because of Beam when running Beam portable runner
  private static final String FLINK = "flink";
  private static final String SPARK = "spark";
  @EJB
  TfLibMappingUtil tfLibMappingUtil;
  
  
  @EJB
  private KubeClientService kubeClientService;
  @EJB
  private Settings settings;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private JobsJWTManager jobsJWTManager;
  @EJB
  private KafkaBrokers kafkaBrokers;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private DistributedFsService dfs;
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Execution start(Jobs job, String args, Users user) throws JobException, GenericException,
    ServiceException, ProjectException {
    
    if (job.getJobType() == JobType.PYTHON) {
      Project project = job.getProject();
      String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
      Execution execution = executionFacade.create(job, user, null, null, null, 0, hdfsUser, args);
      String[] logOutputPaths = Utils.getJobLogLocation(execution.getJob().getProject().getName(),
        execution.getJob().getJobType());
      String logSuffix = job.getName() + File.separator + execution.getId() + File.separator;
      execution.setStdoutPath(logOutputPaths[0] + logSuffix + "stdout.log");
      execution.setStderrPath(logOutputPaths[0] + logSuffix + "stderr.log");
      execution.setExecutionStart(System.currentTimeMillis());
      execution = executionFacade.update(execution);
      try {
        String secretDir = "/srv/hops/secrets";
        String certificatesDir = "/srv/hops/certificates";
        PythonJobConfiguration pythonJobConfiguration = ((PythonJobConfiguration) job.getJobConfig());
  
        DistributedFileSystemOps udfso = null;
        try {
          udfso = dfs.getDfsOps(hdfsUser);
          if (!Strings.isNullOrEmpty(pythonJobConfiguration.getFiles())) {
            for (String filePath : pythonJobConfiguration.getFiles().split(",")) {
              if (!Strings.isNullOrEmpty(filePath) && !udfso.exists(filePath)) {
                throw new ProjectException(RESTCodes.ProjectErrorCode.FILE_NOT_FOUND, Level.FINEST,
                  "Attached file does not exist: " + filePath);
              }
            }
          }
        } finally {
          if (udfso != null) {
            dfs.closeDfsClient(udfso);
          }
        }
        
        //If it is a notebook we need to convert it to a .py file every time the job is run
        String appPath = pythonJobConfiguration.getAppPath();
        if (appPath.endsWith(".ipynb")) {
          String outPath = "hdfs://" + Utils.getProjectPath(job.getProject().getName()) + Settings.PROJECT_STAGING_DIR;
          String pyAppPath = outPath + "/job_tmp_" + job.getName() + ".py";
          pythonJobConfiguration.setAppPath(pyAppPath);
          jupyterController.convertIPythonNotebook(hdfsUser, appPath, job.getProject(), pyAppPath,
            JupyterController.NotebookConversion.PY_JOB);
        }

        String kubeProjectUser = kubeClientService.getKubeDeploymentName(job.getProject(), user);
        String secretsName = kubeClientService.getKubeDeploymentName(execution) + JWT_SUFFIX;
        String deploymentName = PYTHON_PREFIX + kubeClientService.getKubeDeploymentName(execution);
        String anacondaProjectDir = settings.getAnacondaProjectDir(job.getProject());
        jobsJWTManager.materializeJWT(user, job.getProject(), execution);
        kubeClientService.createJob(job.getProject(),
          buildJob(
            deploymentName,
            secretsName,
            kubeProjectUser,
            anacondaProjectDir,
            secretDir,
            certificatesDir,
            hdfsUser,
            settings.getFlinkConfDir(),
            pythonJobConfiguration.getAppPath(),
            execution.getArgs(),
            pythonJobConfiguration,
            execution,
            project));
        
      } catch (Exception e) {
        executionFacade.updateState(execution, JobState.FAILED);
        String usrMsg = "";
        if (e instanceof EJBException &&
          ((EJBException) e).getCausedByException() instanceof KubernetesClientException) {
          usrMsg = "Reason: " +
            ((KubernetesClientException) ((EJBException) e).getCausedByException()).getStatus().getMessage();
        }
        // Write log in Logs dataset
        DistributedFileSystemOps udfso = null;
        try {
          udfso = dfs.getDfsOps(hdfsUser);
          YarnLogUtil.writeLog(udfso, execution.getStderrPath(), usrMsg, e);
        } finally {
          if (udfso != null) {
            dfs.closeDfsClient(udfso);
          }
        }
        throw new JobException(RESTCodes.JobErrorCode.JOB_START_FAILED, SEVERE, "Job:" + job.getName(), usrMsg, e);
        
      }
      return execution;
    }
    return super.start(job, args, user);
  }
  
  private Job buildJob(String name, String secretsName, String kubeProjectUser, String anacondaEnv,
    String secretsDir,
    String certificatesDir, String hadoopUser, String flinkConfDir, String appPath, String args,
    PythonJobConfiguration pythonJobConfiguration, Execution execution, Project project) throws ServiceException {
  
    ResourceRequirements resourceRequirements = kubeClientService.buildResourceRequirements(pythonJobConfiguration);
    Map<String, String> jobEnv = new HashMap<>();
    jobEnv.put("SPARK_HOME", settings.getSparkDir());
    jobEnv.put("SPARK_CONF_DIR", settings.getSparkConfDir());
    jobEnv.put("ELASTIC_ENDPOINT", settings.getElasticRESTEndpoint());
    jobEnv.put("HADOOP_VERSION", settings.getHadoopVersion());
    jobEnv.put("HOPSWORKS_VERSION", settings.getHopsworksVersion());
    jobEnv.put("CUDA_VERSION", settings.getCudaVersion());
    jobEnv.put("TENSORFLOW_VERSION", settings.getTensorflowVersion());
    jobEnv.put("KAFKA_VERSION", settings.getKafkaVersion());
    jobEnv.put("SPARK_VERSION", settings.getSparkVersion());
    jobEnv.put("LIVY_VERSION", settings.getLivyVersion());
    jobEnv.put("HADOOP_HOME", settings.getHadoopSymbolicLinkDir());
    jobEnv.put("HADOOP_HDFS_HOME", settings.getHadoopSymbolicLinkDir());
    jobEnv.put("HADOOP_USER_NAME", hadoopUser);
    jobEnv.put("LD_LIBRARY_PATH", settings.getJavaHome() +
      "/jre/lib/amd64/server" + File.pathSeparator + tfLibMappingUtil.getTfLdLibraryPath(project) +
      settings.getHadoopSymbolicLinkDir() + "/lib/native" + File.pathSeparator + anacondaEnv
      + "/lib/");

    String jobName = execution.getJob().getName();
    String executionId = String.valueOf(execution.getId());
    jobEnv.put("HOPSWORKS_JOB_NAME", jobName);
    jobEnv.put("HOPSWORKS_JOB_EXECUTION_ID", executionId);
    jobEnv.put("HOPSWORKS_JOB_TYPE", execution.getJob().getJobConfig().getJobType().getName());
    jobEnv.put("HOPSWORKS_LOGS_DATASET", Settings.BaseDataset.LOGS.getName());
  
    if(!Strings.isNullOrEmpty(kafkaBrokers.getKafkaBrokersString())) {
      jobEnv.put("KAFKA_BROKERS", kafkaBrokers.getKafkaBrokersString());
    }
    jobEnv.put("REST_ENDPOINT", settings.getRestEndpoint());
    jobEnv.put(Settings.SPARK_PYSPARK_PYTHON, settings.getAnacondaProjectDir(project) + "/bin/python");
    jobEnv.put("HOPSWORKS_PROJECT_ID", Integer.toString(project.getId()));
    jobEnv.put("REQUESTS_VERIFY", String.valueOf(settings.getRequestsVerify()));
    jobEnv.put( "DOMAIN_CA_TRUSTSTORE_PEM",
      settings.getSparkConfDir() + File.separator + Settings.DOMAIN_CA_TRUSTSTORE_PEM);
    jobEnv.put( "SECRETS_DIR", secretsDir);
    jobEnv.put( "CERTS_DIR", certificatesDir);
    jobEnv.put("FLINK_CONF_DIR", flinkConfDir);
    jobEnv.put("SPARK_CONF_DIR", settings.getSparkConfDir());
    jobEnv.put("ANACONDA_ENV", anacondaEnv);
    jobEnv.put("APP_PATH", appPath);
    jobEnv.put("APP_FILE", FilenameUtils.getName(appPath));
    jobEnv.put("APP_ARGS", args);
    jobEnv.put("APP_FILES", pythonJobConfiguration.getFiles());
  
  
    Map<String, String> filebeatEnv = new HashMap<>();
    filebeatEnv.put("LOGPATH", "/app/logs/*");
    filebeatEnv.put("LOGSTASH", settings.getLogstashIp() + ":" + settings.getLogstashPortPythonJobs());
    filebeatEnv.put("JOB", jobName);
    filebeatEnv.put("EXECUTION", executionId);
    filebeatEnv.put("PROJECT", project.getName().toLowerCase());
    
    List<Container> containers = buildContainers(secretsDir, certificatesDir, flinkConfDir, resourceRequirements,
      jobEnv, filebeatEnv);
  
    //Selector is disabled due to https://github.com/kubernetes/kubernetes/issues/26202 and
    // changing api spec resulted in https://github.com/kubernetes/website/issues/2325
    //.withSelector(new LabelSelectorBuilder()
    //  .addToMatchLabels(PYTHON, kubeProjectUser)
    // .build())
  
    // We build the spec like this since we want to set the backofflimit
    // https://github.com/fabric8io/kubernetes-model/issues/239#issuecomment-376420931
    JobSpec jobSpec = new JobSpec();
    jobSpec.setParallelism(1);
    jobSpec.setAdditionalProperty("backoffLimit", 0);
    jobSpec.setTemplate(new PodTemplateSpecBuilder()
      .withMetadata(
        new ObjectMetaBuilder()
          .withLabels(ImmutableMap.of(PYTHON, kubeProjectUser,
            "execution", Integer.toString(execution.getId()),
            "job-type", PYTHON,
            "deployment-type", "job"))
          .build())
      .withSpec(buildPodSpec(secretsName, kubeProjectUser, anacondaEnv, containers))
      .build());
    
    Job job = new JobBuilder()
      .withNewMetadata()
      .withName(name)
      .withLabels(ImmutableMap.of(PYTHON, kubeProjectUser,
        "execution", Integer.toString(execution.getId()),
        "job-type", PYTHON,
        "deployment-type", "job"))
      .endMetadata()
      .build();
  
    job.setSpec(jobSpec);
    return job;
  }
  
  private PodSpec buildPodSpec(String secretsName, String kubeProjectUser, String anacondaEnv,
    List<Container> containers) {
    return new PodSpecBuilder()
      .withContainers(containers)
      .withVolumes(
        new VolumeBuilder()
          .withName(ANACONDA)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(anacondaEnv)
              .build())
          .build(),
        new VolumeBuilder()
          .withName(CERTS)
          .withSecret(
            new SecretVolumeSourceBuilder()
              .withSecretName(kubeProjectUser)
              .build())
          .build(),
        new VolumeBuilder()
          .withName(HADOOP)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(settings.getHadoopSymbolicLinkDir())
              .build())
          .build(),
        new VolumeBuilder()
          .withName(JWT)
          .withSecret(
            new SecretVolumeSourceBuilder()
              .withSecretName(secretsName)
              .build())
          .build(),
        new VolumeBuilder()
          .withName(FLINK)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(settings.getFlinkConfDir())
              .build())
          .build(),
        new VolumeBuilder()
          .withName(SPARK)
          .withHostPath(
            new HostPathVolumeSourceBuilder()
              .withPath(settings.getSparkConfDir())
              .build())
          .build(),
        new VolumeBuilder()
          .withName("logs")
          .withEmptyDir(new EmptyDirVolumeSource())
          .build())
      .withRestartPolicy("Never")
      .build();
  }
  
  private List<Container> buildContainers(String secretDir, String certificatesDir, String flinkConfDir,
    ResourceRequirements resourceRequirements, Map<String, String> jobEnv, Map<String, String> filebeatEnv) {
  
    List<Container> containers = new ArrayList<>();
    VolumeMount logMount = new VolumeMountBuilder()
      .withName("logs")
      .withMountPath("/app/logs")
      .build();
    
    //Add Job container
    containers.add(new ContainerBuilder()
      .withName(PYTHON)
      .withImage(settings.getKubeRegistry() + "/" + getDockerImageName(resourceRequirements) + ":"
        + settings.getPythonImgVersion())
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withResources(resourceRequirements)
      .withEnv(kubeClientService.getEnvVars(jobEnv))
      .withVolumeMounts(
        new VolumeMountBuilder()
          .withName(ANACONDA)
          .withReadOnly(true)
          .withMountPath("/srv/hops/anaconda/env")
          .build(),
        new VolumeMountBuilder()
          .withName(CERTS)
          .withReadOnly(true)
          .withMountPath(certificatesDir)
          .build(),
        new VolumeMountBuilder()
          .withName(HADOOP)
          .withReadOnly(true)
          .withMountPath("/srv/hops/hadoop")
          .build(),
        new VolumeMountBuilder()
          .withName(JWT)
          .withReadOnly(true)
          .withMountPath(secretDir)
          .build(),
        new VolumeMountBuilder()
          .withName(FLINK)
          .withReadOnly(true)
          .withMountPath(flinkConfDir)
          .build(),
        new VolumeMountBuilder()
          .withName(SPARK)
          .withReadOnly(true)
          .withMountPath(settings.getSparkConfDir())
          .build(),
        logMount  )
      .build());
    
    containers.add(new ContainerBuilder()
      .withName("filebeat")
      .withImage(settings.getKubeRegistry() + "/filebeat:" + settings.getKubeFilebeatImgVersion())
      .withImagePullPolicy(settings.getKubeImagePullPolicy())
      .withEnv(kubeClientService.getEnvVars(filebeatEnv))
      .withVolumeMounts(logMount)
      .build());
    
    return containers;
  }
  
  private String getDockerImageName(ResourceRequirements resourceRequirements) {
    if (resourceRequirements.getLimits().containsKey("nvidia.com/gpu")) {
      int requestedGPUs = Double.valueOf(resourceRequirements.getLimits().get("nvidia.com/gpu").getAmount()).intValue();
      if (requestedGPUs > 0) {
        return PYTHON_NVIDIA;
      }
    }
    return PYTHON;
  }
  
  private Optional<Exception> runCatchAndLog(Runnable runnable, String errorMessage,
    Optional<Exception> previousError) {
    try {
      runnable.run();
    } catch (Exception e) {
      LOGGER.log(SEVERE, errorMessage, e);
      return previousError.isPresent() ? previousError : Optional.of(e);
    }
    return previousError;
  }
  
  @Override
  public Execution stop(Jobs job) throws JobException {
    return super.stop(job);
  }
  
  @Override
  public Execution stopExecution(Integer id) throws JobException {
    Execution execution =
      executionFacade.findById(id).orElseThrow(() -> new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND,
        FINE, "Execution: " + id));
    if (execution.getJob().getJobType() == JobType.PYTHON) {
      return stopExecution(execution);
    }
    return super.stopExecution(execution);
  }
  
  @Override
  public Execution stopExecution(Execution execution) throws JobException {
    Optional<Exception> t = Optional.empty();
//    t = runCatchAndLog(() -> kubeClientService.deleteExecution(PYTHON_PREFIX, execution),
//      RESTCodes.JobErrorCode.JOB_STOP_FAILED.getMessage(), t);
    // Set state to failed as execution was terminated by user
    t = runCatchAndLog(() -> executionFacade.updateState(execution, JobState.KILLED),
      RESTCodes.JobErrorCode.JOB_STOP_FAILED.getMessage(), t);
//    t = runCatchAndLog(() -> jobsJWTManager.cleanJWT(new ExecutionJWT(execution)),
//      RESTCodes.JobErrorCode.JOB_STOP_FAILED.getMessage(), t);
    if (t.isPresent()) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_STOP_FAILED, SEVERE,
        "Job: " + execution.getJob().getName() + ", Execution: " + execution.getId(), null, t.get());
    }
    return execution;
  }
  
  @Override
  public JobLogDTO retryLogAggregation(Execution execution, JobLogDTO.LogType type) throws JobException {
    if (execution.getJob().getJobType() != JobType.PYTHON) {
      return super.retryLogAggregation(execution, type);
    }
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void checkAccessRight(String appId, Project project) throws JobException {
    super.checkAccessRight(appId, project);
  }
  
  @Override
  public List<YarnAppUrlsDTO> getTensorBoardUrls(Users user, String appId, Project project) throws JobException {
    return super.getTensorBoardUrls(user, appId, project);
  }
  
}
