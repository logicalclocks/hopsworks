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
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.api.model.HostPathVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.LifecycleBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
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
import io.hops.hopsworks.common.serving.ServingConfig;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;
import io.hops.hopsworks.kube.project.KubeProjectConfigMaps;
import io.hops.hopsworks.kube.security.KubeApiKeyUtils;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DockerJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.configuration.python.PythonJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.GlassfishTags;
import io.hops.hopsworks.servicediscovery.tags.LogstashTags;
import io.hops.hopsworks.servicediscovery.tags.NamenodeTags;
import io.hops.hopsworks.servicediscovery.tags.OpenSearchTags;
import org.apache.commons.io.FilenameUtils;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  @EJB
  private KubeApiKeyUtils kubeApiKeyUtils;
  @Inject
  private ServingConfig servingConfig;

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public Execution start(Jobs job, String args, Users user) throws JobException, GenericException,
          ServiceException, ProjectException {

    enforceParallelExecutionsQuota(job.getProject());

    // If the limit for the number of executions for this job has been reached, return an error
    checkExecutionLimit(job);

    if (job.getJobType() == JobType.PYTHON || job.getJobType() == JobType.DOCKER) {
      Project project = job.getProject();
      String hdfsUser = hdfsUsersController.getHdfsUserName(project, user);
      Execution execution = executionFacade.create(job, user, null, null, null, 0, hdfsUser, args);
      String logPath = getLogsPath(job, execution);
      execution.setStdoutPath(logPath + "/stdout.log");
      execution.setStderrPath(logPath + "/stderr.log");
      execution.setExecutionStart(System.currentTimeMillis());
      execution = executionFacade.update(execution);

      DockerJobConfiguration jobConfiguration = (DockerJobConfiguration)job.getJobConfig();
      String originalParentDir = null;
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

            //If it is a notebook we need to convert it to a .py file every time the job is run
            String appPath = ((PythonJobConfiguration) job.getJobConfig()).getAppPath();
            if (appPath.endsWith(".ipynb")) {
              originalParentDir = (new File(appPath)).getParent();
              executionFacade.updateState(execution, JobState.CONVERTING_NOTEBOOK);
              String pyAppPath = HopsUtils.prepJupyterNotebookConversion(execution, hdfsUser, dfs);
              ((PythonJobConfiguration) job.getJobConfig()).setAppPath(pyAppPath);
              jupyterController.convertIPythonNotebook(project, user, appPath, pyAppPath,
                  JupyterController.NotebookConversion.PY_JOB);
            }
          } finally {
            if (udfso != null) {
              dfs.closeDfsClient(udfso);
            }
          }
        }
        String kubeProjectUser = kubeClientService.getKubeDeploymentName(job.getProject(), user);
        String secretsName = kubeClientService.getKubeDeploymentName(execution) + JWT_SUFFIX;

        String deploymentName = job.getJobType().getName().toLowerCase()
                + SEPARATOR + kubeClientService.getKubeDeploymentName(execution);

        jobsJWTManager.materializeJWT(user, job.getProject(), execution);

        kubeProjectConfigMaps.reloadConfigMaps(project);

        ResourceRequirements resourceRequirements = kubeClientService.
          buildResourceRequirements(jobConfiguration.getResourceConfig());

        List<EnvVar> primaryContainerEnv = getPrimaryContainerEnv(job.getJobType(), user, hdfsUser, project, execution,
          certificatesDir, secretsDir, settings.getAnacondaProjectDir(), jobConfiguration, resourceRequirements);
        List<EnvVar> sidecarContainerEnv = getSidecarContainerEnv(job.getJobType(), project, execution,
          certificatesDir, hdfsUser);

        if (originalParentDir != null) {
          originalParentDir = Utils.prepPath(originalParentDir);
          originalParentDir = originalParentDir.replaceFirst(Utils.getProjectPath(project.getName()), "");
          primaryContainerEnv.add(new EnvVarBuilder().withName("ORIGINAL_APP_FILE_DIR").withValue(originalParentDir)
              .build());
        }
        
        List<Container> containers = buildContainers(job.getJobType(), jobConfiguration, secretsDir,
                certificatesDir, resourceRequirements, primaryContainerEnv,
                sidecarContainerEnv, project, job, execution);
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
        execution.setFinalStatus(JobFinalStatus.FAILED);
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
      .withMetadata(getPodMetadata(kubeProjectUser, execution))
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
                               String kubeProjectUser, List<Container> containers,
                               DockerJobConfiguration jobConfiguration) throws JobException {
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

    if (!Strings.isNullOrEmpty(jobConfiguration.getOutputPath())) {
      volumes.add(new VolumeBuilder()
              .withName("output")
              .withEmptyDir(new EmptyDirVolumeSource())
              .build());
    }

    if (jobConfiguration.getInputPaths() != null && !jobConfiguration.getInputPaths().isEmpty()) {
      List<String> inputPaths = jobConfiguration.getInputPaths();
      for (int i = 0, inputPathsSize = inputPaths.size(); i < inputPathsSize; i++) {
        if (!Strings.isNullOrEmpty(inputPaths.get(i))) {
          volumes.add((new VolumeBuilder()
                  .withName("inputpath" + i)
                  .withEmptyDir(new EmptyDirVolumeSource())
                  .build()));
        }
      }
    }
    parseVolumes(jobConfiguration).stream().map(Pair::getValue0).forEach(volumes::add);

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
                                          List<EnvVar> primaryContainerEnv,
                                          List<EnvVar> sidecarContainerEnv,
                                          Project project,
                                          Jobs job,
                                          Execution execution)
          throws ServiceDiscoveryException, JobException {

    List<Container> containers = new ArrayList<>();
    if (jobType == JobType.PYTHON) {
      if (settings.getMountHopsfsInJobContainer()) {
        ResourceRequirementsBuilder resourceRequirementsBuilder = new ResourceRequirementsBuilder(resourceRequirements);
        resourceRequirementsBuilder.addToLimits("smarter-devices/fuse", new QuantityBuilder()
            .withAmount(Integer.toString(1)).build());
        resourceRequirementsBuilder.addToRequests("smarter-devices/fuse", new QuantityBuilder()
            .withAmount(Integer.toString(1)).build());
        resourceRequirements = resourceRequirementsBuilder.build();
      }
      //Add Job container
      containers.add(new ContainerBuilder()
              .withName(jobType.getName().toLowerCase())
              .withImage(projectUtils.getFullDockerImageName(project, false))
              .withImagePullPolicy(settings.getKubeImagePullPolicy())
              .withResources(resourceRequirements)
              .withSecurityContext(new SecurityContextBuilder().withRunAsUser(settings.getYarnAppUID()).build())
              .withEnv(primaryContainerEnv)
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
              .withEnv(sidecarContainerEnv)
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
              .withMountPath(getLogsPath(job, execution))
              .build());
      if (!Strings.isNullOrEmpty(jobConfiguration.getOutputPath())) {
        volumeMounts.add(new VolumeMountBuilder()
              .withName("output")
              .withMountPath(jobConfiguration.getOutputPath())
              .build());
      }

      if (jobConfiguration.getInputPaths() != null && !jobConfiguration.getInputPaths().isEmpty()) {
        List<String> inputPaths = jobConfiguration.getInputPaths();
        for (int i = 0; i < inputPaths.size(); i++) {
          if (!Strings.isNullOrEmpty(inputPaths.get(i))) {
            volumeMounts.add(new VolumeMountBuilder()
                    .withName("inputpath" + i)
                    .withMountPath(inputPaths.get(i))
                    .build());
          }
        }
      }

      // Parse volumes from job and create mounts
      parseVolumes(jobConfiguration).stream().map(Pair::getValue1).forEach(volumeMounts::add);

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

      String stdLogsPath = " 2> " + execution.getStderrPath() + " 1> " + execution.getStdoutPath();
      String args = null;
      if (!Strings.isNullOrEmpty(jobConfiguration.getDefaultArgs())) {
        args = jobConfiguration.getDefaultArgs();
        if (jobConfiguration.getLogRedirection()) {
          args += stdLogsPath;
        }
      } else if (!Strings.isNullOrEmpty(execution.getArgs())) {
        args = execution.getArgs();
        if (jobConfiguration.getLogRedirection()) {
          args += stdLogsPath;
        }
      } else if (jobConfiguration.getLogRedirection()) {
        String lastCommand = jobConfiguration.getCommand().get(jobConfiguration.getCommand().size() - 1);
        lastCommand += stdLogsPath;
        jobConfiguration.getCommand().set(jobConfiguration.getCommand().size() - 1, lastCommand);
      }

      //Add Job container
      containers.add(new ContainerBuilder()
              .withName(jobType.getName().toLowerCase())
              .withImage(jobConfiguration.getImagePath())
              .withImagePullPolicy(settings.getKubeImagePullPolicy())
              .withResources(resourceRequirements)
              .withSecurityContext(sc)
              .withEnv(primaryContainerEnv)
              .withCommand(jobConfiguration.getCommand())
              .withArgs(args)
              .withVolumeMounts(volumeMounts)
              .build());

      String logsPath = getLogsPath(job, execution);

      Path path = Paths.get(logsPath);
      String directory = path.getParent().toString();
      String copyCmd = "hdfs dfs -mkdir -p " + logsPath + " ; " +
                       "hdfs dfs -copyFromLocal -f " + logsPath + " " + directory + " ; ";

      if (!Strings.isNullOrEmpty(jobConfiguration.getOutputPath())) {
        path = Paths.get(jobConfiguration.getOutputPath());
        directory = path.getParent().toString();
        copyCmd += "hdfs dfs -mkdir -p " + jobConfiguration.getOutputPath() + " ; " +
                   "hdfs dfs -copyFromLocal -f " + jobConfiguration.getOutputPath() + " " +
                    directory;
      }

      LifecycleBuilder lifecycleBuilder = new LifecycleBuilder()
                      .withNewPreStop()
                      .withNewExec()
                      .withCommand("/bin/sh", "-c", copyCmd)
                      .endExec()
                      .endPreStop();

      if (jobConfiguration.getInputPaths() != null && !jobConfiguration.getInputPaths().isEmpty()) {
        String command = "echo \"" + String.join("\n", jobConfiguration.getInputPaths()) +
                "\" | while read line ; do hdfs dfs -test -d $line  && hdfs dfs -copyToLocal -f $line/* $line " +
                "&&  chmod +rx -R $line ; done";

        LOGGER.log(Level.INFO, "command:" + command);
        lifecycleBuilder
                .withNewPostStart()
                .withNewExec()
                .withCommand("/bin/sh", "-c", command)
                .endExec()
                .endPostStart();
      }
      List<VolumeMount> jobVolumeMounts = new ArrayList<>();
      jobVolumeMounts.add(new VolumeMountBuilder()
                      .withName(CERTS)
                      .withReadOnly(true)
                      .withMountPath(certificatesDir)
                      .build());
      jobVolumeMounts.add(new VolumeMountBuilder()
                      .withName(HADOOP_CONF)
                      .withReadOnly(true)
                      .withMountPath("/srv/hops/hadoop/etc/hadoop")
                      .build());
      jobVolumeMounts.add(new VolumeMountBuilder()
                      .withName("logs")
                      .withMountPath(getLogsPath(job, execution))
              .build());
      if (!Strings.isNullOrEmpty(jobConfiguration.getOutputPath())) {
        jobVolumeMounts.add(new VolumeMountBuilder()
                      .withName("output")
                      .withMountPath(jobConfiguration.getOutputPath())
                      .build());
      }
      if (jobConfiguration.getInputPaths() != null && !jobConfiguration.getInputPaths().isEmpty()) {
        List<String> inputPaths = jobConfiguration.getInputPaths();
        for (int i = 0, inputPathsSize = inputPaths.size(); i < inputPathsSize; i++) { ;
          if (!Strings.isNullOrEmpty(inputPaths.get(i))) {
            jobVolumeMounts.add(new VolumeMountBuilder()
                    .withName("inputpath" + i)
                    .withMountPath(inputPaths.get(i))
                    .build());
          }
        }
      }

      containers.add (new ContainerBuilder()
              .withName("logger")
              .withImage(projectUtils.getFullDockerImageName(project, true))
              .withImagePullPolicy(settings.getKubeImagePullPolicy())
              .withEnv(sidecarContainerEnv)
              .withCommand("/bin/sh")
              .withArgs("-c","while true; do sleep 1; done")
              .withVolumeMounts(jobVolumeMounts)
              .withLifecycle(lifecycleBuilder.build())
              .build());

    }
    return containers;
  }

  private List<EnvVar> getPrimaryContainerEnv(JobType jobType, Users user, String hadoopUser, Project project,
    Execution execution, String certificatesDir, String secretsDir, String anacondaEnv,
    DockerJobConfiguration dockerJobConfiguration, ResourceRequirements resourceRequirements)
    throws ServiceDiscoveryException, IOException, ApiKeyException {

    String elasticEndpoint = (settings.isOpenSearchHTTPSEnabled() ? "https://" : "http://") +
        serviceDiscoveryController.constructServiceAddressWithPort(
            HopsworksService.OPENSEARCH.getNameWithTag(OpenSearchTags.rest));

    List<EnvVar> environment = new ArrayList<>();
    switch (jobType) {
      case PYTHON:
        environment.add(new EnvVarBuilder().withName("SPARK_HOME").withValue(settings.getSparkDir()).build());
        environment.add(new EnvVarBuilder().withName("SPARK_CONF_DIR").withValue(settings.getSparkConfDir()).build());
        environment.add(new EnvVarBuilder().withName("ELASTIC_ENDPOINT").withValue(elasticEndpoint)
          .build());
        environment.add(new EnvVarBuilder().withName("HADOOP_VERSION").withValue(settings.getHadoopVersion()).build());
        environment.add(new EnvVarBuilder().withName("HOPSWORKS_VERSION").withValue(settings.getHopsworksVersion())
          .build());
        environment.add(new EnvVarBuilder().withName("TENSORFLOW_VERSION").withValue(settings.getTensorflowVersion())
          .build());
        environment.add(new EnvVarBuilder().withName("KAFKA_VERSION").withValue(settings.getKafkaVersion()).build());
        environment.add(new EnvVarBuilder().withName("SPARK_VERSION").withValue(settings.getSparkVersion()).build());
        environment.add(new EnvVarBuilder().withName("LIVY_VERSION").withValue(settings.getLivyVersion()).build());
        environment.add(new EnvVarBuilder().withName("HADOOP_HOME").withValue(settings.getHadoopSymbolicLinkDir())
          .build());
        environment.add(new EnvVarBuilder().withName("HADOOP_HDFS_HOME").withValue(settings.getHadoopSymbolicLinkDir())
          .build());
        environment.add(new EnvVarBuilder().withName("HADOOP_USER_NAME").withValue(hadoopUser).build());

        String jobName = execution.getJob().getName();
        String executionId = String.valueOf(execution.getId());
        environment.add(new EnvVarBuilder().withName("HOPSWORKS_JOB_NAME").withValue(jobName).build());
        environment.add(new EnvVarBuilder().withName("HOPSWORKS_JOB_EXECUTION_ID").withValue(executionId).build());
        environment.add(new EnvVarBuilder().withName("HOPSWORKS_JOB_TYPE")
          .withValue(execution.getJob().getJobConfig().getJobType().getName()).build());
        environment.add(new EnvVarBuilder().withName("HOPSWORKS_LOGS_DATASET")
          .withValue(Settings.BaseDataset.LOGS.getName()).build());

        String brokers = kafkaBrokers.getBrokerEndpointsString(KafkaBrokers.BrokerProtocol.INTERNAL);
        if (!Strings.isNullOrEmpty(brokers)) {
          environment.add(new EnvVarBuilder().withName("KAFKA_BROKERS").withValue(brokers)
            .build());
        }
        Service hopsworks =
                serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
                        HopsworksService.GLASSFISH.getNameWithTag(GlassfishTags.hopsworks));
        environment.add(new EnvVarBuilder().withName("REST_ENDPOINT")
          .withValue("https://" + hopsworks.getName() + ":" + hopsworks.getPort()).build());
        environment.add(new EnvVarBuilder().withName(Settings.SPARK_PYSPARK_PYTHON)
          .withValue(settings.getAnacondaProjectDir() + "/bin/python").build());
        environment.add(new EnvVarBuilder().withName("HOPSWORKS_PROJECT_ID")
          .withValue(Integer.toString(project.getId())).build());
        environment.add(new EnvVarBuilder().withName("REQUESTS_VERIFY")
          .withValue(String.valueOf(settings.getRequestsVerify())).build());
        environment.add(new EnvVarBuilder().withName("DOMAIN_CA_TRUSTSTORE")
          .withValue(Paths.get(certificatesDir, hadoopUser + Settings.TRUSTSTORE_SUFFIX).toString()).build());
        environment.add(new EnvVarBuilder().withName("SECRETS_DIR").withValue(secretsDir).build());
        environment.add(new EnvVarBuilder().withName("CERTS_DIR").withValue(certificatesDir).build());
        environment.add(new EnvVarBuilder().withName("FLINK_CONF_DIR").withValue(settings.getFlinkConfDir()).build());
        environment.add(new EnvVarBuilder().withName("FLINK_LIB_DIR").withValue(settings.getFlinkLibDir()).build());
        environment.add(new EnvVarBuilder().withName("HADOOP_CLASSPATH_GLOB")
          .withValue(settings.getHadoopClasspathGlob()).build());
        environment.add(new EnvVarBuilder().withName("SPARK_CONF_DIR").withValue(settings.getSparkConfDir()).build());
        environment.add(new EnvVarBuilder().withName("ANACONDA_ENV").withValue(anacondaEnv).build());
        environment.add(new EnvVarBuilder().withName("APP_FILE")
          .withValue(FilenameUtils.getName(((PythonJobConfiguration)dockerJobConfiguration).getAppPath())).build());
        environment.add(new EnvVarBuilder().withName("APP_ARGS").withValue(execution.getArgs()).build());
        environment.add(new EnvVarBuilder().withName("APP_FILES")
          .withValue(((PythonJobConfiguration)dockerJobConfiguration).getFiles()).build());
        String nvidiaVisibleDevices = kubeClientService.getNvidiaVisibleDevices(resourceRequirements);
        if (nvidiaVisibleDevices != null) {
          environment.add(new EnvVarBuilder().withName("NVIDIA_VISIBLE_DEVICES")
            .withValue(nvidiaVisibleDevices).build());
        }
        
        // serving env vars
        if (settings.getKubeKServeInstalled()) {
          environment.add(new EnvVarBuilder().withName("SERVING_API_KEY").withValueFrom(
            new EnvVarSourceBuilder().withNewSecretKeyRef(KubeApiKeyUtils.SERVING_API_KEY_SECRET_KEY,
              kubeApiKeyUtils.getProjectServingApiKeySecretName(user), false).build()).build());
          Map<String, String> servingEnvVars = servingConfig.getEnvVars(user, false);
          servingEnvVars.forEach((key, value) -> environment.add(
            new EnvVarBuilder().withName(key).withValue(value).build()));
        }
        //  HOPSWORKS-3158 add public hopsworks hostname
        environment.add(new EnvVarBuilder().withName("HOPSWORKS_PUBLIC_HOST").
                                           withValue(settings.getHopsworksPublicHost()).build());
        String appPath = ((PythonJobConfiguration)dockerJobConfiguration).getAppPath();
        if (settings.getMountHopsfsInJobContainer()) {
          com.logicalclocks.servicediscoverclient.service.Service hdfsService =
              serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
                  HopsworksService.NAMENODE.getNameWithTag(NamenodeTags.rpc));
          appPath = Utils.prepPath(appPath);
          appPath = appPath.replaceFirst(Utils.getProjectPath(project.getName()), "");
          environment.add(new EnvVarBuilder().withName("IS_TLS").withValue(String.valueOf(settings.getHopsRpcTls()))
              .build());
          environment.add(new EnvVarBuilder().withName("NAMENODE_IP").withValue(hdfsService.getAddress()).build());
          environment.add(new EnvVarBuilder().withName("NAMENODE_PORT").withValue(String.valueOf(hdfsService.getPort()))
              .build());
          environment.add(new EnvVarBuilder().withName("MOUNT_HOPSFS").withValue(String.valueOf(true)).build());
          environment.add(new EnvVarBuilder().withName("PROJECT_PATH")
              .withValue(Utils.getProjectPath(project.getName())).build());
        }
        environment.add(new EnvVarBuilder().withName("APP_PATH").withValue(appPath).build());
        break;
      case DOCKER:
        if (dockerJobConfiguration.getEnvVars() != null && !dockerJobConfiguration.getEnvVars().isEmpty()) {
          Map<String, String> envVars = HopsUtils.parseUserProperties(String.join("\n",
                  dockerJobConfiguration.getEnvVars()));
          environment.addAll(kubeClientService.getEnvVars(envVars));
        }
        break;
      default:
        throw new UnsupportedOperationException("Job type not supported: " + jobType);
    }
    return environment;
  }
  
  private List<EnvVar> getSidecarContainerEnv(JobType jobType, Project project, Execution execution,
    String certificatesDir, String hdfsUser) throws ServiceDiscoveryException {
    
    List<EnvVar> environment = new ArrayList<>();
    switch (jobType) {
      case PYTHON:
        String jobName = execution.getJob().getName();
        String executionId = String.valueOf(execution.getId());
        environment.add(new EnvVarBuilder().withName("LOGPATH").withValue("/app/logs/*").build());
        environment.add(new EnvVarBuilder().withName("LOGSTASH").withValue(getLogstashURL()).build());
        environment.add(new EnvVarBuilder().withName("JOB").withValue(jobName).build());
        environment.add(new EnvVarBuilder().withName("EXECUTION").withValue(executionId).build());
        environment.add(new EnvVarBuilder().withName("PROJECT").withValue(project.getName().toLowerCase()).build());
        break;
      case DOCKER:
        environment.add(new EnvVarBuilder().withName("HADOOP_CONF_DIR").withValue(settings.getHadoopConfDir()).build());
        environment.add(new EnvVarBuilder().withName("HADOOP_CLIENT_OPTS")
          .withValue("-Dfs.permissions.umask-mode=0007").build());
        environment.add(new EnvVarBuilder().withName("MATERIAL_DIRECTORY").withValue(certificatesDir).build());
        environment.add(new EnvVarBuilder().withName("HADOOP_USER_NAME").withValue(hdfsUser).build());
        break;
      default:
        throw new UnsupportedOperationException("Job type not supported: " + jobType);
    }
    return environment;
  }

  private ObjectMeta getPodMetadata(String kubeProjectUser, Execution execution) {
    JobType jobType = execution.getJob().getJobType();
    ObjectMetaBuilder podMetaBuilder = new ObjectMetaBuilder()
        .withLabels(ImmutableMap.of(jobType.getName().toLowerCase(), kubeProjectUser,
            "execution", Integer.toString(execution.getId()),
            "job-type", jobType.getName().toLowerCase(),
            "deployment-type", "job"));
    if (settings.getMountHopsfsInJobContainer()) {
      String apparmorProfile = "unconfined";
      if (settings.getApplyHopsfsMountApparmor()) {
        apparmorProfile = "localhost/" + settings.getHopsfsMountApparmorProfile();
      }
      podMetaBuilder.withAnnotations(
          ImmutableMap.of(
              "container.apparmor.security.beta.kubernetes.io/"
                  + execution.getJob().getJobType().getName().toLowerCase(),
              apparmorProfile
          )
      );
    }
    return podMetaBuilder.build();
  }

  @Override
  public Execution stop(Jobs job) throws JobException {
    return super.stop(job);
  }

  @Override
  public Execution stopExecution(Integer id) throws JobException {
    Execution execution =
            executionFacade.findById(id).orElseThrow(() ->
                    new JobException(RESTCodes.JobErrorCode.JOB_EXECUTION_NOT_FOUND, FINE, "Execution: " + id));
    if (execution.getJob().getJobType() == JobType.PYTHON || execution.getJob().getJobType() == JobType.DOCKER) {
      return stopExecution(execution);
    }
    return super.stopExecution(execution);
  }

  @Override
  public Execution stopExecution(Execution execution) throws JobException {
    try {
      executionFacade.updateState(execution, JobState.KILLED);
      executionFacade.updateFinalStatus(execution, JobFinalStatus.KILLED);
    } catch (Exception e) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_STOP_FAILED, SEVERE,
        "Job: " + execution.getJob().getName() + ", Execution: " + execution.getId(), null, e);
    }
    return execution;
  }

  @Override
  public void delete(Execution execution, Users user) throws JobException {
    if (execution.getJob().getJobType() == JobType.PYTHON || execution.getJob().getJobType() == JobType.DOCKER) {
      stopExecution(execution);
    } else {
      super.stopExecution(execution);
    }
    super.delete(execution, user);
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
                    .getAnyAddressOfServiceWithDNS(
                        HopsworksService.LOGSTASH.getNameWithTag(LogstashTags.pythonjobs));
    return logstash.getAddress() + ":" + logstash.getPort();
  }

  private List<Pair<Volume, VolumeMount>> parseVolumes(DockerJobConfiguration jobConfiguration) throws JobException {
    if (jobConfiguration.getJobType() == JobType.DOCKER && jobConfiguration.getVolumes() != null
            && !jobConfiguration.getVolumes().isEmpty()) {
      if (!settings.isDockerJobMountAllowed()) {
        throw new JobException(RESTCodes.JobErrorCode.DOCKER_MOUNT_NOT_ALLOWED, FINE);
      }
      List<Pair<Volume, VolumeMount>> volumeMounts = new ArrayList<>();
      int i = 0;
      for (String volume : jobConfiguration.getVolumes()) {
        String hostPath = volume.split(":")[0];
        if (!Strings.isNullOrEmpty(hostPath)) {// Check if the user-provided volume is allowed
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
    return new ArrayList<>();
  }

  /**
   * Returns the path in the Logs dataset where logs should be written by default.
   * @param job
   * @param execution
   * @return String logs path
   */
  private String getLogsPath(Jobs job, Execution execution) {
    return Utils.getJobLogLocation(execution.getJob().getProject().getName(),
            execution.getJob().getJobType())[0] + job.getName() + File.separator + execution.getId() ;
  }
}
