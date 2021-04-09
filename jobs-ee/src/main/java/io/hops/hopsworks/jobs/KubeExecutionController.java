/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jobs;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.LifecycleBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobSpec;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.YarnAppUrlsDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.jobs.JobLogDTO;
import io.hops.hopsworks.common.jobs.execution.AbstractExecutionController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.kafka.KafkaBrokers;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DockerJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FilenameUtils;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
  private static final String CERTS = "certs";
  private static final String HADOOP_CONF = "hadoopconf";
  private static final String JWT = "jwt";
  private static final String JWT_SUFFIX = SEPARATOR + "jwt";
  // Flink is here because of Beam when running Beam portable runner
  private static final String FLINK = "flink";
  private static final String SPARK = "spark";

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
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private KubeProjectConfigMaps kubeProjectConfigMaps;
  
  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Execution start(Jobs job, String args, Users user) throws JobException, GenericException,
    ServiceException, ProjectException {
    
    if (job.getJobType() == JobType.PYTHON || job.getJobType() == JobType.DOCKER) {
      Project project = job.getProject();
      String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
      Execution execution = executionFacade.create(job, user, null, null, null, 0, hdfsUser, args);
      String logOutputPath = Utils.getJobLogLocation(execution.getJob().getProject().getName(),
        execution.getJob().getJobType())[0] + job.getName() + File.separator + execution.getId() + File.separator;
      execution.setStdoutPath(logOutputPath + "stdout.log");
      execution.setStderrPath(logOutputPath + "stderr.log");
      execution.setExecutionStart(System.currentTimeMillis());
      execution = executionFacade.update(execution);

      DockerJobConfiguration jobConfiguration = (DockerJobConfiguration)job.getJobConfig();
      try {
        String secretsDir = "/srv/hops/secrets";
        String certificatesDir = "/srv/hops/certificates";

        if (job.getJobType() == JobType.PYTHON) {
          ((PythonJobConfiguration) job.getJobConfig()).setImagePath(
                  projectUtils.getFullDockerImageName(project, false));

          DistributedFileSystemOps udfso = null;
          try {
            udfso = dfs.getDfsOps(hdfsUser);
            if (!Strings.isNullOrEmpty(((PythonJobConfiguration) jobConfiguration).getFiles())) {
              for (String filePath : ((PythonJobConfiguration) jobConfiguration).getFiles().split(",")) {
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
          String appPath = ((PythonJobConfiguration) job.getJobConfig()).getAppPath();
          if (appPath.endsWith(".ipynb")) {
            executionFacade.updateState(execution, JobState.CONVERTING_NOTEBOOK);
            String outPath = "hdfs://" + Utils.getProjectPath(job.getProject().getName())
                    + Settings.PROJECT_STAGING_DIR;
            String pyAppPath = outPath + "/job_tmp_" + job.getName() + ".py";
            ((PythonJobConfiguration) job.getJobConfig()).setAppPath(pyAppPath);
            jupyterController.convertIPythonNotebook(hdfsUser, appPath, job.getProject(), pyAppPath,
                    jupyterController.getNotebookConversionType(appPath, user, job.getProject()));
          }
        }
        String kubeProjectUser = kubeClientService.getKubeDeploymentName(job.getProject(), user);
        String secretsName = kubeClientService.getKubeDeploymentName(execution) + JWT_SUFFIX;

        String deploymentName = job.getJobType().getName().toLowerCase()
                + SEPARATOR + kubeClientService.getKubeDeploymentName(execution);

        jobsJWTManager.materializeJWT(user, job.getProject(), execution);

        kubeProjectConfigMaps.reloadConfigMaps(project);

        ResourceRequirements resourceRequirements = kubeClientService.buildResourceRequirements(jobConfiguration);
        Map<String, String> primaryContainerEnv = new HashMap<>();
        Map<String, String> sidecarContainerEnv = new HashMap<>();
        setContainerEnv(job.getJobType(), primaryContainerEnv, sidecarContainerEnv, hdfsUser, project, execution,
                        certificatesDir, secretsDir, settings.getAnacondaProjectDir(), jobConfiguration, hdfsUser);

        List<Container> containers = buildContainers(job.getJobType(), jobConfiguration, secretsDir,
                                                     certificatesDir, resourceRequirements, primaryContainerEnv,
                                                     sidecarContainerEnv, project);
        kubeClientService.createJob(job.getProject(),
          buildJob(
            deploymentName,
            secretsName,
            kubeProjectUser,
            execution,
            project,
            job.getJobType(),
            containers,
            jobConfiguration));
      } catch (Exception e) {
        execution.setExecutionStop(System.currentTimeMillis());
        execution.setProgress(1);
        execution.setState(JobState.FAILED);
        executionFacade.update(execution);

        String usrMsg = "";
        if (e instanceof EJBException &&
          ((EJBException) e).getCausedByException() instanceof KubernetesClientException) {
          usrMsg = "Reason: " +
            ((KubernetesClientException) ((EJBException) e).getCausedByException()).getStatus().getMessage();
        } else if (e instanceof JobException) {
          usrMsg = ((JobException)e).getErrorCode().getMessage();
        } else {
          usrMsg = e.getMessage();
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
        throw new JobException(RESTCodes.JobErrorCode.JOB_START_FAILED, FINE, "Job:" + job.getName() + ", " + usrMsg,
                null, e);
      }
      return execution;
    }
    return super.start(job, args, user);
  }
  
  private Job buildJob(String name, String secretsName, String kubeProjectUser,
                       Execution execution, Project project, JobType jobType, List<Container> containers,
                       DockerJobConfiguration jobConfiguration) throws JobException {
    // We build the spec like this since we want to set the backofflimit
    // https://github.com/fabric8io/kubernetes-model/issues/239#issuecomment-376420931
    JobSpec jobSpec = new JobSpec();
    jobSpec.setParallelism(1);
    jobSpec.setAdditionalProperty("backoffLimit", 0);
    jobSpec.setTemplate(new PodTemplateSpecBuilder()
      .withMetadata(
        new ObjectMetaBuilder()
          .withLabels(ImmutableMap.of(jobType.getName().toLowerCase(), kubeProjectUser,
            "execution", Integer.toString(execution.getId()),
            "job-type", jobType.getName().toLowerCase(),
            "deployment-type", "job"))
          .build())
      .withSpec(buildPodSpec(project, secretsName, kubeProjectUser, containers, jobConfiguration))
      .build());
    
    Job job = new JobBuilder()
      .withNewMetadata()
      .withName(name)
      .withLabels(ImmutableMap.of(jobType.getName().toLowerCase(), kubeProjectUser,
        "execution", Integer.toString(execution.getId()),
        "job-type", jobType.getName().toLowerCase(),
        "deployment-type", "job"))
      .endMetadata()
      .build();
  
    job.setSpec(jobSpec);
    return job;
  }
  
  private PodSpec buildPodSpec(Project project, String secretsName,
      String kubeProjectUser, List<Container> containers, DockerJobConfiguration jobConfiguration) throws JobException {
    List<Volume> volumes = new ArrayList<>();
    volumes.add(new VolumeBuilder()
            .withName(CERTS)
            .withSecret(
                    new SecretVolumeSourceBuilder()
                            .withSecretName(kubeProjectUser)
                            .build())
            .build());
    volumes.add(new VolumeBuilder()
            .withName(HADOOP_CONF)
            .withConfigMap(
                    new ConfigMapVolumeSourceBuilder()
                            .withName(kubeProjectConfigMaps.getHadoopConfigMapName(project))
                            .build())
            .build());
    volumes.add(new VolumeBuilder()
            .withName(JWT)
            .withSecret(
                    new SecretVolumeSourceBuilder()
                            .withSecretName(secretsName)
                            .build())
            .build());

    volumes.add(new VolumeBuilder()
            .withName("logs")
            .withEmptyDir(new EmptyDirVolumeSource())
            .build());
    parseVolumes(jobConfiguration.getVolumes()).stream().map(Pair::getValue0).forEach(volumes::add);

    return new PodSpecBuilder()
      .withContainers(containers)
      .withVolumes(volumes)
      .withRestartPolicy("Never")
      .build();
  }
  
  private List<Container> buildContainers(JobType jobType,
                                          DockerJobConfiguration jobConfiguration,
                                          String secretDir,
                                          String certificatesDir,
                                          ResourceRequirements resourceRequirements,
                                          Map<String, String> primaryContainerEnv,
                                          Map<String, String> sidecarContainerEnv,
                                          Project project)
          throws ServiceDiscoveryException, JobException {

    List<Container> containers = new ArrayList<>();
    if (jobType == JobType.PYTHON) {
      //Add Job container
      containers.add(new ContainerBuilder()
              .withName(jobType.getName().toLowerCase())
              .withImage(projectUtils.getFullDockerImageName(project, false))
              .withImagePullPolicy(settings.getKubeImagePullPolicy())
              .withResources(resourceRequirements)
              .withSecurityContext(new SecurityContextBuilder().withRunAsUser(settings.getYarnAppUID()).build())
              .withEnv(kubeClientService.getEnvVars(primaryContainerEnv))
              .withCommand("python-exec.sh")
              .withVolumeMounts(
                      new VolumeMountBuilder()
                              .withName(CERTS)
                              .withReadOnly(true)
                              .withMountPath(certificatesDir)
                              .build(),
                      new VolumeMountBuilder()
                              .withName(HADOOP_CONF)
                              .withReadOnly(true)
                              .withMountPath(settings.getHadoopConfDir())
                              .build(),
                      new VolumeMountBuilder()
                              .withName(JWT)
                              .withReadOnly(true)
                              .withMountPath(secretDir)
                              .build(),
                      new VolumeMountBuilder()
                              .withName("logs")
                              .withMountPath("/app/logs")
                              .build())
              .build());

      containers.add(new ContainerBuilder()
              .withName("filebeat")
              .withImage(ProjectUtils.getRegistryURL(settings,
                      serviceDiscoveryController) + "/filebeat:" + settings.getHopsworksVersion())
              .withImagePullPolicy(settings.getKubeImagePullPolicy())
              .withEnv(kubeClientService.getEnvVars(sidecarContainerEnv))
              .withVolumeMounts(
                      new VolumeMountBuilder()
                              .withName("logs")
                              .withMountPath("/app/logs")
                              .build())
              .build());
    } else if (jobType == JobType.DOCKER) {

      List<VolumeMount> volumeMounts = new ArrayList<>();
      volumeMounts.add(new VolumeMountBuilder()
              .withName(CERTS)
              .withReadOnly(true)
              .withMountPath(certificatesDir)
              .build());
      volumeMounts.add(new VolumeMountBuilder()
              .withName(JWT)
              .withReadOnly(true)
              .withMountPath(secretDir)
              .build());
      volumeMounts.add(new VolumeMountBuilder()
              .withName("logs")
              .withMountPath(jobConfiguration.getOutputPath())
              .build());

      // Parse volumes from job and create mounts
      parseVolumes(jobConfiguration.getVolumes()).stream().map(Pair::getValue1).forEach(volumeMounts::add);

      // Validate uid and gid
      Long uid = null;
      Long gid = null;
      SecurityContext sc = null;
      if (!settings.isDockerJobUidStrict()) {
        if (jobConfiguration.getUid() != null) {
          uid = jobConfiguration.getUid();
        }
        if (jobConfiguration.getGid() != null) {
          gid = jobConfiguration.getGid();
        }
        sc = new SecurityContextBuilder().withRunAsUser(uid).withRunAsGroup(gid).build();
      } else if (settings.isDockerJobUidStrict() && (jobConfiguration.getUid() != null
                                                  || jobConfiguration.getGid() != null)) {
        throw new JobException(RESTCodes.JobErrorCode.DOCKER_UID_GID_STRICT, FINE);
      }

      //Add Job container
      containers.add(new ContainerBuilder()
              .withName(jobType.getName().toLowerCase())
              .withImage(jobConfiguration.getImagePath())
              .withImagePullPolicy(settings.getKubeImagePullPolicy())
              .withResources(resourceRequirements)
              .withSecurityContext(sc)
              .withEnv(kubeClientService.getEnvVars(primaryContainerEnv))
              .withCommand(jobConfiguration.getCommand())
              .withArgs(jobConfiguration.getArgs())
              .withVolumeMounts(volumeMounts)
              .build());

      containers.add(new ContainerBuilder()
              .withName("logger")
              .withImage(projectUtils.getFullDockerImageName(project, true))
              .withImagePullPolicy(settings.getKubeImagePullPolicy())
              .withEnv(kubeClientService.getEnvVars(sidecarContainerEnv))
              .withCommand("/bin/sh")
              .withArgs("-c","while true; do sleep 1; done")
              .withVolumeMounts(
                      new VolumeMountBuilder()
                              .withName(CERTS)
                              .withReadOnly(true)
                              .withMountPath(certificatesDir)
                              .build(),
                      new VolumeMountBuilder()
                              .withName(HADOOP_CONF)
                              .withReadOnly(true)
                              .withMountPath("/srv/hops/hadoop/etc/hadoop")
                              .build(),
                      new VolumeMountBuilder()
                              .withName("logs")
                              .withMountPath(jobConfiguration.getOutputPath())
                              .build())
              .withLifecycle(new LifecycleBuilder()
                      .withNewPreStop()
                        .withNewExec()
                        .withCommand("/bin/sh", "-c",
                                     "hdfs dfs -mkdir -p " + jobConfiguration.getOutputPath() + " ; " +
                                     "hdfs dfs -copyFromLocal -f " + jobConfiguration.getOutputPath() + "/* "
                                                                   + jobConfiguration.getOutputPath())
                        .endExec()
                      .endPreStop()
                      .build())
              .build());

    }
    return containers;
  }

  private void setContainerEnv(JobType jobType, Map<String, String> primaryContainer,
                                              Map<String, String> sideContainer, String hadoopUser, Project project,
                                              Execution execution, String certificatesDir, String secretsDir,
                                              String anacondaEnv, DockerJobConfiguration dockerJobConfiguration,
                                              String hdfsUser)
          throws ServiceDiscoveryException, IOException {
    switch (jobType) {
      case PYTHON:
        primaryContainer.put("SPARK_HOME", settings.getSparkDir());
        primaryContainer.put("SPARK_CONF_DIR", settings.getSparkConfDir());
        primaryContainer.put("ELASTIC_ENDPOINT", settings.getElasticRESTEndpoint());
        primaryContainer.put("HADOOP_VERSION", settings.getHadoopVersion());
        primaryContainer.put("HOPSWORKS_VERSION", settings.getHopsworksVersion());
        primaryContainer.put("TENSORFLOW_VERSION", settings.getTensorflowVersion());
        primaryContainer.put("KAFKA_VERSION", settings.getKafkaVersion());
        primaryContainer.put("SPARK_VERSION", settings.getSparkVersion());
        primaryContainer.put("LIVY_VERSION", settings.getLivyVersion());
        primaryContainer.put("HADOOP_HOME", settings.getHadoopSymbolicLinkDir());
        primaryContainer.put("HADOOP_HDFS_HOME", settings.getHadoopSymbolicLinkDir());
        primaryContainer.put("HADOOP_USER_NAME", hadoopUser);

        String jobName = execution.getJob().getName();
        String executionId = String.valueOf(execution.getId());
        primaryContainer.put("HOPSWORKS_JOB_NAME", jobName);
        primaryContainer.put("HOPSWORKS_JOB_EXECUTION_ID", executionId);
        primaryContainer.put("HOPSWORKS_JOB_TYPE", execution.getJob().getJobConfig().getJobType().getName());
        primaryContainer.put("HOPSWORKS_LOGS_DATASET", Settings.BaseDataset.LOGS.getName());

        if (!Strings.isNullOrEmpty(kafkaBrokers.getKafkaBrokersString())) {
          primaryContainer.put("KAFKA_BROKERS", kafkaBrokers.getKafkaBrokersString());
        }
        Service hopsworks =
                serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
                        ServiceDiscoveryController.HopsworksService.HOPSWORKS_APP);
        primaryContainer.put("REST_ENDPOINT", "https://" + hopsworks.getName() + ":" + hopsworks.getPort());
        primaryContainer.put(Settings.SPARK_PYSPARK_PYTHON, settings.getAnacondaProjectDir() + "/bin/python");
        primaryContainer.put("HOPSWORKS_PROJECT_ID", Integer.toString(project.getId()));
        primaryContainer.put("REQUESTS_VERIFY", String.valueOf(settings.getRequestsVerify()));
        primaryContainer.put("DOMAIN_CA_TRUSTSTORE",
                Paths.get(certificatesDir, hadoopUser + Settings.TRUSTSTORE_SUFFIX).toString());
        primaryContainer.put("SECRETS_DIR", secretsDir);
        primaryContainer.put("CERTS_DIR", certificatesDir);
        primaryContainer.put("FLINK_CONF_DIR", settings.getFlinkConfDir());
        primaryContainer.put("FLINK_LIB_DIR", settings.getFlinkLibDir());
        primaryContainer.put("HADOOP_CLASSPATH_GLOB", settings.getHadoopClasspathGlob());
        primaryContainer.put("SPARK_CONF_DIR", settings.getSparkConfDir());
        primaryContainer.put("ANACONDA_ENV", anacondaEnv);
        primaryContainer.put("APP_PATH", ((PythonJobConfiguration)dockerJobConfiguration).getAppPath());
        primaryContainer.put("APP_FILE",
                                FilenameUtils.getName(((PythonJobConfiguration)dockerJobConfiguration).getAppPath()));
        primaryContainer.put("APP_ARGS", execution.getArgs());
        primaryContainer.put("APP_FILES", ((PythonJobConfiguration)dockerJobConfiguration).getFiles());


        sideContainer.put("LOGPATH", "/app/logs/*");
        sideContainer.put("LOGSTASH", getLogstashURL());
        sideContainer.put("JOB", jobName);
        sideContainer.put("EXECUTION", executionId);
        sideContainer.put("PROJECT", project.getName().toLowerCase());
        break;
      case DOCKER:
        Map<String, String> envVars = HopsUtils.parseUserProperties(String.join("\n",
                                                                                dockerJobConfiguration.getEnvVars()));
        primaryContainer.putAll(envVars);

        sideContainer.put("HADOOP_CONF_DIR", settings.getHadoopConfDir());
        sideContainer.put("HADOOP_CLIENT_OPTS", "-Dfs.permissions.umask-mode=0007");
        sideContainer.put("MATERIAL_DIRECTORY", certificatesDir);
        sideContainer.put("HADOOP_USER_NAME", hdfsUser);
        break;
      default:
        throw new UnsupportedOperationException("Job type not supported: " + jobType);
    }
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
    if (execution.getJob().getJobType() == JobType.PYTHON || execution.getJob().getJobType() == JobType.DOCKER) {
      return stopExecution(execution);
    }
    return super.stopExecution(execution);
  }
  
  @Override
  public Execution stopExecution(Execution execution) throws JobException {
    Optional<Exception> t = Optional.empty();
    // Set state to failed as execution was terminated by user
    t = runCatchAndLog(() -> executionFacade.updateState(execution, JobState.KILLED),
      RESTCodes.JobErrorCode.JOB_STOP_FAILED.getMessage(), t);
    if (t.isPresent()) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_STOP_FAILED, SEVERE,
        "Job: " + execution.getJob().getName() + ", Execution: " + execution.getId(), null, t.get());
    }
    return execution;
  }
  
  @Override
  public void delete(Execution execution) throws JobException {
    if (execution.getJob().getJobType() == JobType.PYTHON || execution.getJob().getJobType() == JobType.DOCKER) {
      stopExecution(execution);
    } else {
      super.stopExecution(execution);
    }
    super.delete(execution);
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
  
  private String getLogstashURL() throws ServiceDiscoveryException {
    com.logicalclocks.servicediscoverclient.service.Service logstash =
        serviceDiscoveryController
            .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.PYTHON_JOBS_LOGSTASH);
    return logstash.getAddress() + ":" + logstash.getPort();
  }

  private List<Pair<Volume, VolumeMount>> parseVolumes(List<String> volumes) throws JobException {
    if (!settings.isDockerJobMountAllowed()) {
      throw new JobException(RESTCodes.JobErrorCode.DOCKER_MOUNT_NOT_ALLOWED, FINE);
    }
    List<Pair<Volume, VolumeMount>> volumeMounts = new ArrayList<>();
    if (volumes != null && !volumes.isEmpty()) {
      int i = 0;
      for (String volume : volumes) {
        String hostPath = volume.split(":")[0];
        // Check if the user-provided volume is allowed
        if (!settings.getDockerMountsList().contains(hostPath)) {
          throw new JobException(RESTCodes.JobErrorCode.DOCKER_MOUNT_DIR_NOT_ALLOWED, FINE, "path:" + hostPath);
        }

        String name = "vol" + i++;
        volumeMounts.add(new Pair<>(
                new VolumeBuilder()
                        .withName(name)
                        .withHostPath(
                                new HostPathVolumeSourceBuilder()
                                        .withPath(hostPath)
                                        .build())
                        .build(),
                new VolumeMountBuilder()
                        .withName(name)
                        .withMountPath(volume.split(":")[1])
                        .withReadOnly(true)
                        .build()));
      }
    }
    return volumeMounts;
  }
}
