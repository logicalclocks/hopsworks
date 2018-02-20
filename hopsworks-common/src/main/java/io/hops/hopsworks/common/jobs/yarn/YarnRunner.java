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

package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.jobs.AsynchronousJobExecutor;
import io.hops.hopsworks.common.jobs.flink.YarnClusterClient;
import io.hops.hopsworks.common.jobs.flink.YarnClusterDescriptor;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.IoUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.tensorflow.Client;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.tensorflow.LocalResourceInfo;
import org.apache.flink.client.program.PackagedProgram;
import org.apache.flink.client.program.ProgramInvocationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.codehaus.plexus.util.FileUtils;

/**
 * <p>
 */
public class YarnRunner {

  private static final Logger logger = Logger.getLogger(YarnRunner.class.
      getName());
  public static final String APPID_PLACEHOLDER = "$APPID";
  private static final String APPID_REGEX = "\\$APPID";
  public static final String KEY_CLASSPATH = "CLASSPATH";
  private static final String LOCAL_LOG_DIR_PLACEHOLDER = "<LOG_DIR>";

  private Configuration conf;
  private ApplicationId appId = null;
  //Type of Job to run, Spark/Flink/Adam...
  private JobType jobType;
  //The parallelism parameter of Flink
  private int parallelism;
  private YarnClusterDescriptor flinkCluster;
  private Client tfClient;
  private String appJarPath;
  private final String amJarLocalName;
  private final String amJarPath;
  private final String amQueue;
  private int amMemory;
  private int amVCores;
  private String appName;
  private final String amMainClass;
  private String amArgs;
  private final Map<String, LocalResourceDTO> amLocalResourcesToCopy;
  private final Map<String, LocalResourceDTO> amLocalResourcesOnHDFS;
  private final Map<String, String> amEnvironment;
  private String localResourcesBasePath;
  private final boolean shouldCopyAmJarToLocalResources;
  private final List<String> filesToBeCopied;
  private final List<YarnSetupCommand> commands;
  private final List<String> javaOptions;
  private final List<String> filesToRemove;
  private String serviceDir;
  private final AsynchronousJobExecutor services;
  private DistributedFileSystemOps dfsClient;
  private YarnClient yarnClient;
  private final String keyStorePassword;
  private final String trustStorePassword;
  private String jobUser;
  
  private boolean readyToSubmit = false;
  private ApplicationSubmissionContext appContext;

  /**
   * Taken from Apache Spark code: Escapes a string for inclusion in a command
   * line executed by Yarn. Yarn executes commands
   * using `bash -c "command arg1 arg2"` and that means plain quoting doesn't
   * really work. The
   * argument is enclosed in single quotes and some key characters are escaped.
   * <p/>
   * @param s A single argument.
   * @return Argument quoted for execution via Yarn's generated shell script.
   */
  public static String escapeForShell(String s) {
    if (s != null) {
      StringBuilder escaped = new StringBuilder("'");
      for (int i = 0; i < s.length(); i++) {
        switch (s.charAt(i)) {
          case '$':
            escaped.append("\\$");
            break;
          case '"':
            escaped.append("\\\"");
            break;
          case '\'':
            escaped.append("'\\''");
            break;
          default:
            escaped.append(s.charAt(i));
            break;
        }
      }
      return escaped.append("'").toString();
    } else {
      return s;
    }
  }

  /**
   * This method is only used by Spark family jobs. Flink jobs copy their
   * certificates in FlinkJob since it's a little bit problematic the way it
   * submits a job to Yarn
   *
   * @param project
   * @param jobType
   * @param dfso
   * @param username
   * @param applicationId
   */
  private void copyUserCertificates(
      Project project, JobType jobType, DistributedFileSystemOps dfso,
      String username, String applicationId) {
    List<LocalResourceDTO> materialResources = new ArrayList<>(2);
    Map<String, String> systemProperties = new HashMap<>(2);

    HopsUtils.copyProjectUserCerts(project, username,
        services.getSettings().getHopsworksTmpCertDir(),
        services.getSettings().getHdfsTmpCertDir(), jobType,
        dfso, materialResources, systemProperties, services.getSettings().getGlassfishTrustStoreHdfs(),
        applicationId, services.getCertificateMaterializer(),
        services.getSettings().getHopsRpcTls());

    for (LocalResourceDTO materialDTO : materialResources) {
      amLocalResourcesOnHDFS.put(materialDTO.getName(), materialDTO);
    }

    for (Map.Entry<String, String> sysProp : systemProperties.entrySet()) {
      String option = YarnRunner.escapeForShell("-D" + sysProp.getKey() + "=" + sysProp.getValue());
      javaOptions.add(option);
    }
  }

  //---------------------------------------------------------------------------
  //-------------- CORE METHOD: START APPLICATION MASTER ----------------------
  //---------------------------------------------------------------------------
  /**
   * Start the Yarn Application Master.
   * @param project
   * @param dfso
   * @param username
   * @return The received ApplicationId identifying the application.
   * @throws YarnException
   * @throws IOException Can occur upon opening and moving execution and input
   * files.
   * @throws java.net.URISyntaxException
   */
  public YarnMonitor startAppMaster(YarnClientService ycs, String dfsUsername,
      Project project, DistributedFileSystemOps dfso, String username) throws
      YarnException, IOException, URISyntaxException {
    logger.info("Starting application master.");
    // Create a new client for monitoring
    YarnClientWrapper newYarnClientWrapper = ycs.getYarnClient(dfsUsername);
    
    YarnMonitor monitor = null;
    if (jobType == JobType.SPARK || jobType == JobType.PYSPARK || jobType == JobType.ADAM || 
        jobType == JobType.TFSPARK) {
      //Get application id
      
      YarnClientApplication app = yarnClient.createApplication();
      GetNewApplicationResponse appResponse = app.getNewApplicationResponse();
      appId = appResponse.getApplicationId();
      //And replace all occurences of $APPID with the real id.
      fillInAppid(appId.toString());

      copyUserCertificates(project, jobType, dfso, username,
          appId.toString());

      //Check resource requests and availabilities
      checkAmResourceRequest(appResponse);

      //Set application name and type
      appContext = app.getApplicationSubmissionContext();
      appContext.setApplicationName(appName);
      appContext.setApplicationType("HopsWorks-Yarn");

      //Add local resources to AM container
      Map<String, LocalResource> localResources = addAllToLocalResources();

      //Copy files to HDFS that are expected to be there
      copyAllToHDFS();
      
      //Set up environment
      Map<String, String> env = new HashMap<>();
      env.putAll(amEnvironment);
      setUpClassPath(env);

      //Set up commands
      String hdfsUser = project.getName() + "__" + username;
      List<String> amCommands = setUpCommands(hdfsUser);
      //Set up container launch context
      ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
          localResources, env, amCommands, null, null, null);

      //Finally set up context
      appContext.setAMContainerSpec(amContainer); //container spec
      appContext.setResource(Resource.newInstance(amMemory, amVCores)); //resources
      appContext.setQueue(amQueue); //Queue

      // Signify that ready to submit
      readyToSubmit = true;

      //Run any remaining commands
      for (YarnSetupCommand c : commands) {
        c.execute(this);
      }

      // Set keystore and truststore passwords
      if (services.getSettings().getHopsRpcTls()) {
        appContext.setKeyStorePassword(keyStorePassword);
        appContext.setTrustStorePassword(trustStorePassword);
      }
      
      //And submit
      logger.log(Level.INFO,
          "Submitting application {0} to applications manager.", appId);
      yarnClient.submitApplication(appContext);
      monitor = new YarnMonitor(appId, newYarnClientWrapper, ycs);

    } else if (jobType == JobType.FLINK) {
      // Objects needed for materializing user certificates
      flinkCluster.setCertsObjects(services, project, username, javaOptions);

      YarnClusterClient client = flinkCluster.deploy();
      appId = client.getApplicationId();

      fillInAppid(appId.toString());
      monitor = new YarnMonitor(appId, newYarnClientWrapper, ycs);
      String[] args = {};
      if (amArgs != null) {
        if (!javaOptions.isEmpty()) {
          amArgs += " --kafka_params \"";
          for (String s : javaOptions) {
            amArgs += s + ",";
          }
          amArgs = amArgs.substring(0, amArgs.length() - 1);
          amArgs += "\"";
        }
        args = amArgs.trim().split(" ");
      }

      /*
       * Copy the appjar to the localOS as it is needed by the Flink client
       * Create path in local /tmp to store the appjar
       * To distinguish between jars for different job executions, add the
       * current system time in the filename. This jar is removed after
       * the job is finished.
       */
      String localPathAppJarDir = "/tmp/" + appJarPath.substring(appJarPath.
          indexOf("Projects"), appJarPath.lastIndexOf("/")) + "/" + appId;
      String appJarName = appJarPath.substring(appJarPath.lastIndexOf("/")).
          replace("/", "");
      File tmpDir = new File(localPathAppJarDir);
      if (!tmpDir.exists()) {
        tmpDir.mkdir();
      }
      //Copy job jar locaclly so that Flink client has access to it 
      //in YarnRunner
      FileSystem fs = FileSystem.get(conf);
      fs.copyToLocalFile(new Path(appJarPath), new Path(localPathAppJarDir + "/"
          + appJarName));
      //app.jar path 
      File file = new File(localPathAppJarDir + "/" + appJarName);
      try {
        List<URL> classpaths = new ArrayList<>();
        //Copy Flink jar to local machine and pass it to the classpath
        URL flinkURL = new File(serviceDir + "/"
            + Settings.FLINK_LOCRSC_FLINK_JAR).toURI().toURL();
        classpaths.add(flinkURL);
        PackagedProgram program = new PackagedProgram(file, classpaths, args);
        client.run(program, parallelism);
      } catch (ProgramInvocationException ex) {
        logger.log(Level.WARNING, "Error while submitting Flink job to cluster",
            ex);
        //Kill the flink job here
        Runtime rt = Runtime.getRuntime();
        rt.exec(services.getSettings().getHadoopSymbolicLinkDir() + "/bin/yarn application -kill " + appId.toString());
        throw new IOException("Error while submitting Flink job to cluster:"+ex.getMessage());
      } finally {
        //Remove local flink app jar
        FileUtils.deleteDirectory(localPathAppJarDir);
        flinkCluster = null;
        appId = null;
        appContext = null;
        //Try to delete any local certificates for this project
        logger.log(Level.INFO, "Deleting local flink app jar:{0}", appJarPath);
      }

    } else if (jobType == JobType.TENSORFLOW) {
      try {
        
        tfClient.setConf(conf);
        tfClient.initYarnClient();
        YarnClientApplication app = tfClient.createApplication();
        appId = app.getNewApplicationResponse().getApplicationId();
        
        copyUserCertificates(project, jobType, dfso, username, appId.toString());
        
        String kstore = "hdfs://" + services.getSettings().getHdfsTmpCertDir()
            + File.separator + project.getName() + HdfsUsersController
            .USER_NAME_DELIMITER + username + File.separator + appId.toString()
            + File.separator + HopsUtils.getProjectKeystoreName(project.getName(),
            username);
        
  
        String tstore = "hdfs://" + services.getSettings().getHdfsTmpCertDir()
            + File.separator + project.getName() + HdfsUsersController
            .USER_NAME_DELIMITER + username + File.separator +appId.toString()
            + File.separator + HopsUtils.getProjectTruststoreName(project.getName(),
            username);
        
        tfClient.addFile(kstore);
        tfClient.addFile(tstore);
        
        tfClient.getFilesInfo().put(kstore, new LocalResourceInfo(Settings
            .K_CERTIFICATE, kstore, LocalResourceVisibility.PRIVATE.toString(),
            LocalResourceType.FILE.toString(), null));
        tfClient.getFilesInfo().put(tstore, new LocalResourceInfo(Settings
            .T_CERTIFICATE, tstore, LocalResourceVisibility.PRIVATE.toString(),
            LocalResourceType.FILE.toString(), null));
  
        // If RPC TLS is enabled, password file would be injected by the
        // NodeManagers. We don't need to add it as LocalResource
        if (!services.getSettings().getHopsRpcTls()) {
          String passFile =
              "hdfs://" + services.getSettings().getHdfsTmpCertDir()
                  + File.separator + project.getName() + HdfsUsersController
                  .USER_NAME_DELIMITER + username + File.separator +
                  appId.toString()
                  + File.separator + HopsUtils.getProjectMaterialPasswordName(
                  project.getName(), username);
          tfClient.addFile(passFile);
          tfClient.getFilesInfo().put(passFile, new LocalResourceInfo(Settings
              .CRYPTO_MATERIAL_PASSWORD, passFile, LocalResourceVisibility.PRIVATE
              .toString(), LocalResourceType.FILE.toString(), null));
          logger.log(Level.INFO, "Adding local resource {0}", passFile);
        }
        
        logger.log(Level.INFO, "Adding local resource {0}", kstore);
        logger.log(Level.INFO, "Adding local resource {0}", tstore);
        
        
        tfClient.submitApplication(app);
//        String logstashInfo = tfClient.getEnvironment().get(Settings.LOGSTASH_JOB_INFO);
//        logstashInfo = logstashInfo.replaceAll(APPID_REGEX, appId.toString());
//        tfClient.addEnvironmentVariable(Settings.LOGSTASH_JOB_INFO, logstashInfo);
        fillInAppid(appId.toString());
        monitor = new YarnMonitor(appId, newYarnClientWrapper, ycs);
      } finally {
        appId = null;
      }
    }

    return monitor;
  }

  //---------------------------------------------------------------------------
  //--------------------------- CALLBACK METHODS ------------------------------
  //---------------------------------------------------------------------------
  /**
   * Get the ApplicationSubmissionContext used to submit the app. This method
   * should only be called from registered
   * Commands. Invoking it before the ApplicationSubmissionContext is properly
   * set up will result in an
   * IllegalStateException.
   * 
   * @return
   */
  public ApplicationSubmissionContext getAppContext() {
    if (!readyToSubmit) {
      throw new IllegalStateException(
          "ApplicationSubmissionContext cannot be requested before it is set up.");
    }
    return appContext;
  }
  
  public void stop(DistributedFsService dfs) {
    if (dfsClient != null && dfs != null) {
      dfs.closeDfsClient(dfsClient);
    }
  }
  
  //---------------------------------------------------------------------------
  //------------------------- UTILITY METHODS ---------------------------------
  //---------------------------------------------------------------------------
  private void fillInAppid(String id) {
    localResourcesBasePath = localResourcesBasePath.replaceAll(APPID_REGEX, id).replace("\\", "");
    appName = appName.replaceAll(APPID_REGEX, id);
    if (amArgs != null) {
      amArgs = amArgs.replaceAll(APPID_REGEX, id);
    }
    for (Entry<String, LocalResourceDTO> entry : amLocalResourcesToCopy.
        entrySet()) {
      entry.getValue().setName(entry.getValue().getName().
          replaceAll(APPID_REGEX, id));
    }
    //TODO(Theofilos): thread-safety?
    for (Entry<String, String> entry : amEnvironment.entrySet()) {
      entry.setValue(entry.getValue().replaceAll(APPID_REGEX, id));
    }
    for (ListIterator<String> i = javaOptions.listIterator(); i.hasNext();) {
      i.set(i.next().replaceAll(APPID_REGEX, id));
    }
    
    //Loop through files to remove
    for (ListIterator<String> i = filesToRemove.listIterator(); i.hasNext();) {
      i.set(i.next().replaceAll(APPID_REGEX, id));
    }
  }

  private void checkAmResourceRequest(GetNewApplicationResponse appResponse) {
    int maxMem = appResponse.getMaximumResourceCapability().getMemory();
    if (amMemory > maxMem) {
      logger.log(Level.WARNING,
          "AM memory specified above max threshold of cluster. Using max value. Specified: {0}, max: {1}",
          new Object[]{amMemory,
            maxMem});
      amMemory = maxMem;
    }
    int maxVcores = appResponse.getMaximumResourceCapability().getVirtualCores();
    if (amVCores > maxVcores) {
      logger.log(Level.WARNING,
          "AM vcores specified above max threshold of cluster. Using max value. Specified: {0}, max: {1}",
          new Object[]{amVCores,
            maxVcores});
      amVCores = maxVcores;
    }
  }

  private Map<String, LocalResource> addAllToLocalResources() throws IOException, URISyntaxException {
    Map<String, LocalResource> localResources = new HashMap<>();
    //If an AM jar has been specified: include that one
    if (shouldCopyAmJarToLocalResources && amJarLocalName != null
        && !amJarLocalName.isEmpty() && amJarPath != null
        && !amJarPath.isEmpty()) {
      if (amJarPath.startsWith("hdfs:")) {
        amLocalResourcesOnHDFS.put(amJarLocalName, new LocalResourceDTO(
            amJarLocalName, amJarPath,
            LocalResourceVisibility.PUBLIC.toString(),
            LocalResourceType.FILE.toString(), null));
      } else {
        amLocalResourcesToCopy.put(amJarLocalName,
            new LocalResourceDTO(amJarLocalName, amJarPath,
                LocalResourceVisibility.PUBLIC.toString(),
                LocalResourceType.FILE.toString(), null));
      }
    }
    //Construct basepath
    FileSystem fs = dfsClient.getFilesystem();
    String hdfsPrefix = conf.get("fs.defaultFS");
    String basePath = hdfsPrefix + localResourcesBasePath;
    logger.log(Level.FINER, "Base path: {0}", basePath);
    //For all local resources with local path: copy and add local resource
    for (Entry<String, LocalResourceDTO> entry : amLocalResourcesToCopy.
        entrySet()) {
      logger.log(Level.FINE, "LocalResourceDTO to upload is :{0}", entry.
          toString());
      String key = entry.getKey();
      String source = entry.getValue().getPath();
      String filename = Utils.getFileName(source);
      Path dst = new Path(basePath + File.separator + filename);
      fs.copyFromLocalFile(new Path(source), dst);
      logger.log(Level.INFO, "Copying from: {0} to: {1}",
          new Object[]{source,
            dst});
      FileStatus scFileStat = fs.getFileStatus(dst);
      LocalResource scRsrc = LocalResource.newInstance(ConverterUtils.
          getYarnUrlFromPath(dst),
          LocalResourceType.
              valueOf(entry.getValue().getType().toUpperCase()),
          LocalResourceVisibility.valueOf(entry.getValue().getVisibility().
              toUpperCase()),
          scFileStat.getLen(),
          scFileStat.getModificationTime(),
          entry.getValue().getPattern());
      localResources.put(key, scRsrc);

    }
    //For all local resources with hdfs path: add local resource
    for (Entry<String, LocalResourceDTO> entry : amLocalResourcesOnHDFS.
        entrySet()) {
      logger.log(Level.FINE, "LocalResourceDTO to upload is :{0}", entry.
          toString());
      String key = entry.getKey();
      String pathToResource = entry.getValue().getPath();
      pathToResource = pathToResource.replaceFirst("hdfs:/*Projects",
          "hdfs:///Projects");
      pathToResource = pathToResource.replaceFirst("hdfs:/*user",
          "hdfs:///user");
      Path src = new Path(pathToResource);
      FileStatus scFileStat = fs.getFileStatus(src);
      LocalResource scRsrc = LocalResource.newInstance(ConverterUtils.
          getYarnUrlFromPath(src),
          LocalResourceType.
              valueOf(entry.getValue().getType().toUpperCase()),
          LocalResourceVisibility.valueOf(entry.getValue().getVisibility().
              toUpperCase()),
          scFileStat.getLen(),
          scFileStat.getModificationTime(),
          entry.getValue().getPattern());
      localResources.put(key, scRsrc);

    }
    //For Spark 2.0, loop through local resources and add their properties
    //as system properties (javaOptions)
    if (jobType == JobType.SPARK || jobType == JobType.PYSPARK || jobType == JobType.TFSPARK) {
      StringBuilder uris = new StringBuilder();
      StringBuilder timestamps = new StringBuilder();
      StringBuilder sizes = new StringBuilder();
      StringBuilder visibilities = new StringBuilder();
      StringBuilder types = new StringBuilder();
      for (Entry<String, LocalResource> entry : localResources.entrySet()) {
        Path destPath = ConverterUtils.getPathFromYarnURL(entry.getValue().
            getResource());
        URI sparkUri = destPath.toUri();
        URI pathURI = new URI(sparkUri.getScheme(), sparkUri.getAuthority(),
            sparkUri.getPath(), null, entry.getKey());
        uris.append(pathURI.toString()).append(",");
        timestamps.append(entry.getValue().getTimestamp()).append(",");
        sizes.append(entry.getValue().getSize()).append(",");
        visibilities.append(entry.getValue().getVisibility()).append(",");
        types.append(entry.getValue().getType()).append(",");
      }
      //Remove the last comma (,) and add them to javaOptions
      javaOptions.
          add(escapeForShell("-D" + Settings.SPARK_CACHE_FILENAMES + "=" + uris.substring(0, uris.length() - 1)));
      javaOptions.add(escapeForShell("-D" + Settings.SPARK_CACHE_TIMESTAMPS + "=" + timestamps.substring(0, timestamps.
          length() - 1)));
      javaOptions.add(escapeForShell("-D" + Settings.SPARK_CACHE_SIZES + "=" + sizes.substring(0, sizes.length() - 1)));
      javaOptions.add(escapeForShell("-D" + Settings.SPARK_CACHE_VISIBILITIES
          + "=" + visibilities.substring(0, visibilities.length() - 1)));
      javaOptions.add(escapeForShell("-D" + Settings.SPARK_CACHE_TYPES + "=" + types.substring(0, types.length() - 1)));
    }
    return localResources;
  }

  private void copyAllToHDFS() throws IOException {
    FileSystem fs = dfsClient.getFilesystem();
    String hdfsPrefix = conf.get("fs.defaultFS");
    String basePath = hdfsPrefix + localResourcesBasePath;
    for (String path : filesToBeCopied) {
      String destination = basePath + File.separator + Utils.getFileName(path);
      Path dst = new Path(destination);
      //copy the input file to where cuneiform expects it
      if (!path.startsWith("hdfs:")) {
        //First, remove any checksum files that are present
        //Since the file may have been downloaded from HDFS, modified and now trying to upload,
        //may run into bug HADOOP-7199 (https://issues.apache.org/jira/browse/HADOOP-7199)
        String dirPart = Utils.getDirectoryPart(path);
        String filename = Utils.getFileName(path);
        String crcName = dirPart + "." + filename + ".crc";
        Files.deleteIfExists(Paths.get(crcName));
        fs.copyFromLocalFile(new Path(path), dst);
      } else {
        Path srcPath = new Path(path);
        Path[] srcs = FileUtil.stat2Paths(fs.globStatus(srcPath), srcPath);
        if (srcs.length > 1 && !fs.isDirectory(dst)) {
          throw new IOException("When copying multiple files, "
              + "destination should be a directory.");
        }
        for (Path src1 : srcs) {
          FileUtil.copy(fs, src1, fs, dst, false, conf);
        }
      }
      logger.log(Level.INFO, "Copying from: {0} to: {1}",
          new Object[]{path, dst});
    }
  }

  private void setUpClassPath(Map<String, String> env) {
    // Add AppMaster.jar location to classpath
    StringBuilder classPathEnv = new StringBuilder();
    for (String c : conf.getStrings(
        YarnConfiguration.YARN_APPLICATION_CLASSPATH,
        YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
      classPathEnv.append(":").append(c.trim());
    }
    //classPathEnv.append(":").append("./log4j.properties");
    // add the runtime classpath needed for tests to work
    if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
      classPathEnv.append(':');
      classPathEnv.append(System.getProperty("java.class.path"));
    }
    String hadoopDir = services.getSettings().getHadoopSymbolicLinkDir();
    classPathEnv.append(HopsUtils.getHadoopClasspathGlob(hadoopDir + "/bin/hadoop", "classpath", "--glob"));
    //Check whether a classpath variable was already set, and if so: merge them
    //TODO(Theofilos): clean this up so no doubles are found in the classpath.
    if (env.containsKey(KEY_CLASSPATH)) {
      String clpth = env.get(KEY_CLASSPATH) + ":" + classPathEnv.toString();
      env.put(KEY_CLASSPATH, clpth);
    } else {
      env.put(KEY_CLASSPATH, classPathEnv.toString());
    }
    
    env.put(Settings.HADOOP_HOME_KEY, hadoopDir);
    //Put some environment vars in env
    env.put(Settings.HADOOP_COMMON_HOME_KEY, hadoopDir);
    env.put(Settings.HADOOP_CONF_DIR_KEY, services.getSettings().getHadoopConfDir(hadoopDir));
    env.put(Settings.HADOOP_HDFS_HOME_KEY, hadoopDir);
    env.put(Settings.HADOOP_YARN_HOME_KEY, hadoopDir);
  }

  private List<String> setUpCommands(String hdfsUser) {
    // Set the necessary command to execute the application master
    List<CharSequence> vargs = new ArrayList<>();
    // Set java executable command
    logger.info("Setting up app master command");
    vargs.add(ApplicationConstants.Environment.JAVA_HOME.$() + "/bin/java");
    // Set Xmx based on am memory size
    vargs.add("-Xmx" + amMemory + "M");
    //vargs.add(" -Dlogback.configurationFile=file:logback.xml");
    //vargs.add(" -Dlog4j.configuration=file:log4j.properties");
    //vargs.add(" -Dlog.file=/srv/hadoop/logs/userlogs/jobmanager1.out") ;   
    //Add jvm options
    for (String s : javaOptions) {
      vargs.add(s);
    }

    // Set class name
    vargs.add(amMainClass);
    // Set params for Application Master
    vargs.add(amArgs);

    vargs.add("1> ");
    vargs.add(LOCAL_LOG_DIR_PLACEHOLDER + "/stdout");

    vargs.add("2> ");
    vargs.add(LOCAL_LOG_DIR_PLACEHOLDER + "/stderr");

    // Get final commmand
    StringBuilder amcommand = new StringBuilder();
    for (CharSequence str : vargs) {
      amcommand.append(str).append(" ");
    }
//    logger.log(Level.INFO, "Completed setting up app master command: {0}",
//            amcommand.toString());
    List<String> amCommands = new ArrayList<>();
    amCommands.add(amcommand.toString());
    return amCommands;
  }

  //---------------------------------------------------------------------------        
  //------------------------- CONSTRUCTOR -------------------------------------
  //---------------------------------------------------------------------------
  private YarnRunner(Builder builder) {
    this.amJarLocalName = builder.amJarLocalName;
    this.amJarPath = builder.amJarPath;
    this.jobType = builder.jobType;
    this.parallelism = builder.parallelism;
    this.flinkCluster = builder.flinkCluster;
    this.tfClient = builder.tfClient;
    this.appJarPath = builder.appJarPath;
    this.amQueue = builder.amQueue;
    this.amMemory = builder.amMemory;
    this.amVCores = builder.amVCores;
    this.appName = builder.appName;
    this.amMainClass = builder.amMainClass;
    this.amArgs = builder.amArgs;
    this.amLocalResourcesToCopy = builder.amLocalResourcesToCopy;
    this.amLocalResourcesOnHDFS = builder.amLocalResourcesOnHDFS;
    this.amEnvironment = builder.amEnvironment;
    this.localResourcesBasePath = builder.localResourcesBasePath;
    this.yarnClient = builder.yarnClient;
    this.dfsClient = builder.dfsClient;
    this.keyStorePassword = builder.keyStorePassword;
    this.trustStorePassword = builder.trustStorePassword;
    this.jobUser = builder.jobUser;
    this.conf = builder.conf;
    this.shouldCopyAmJarToLocalResources
        = builder.shouldAddAmJarToLocalResources;
    this.filesToBeCopied = builder.filesToBeCopied;
    this.commands = builder.commands;
    this.javaOptions = builder.javaOptions;
    this.filesToRemove = builder.filesToRemove;
    this.serviceDir = builder.serviceDir;
    this.services = builder.services;
  }

  //---------------------------------------------------------------------------
  //-------------------------- GETTERS ----------------------------------------
  //---------------------------------------------------------------------------
  public String getAmArgs() {
    return amArgs;
  }

  public String getLocalResourcesBasePath() {
    return localResourcesBasePath;
  }

  public List<String> getFilesToRemove(){
    return filesToRemove;
  }
  
  public void cancelJob(String appid) throws YarnException, IOException {
    ApplicationId applicationId = ConverterUtils.toApplicationId(appid);
    yarnClient.killApplication(applicationId);
  }
  //---------------------------------------------------------------------------
  //-------------------------- BUILDER ----------------------------------------
  //---------------------------------------------------------------------------

  public static final class Builder {

    //Possibly equired attributes
    //The name of the application app master class
    private String amMainClass;
    //Path to the application master jar
    private String amJarPath;
    //The name of the application master jar in the local resources
    private String amJarLocalName;
    //Which job type is running 
    private JobType jobType;
    //Flink parallelism
    private int parallelism;
    private YarnClusterDescriptor flinkCluster;
    private String appJarPath;
    //TensorFlow client
    private Client tfClient;
    //Optional attributes
    // Queue for App master
    private String amQueue = "default"; //TODO(Theofilos): enable changing this, or infer from user data
    // Memory for App master (in MB)
    private int amMemory = 1024;
    //Number of cores for appMaster
    private int amVCores = 1;
    // Application name
    private String appName = "HopsWorks-Yarn";
    //Arguments to pass on in invocation of Application master
    private String amArgs;
    //List of paths to resources that should be copied to application master
    private Map<String, LocalResourceDTO> amLocalResourcesToCopy
        = new HashMap<>();
    //List of paths to resources that are already in HDFS, but AM should know about
    private Map<String, LocalResourceDTO> amLocalResourcesOnHDFS
        = new HashMap<>();
    //Application master environment
    private Map<String, String> amEnvironment = new HashMap<>();
    //Path where the application master expects its local resources to be (added to fs.getHomeDirectory)
    private String localResourcesBasePath;
    //Signify whether the application master jar should be added to local resources
    private boolean shouldAddAmJarToLocalResources = true;
    //List of files to be copied to localResourcesBasePath
    private List<String> filesToBeCopied = new ArrayList<>();
    //List of commands to execute before submission
    private List<YarnSetupCommand> commands = new ArrayList<>();
    //List of options to add to the JVM invocation
    private List<String> javaOptions = new ArrayList<>();
    //List of files to be removed after starting AM.
    private List<String> filesToRemove = new ArrayList<>();

    //Hadoop Configuration
    private Configuration conf;
    //YarnClient
    private YarnClient yarnClient;
    private DistributedFileSystemOps dfsClient;
    private String jobUser;

    private String keyStorePassword;
    private String trustStorePassword;
    
    private String serviceDir;
    private AsynchronousJobExecutor services;
    
    //Constructors
    public Builder(String amMainClass) {
      this.amMainClass = amMainClass;
    }

    public Builder(String amJarPath, String amJarLocalName) {
      this.amJarPath = amJarPath;
      this.amJarLocalName = amJarLocalName;
    }
    
    /**
     * Sets the configured DFS client
     *
     * @param dfsClient
     * @return
     */
    public Builder setDfsClient(DistributedFileSystemOps dfsClient) {
      this.dfsClient = dfsClient;
      return this;
    }
  
    /**
     * Sets the configured Yarn client
     *
     * @param yarnClient
     * @return
     */
    public Builder setYarnClient(YarnClient yarnClient) {
      this.yarnClient = yarnClient;
      return this;
    }

    public Builder setJobUser(String jobUser) {
      this.jobUser = jobUser;
      return this;
    }
    
    public Builder setKeyStorePassword(String password) {
      this.keyStorePassword = password;
      return this;
    }
    
    public Builder setTrustStorePassword(String password) {
      this.trustStorePassword = password;
      return this;
    }
    
    /**
     * Sets the arguments to be passed to the Application Master.
     * <p/>
     * @param amArgs
     * @return
     */
    public Builder amArgs(String amArgs) {
      this.amArgs = amArgs;
      return this;
    }

    /**
     * Set the amount of memory allocated to the Application Master (in MB).
     * <p/>
     * @param amMem Memory in MB.
     * @return
     */
    public Builder amMemory(int amMem) {
      this.amMemory = amMem;
      return this;
    }

    /**
     * Set the amount of cores allocated to the Application Master.
     * <p/>
     * @param amVCores
     * @return
     */
    public Builder amVCores(int amVCores) {
      this.amVCores = amVCores;
      return this;
    }

    public Builder appName(String appName) {
      this.appName = appName;
      return this;
    }

    public Builder amMainClass(String amMainClass) {
      this.amMainClass = amMainClass;
      return this;
    }

    public Builder amJar(String amJarPath, String amJarLocalName) {
      this.amJarLocalName = amJarLocalName;
      this.amJarPath = amJarPath;
      return this;
    }

    public Builder addAmJarToLocalResources(boolean value) {
      this.shouldAddAmJarToLocalResources = value;
      return this;
    }

    public Builder amQueue(String queuename) {
      this.amQueue = queuename;
      return this;
    }

    /**
     * Set the job type for this runner instance.
     *
     * @param jobType
     */
    public void setJobType(JobType jobType) {
      this.jobType = jobType;
    }

    /**
     * Set Flink parallelism property.
     *
     * @param parallelism
     */
    public void setParallelism(int parallelism) {
      this.parallelism = parallelism;
    }

    public void setFlinkCluster(YarnClusterDescriptor flinkCluster) {
      this.flinkCluster = flinkCluster;
    }

    public void setTfClient(Client tfClient) {
      this.tfClient = tfClient;
    }
    
    public void setAppJarPath(String path) {
      this.appJarPath = path;
    }

    /**
     * Set the configuration of the Yarn Application to the values contained in
     * the YarnJobConfiguration object. This
     * overrides any defaults or previously set values contained in the config
     * file.
     * <p/>
     * @param config
     * @return
     */
    public Builder setConfig(YarnJobConfiguration config) {
      this.amQueue = config.getAmQueue();
      this.amMemory = config.getAmMemory();
      this.amVCores = config.getAmVCores();
      this.appName = config.getAppName();
//      for (LocalResourceDTO dto : config.getLocalResources()) {
//        addLocalResource(dto,false);
//      }
      return this;
    }

    /**
     * Set the base path for local resources for the application master. This is
     * the path where the AM expects its local
     * resources to be. Use "$APPID" as a replacement for the appId, which will
     * be replaced once it is available.
     * <p/>
     * If this method is not invoked, a default path will be used.
     *
     * @param basePath
     * @return
     */
    public Builder localResourcesBasePath(String basePath) {
      while (basePath.endsWith(File.separator)) {
        basePath = basePath.substring(0, basePath.length() - 1);
      }
      if (!basePath.startsWith("/")) {
        basePath = "/" + basePath;
      }
      this.localResourcesBasePath = basePath;
      return this;
    }

    /**
     * Add a local resource that should be added to the AM container. The name
     * is the key as used in the LocalResources
     * map passed to the container. The source is the local path to the file.
     * The file will be copied into HDFS under
     * the path
     * <i>localResourcesBasePath</i>/<i>filename</i> and the source file will be
     * removed.
     *
     * @param dto
     * @return
     */
    public Builder addLocalResource(LocalResourceDTO dto) {
      return addLocalResource(dto, true);
    }

    /**
     * Add a local resource that should be added to the AM container. The name
     * is the key as used in the LocalResources
     * map passed to the container. The source is the local path to the file.
     * The file will be copied into HDFS under
     * the path
     * <i>localResourcesBasePath</i>/<i>filename</i> and if removeAfterCopy is
     * true, the original will be removed after
     * starting the AM.
     * <p/>
     * @param dto
     * @param removeAfterCopy
     * @return
     */
    public Builder addLocalResource(LocalResourceDTO dto,
        boolean removeAfterCopy) {
      if (dto.getPath().startsWith("hdfs")) {
        amLocalResourcesOnHDFS.put(dto.getName(), dto);
      } else {
        amLocalResourcesToCopy.put(dto.getName(), dto);
      }
      if (removeAfterCopy) {
        filesToRemove.add(dto.getPath());
      }
      return this;
    }
    
    public void addFileToRemove(String path){
      filesToRemove.add(path);
    }

    public Builder addToAppMasterEnvironment(String key, String value) {
      if (amEnvironment.containsKey(key)) {
        amEnvironment.put(key, amEnvironment.get(key) + ":" + value);
      } else {
        amEnvironment.put(key, value);
      }
      return this;
    }

    public Builder addAllToAppMasterEnvironment(Map<String, String> env) {
      amEnvironment.putAll(env);
      return this;
    }

    /**
     * Add a Command that should be executed before submission of the
     * application to the ResourceManager. The commands
     * will be executed in order of addition.
     * <p/>
     * @param c
     * @return
     */
    public Builder addCommand(YarnSetupCommand c) {
      commands.add(c);
      return this;
    }

    /**
     * Add a java option that will be added in the invocation of the java
     * command. Should be provided in a form that is
     * accepted by the java command, i.e. including a dash in the beginning etc.
     * <p/>
     * @param option
     * @return
     */
    public Builder addJavaOption(String option) {
      javaOptions.add(option);
      return this;
    }

    /**
     * Build the YarnRunner instance
     * <p/>
     * @param serviceDir
     * @param jobType
     * @param services
     * @return
     * @throws IllegalStateException Thrown if (a) configuration is not found,
     * (b) invalid main class name
     * @throws IOException Thrown if stdOut and/or stdErr path have not been set
     * and temp files could not be created
     */
    public YarnRunner build(String serviceDir, JobType jobType, AsynchronousJobExecutor services) throws
        IllegalStateException,
        IOException {
      //Set configuration
      try {
        this.services = services;
        conf = services.getSettings().getConfiguration();
        this.serviceDir = serviceDir;
        if (jobType == JobType.FLINK) {
          flinkCluster.setConf(conf);
        }
      } catch (IllegalStateException e) {
        throw new IllegalStateException("Failed to load configuration", e);
      }

      if (yarnClient == null) {
        //Set YarnClient
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
      }

      //Set main class
      if (amMainClass == null) {
        amMainClass = IoUtils.getMainClassNameFromJar(amJarPath, null);
        if (amMainClass == null) {
          throw new IllegalStateException(
              "Could not infer main class name from jar and was not specified.");
        }
      }
      //Default localResourcesBasePath
      if (localResourcesBasePath == null) {
        localResourcesBasePath = File.separator + APPID_PLACEHOLDER;
      }
      return new YarnRunner(this);
    }

  }

  //---------------------------------------------------------------------------        
  //---------------------------- TOSTRING -------------------------------------
  //---------------------------------------------------------------------------
  @Override
  public String toString() {
    if (!readyToSubmit) {
      return "YarnRunner: application context not requested yet.";
    } else {
      return "YarnRunner, ApplicationSubmissionContext: " + appContext;
    }
  }

  /**
   * Utility method that converts a string of the form "host:port" into an
   * {@link InetSocketAddress}.
   * The returned InetSocketAddress may be unresolved!
   *
   * @param hostport The "host:port" string.
   * @return The converted InetSocketAddress.
   */
  private static InetSocketAddress getInetFromHostport(String hostport) {
    //http://stackoverflow.com/questions/2345063/java-common-way-to-validate-and-convert-hostport-to-inetsocketaddress
    URI uri;
    try {
      uri = new URI("my://" + hostport);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Could not identify hostname and port in '"
          + hostport + "'.", e);
    }
    String host = uri.getHost();
    int port = uri.getPort();
    if (host == null || port == -1) {
      throw new RuntimeException("Could not identify hostname and port in '"
          + hostport + "'.");
    }
    return new InetSocketAddress(host, port);
  }
}
