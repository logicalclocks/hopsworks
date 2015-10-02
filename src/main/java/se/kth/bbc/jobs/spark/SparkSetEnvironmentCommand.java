package se.kth.bbc.jobs.spark;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.util.ConverterUtils;
import se.kth.bbc.jobs.yarn.YarnRunner;
import se.kth.bbc.jobs.yarn.YarnSetupCommand;

/**
 * Extra command to be executed before submitting a Spark job to Yarn. It sets
 * environment variables pointing out the local resources so that Spark
 * executors can access them. It may actually be possible to do this by setting
 * just environment variables (in SparkController.java). However, moving it here
 * keeps that code a bit cleaner.
 * <p/>
 * @author stig
 */
public final class SparkSetEnvironmentCommand extends YarnSetupCommand {

  private static final Logger logger = Logger.getLogger(
          SparkSetEnvironmentCommand.class.toString());

  @Override
  public void execute(YarnRunner r) {
    ApplicationSubmissionContext appContext = r.getAppContext();
    ContainerLaunchContext launchContext = appContext.getAMContainerSpec();
    Map<String, String> env = launchContext.getEnvironment();
    Map<String, LocalResource> rescs = launchContext.getLocalResources();

    StringBuilder fileUris = new StringBuilder();
    StringBuilder fileTimestamps = new StringBuilder();
    StringBuilder fileSizes = new StringBuilder();
    StringBuilder fileVisibilities = new StringBuilder();

    StringBuilder archiveUris = new StringBuilder();
    StringBuilder archiveTimestamps = new StringBuilder();
    StringBuilder archiveSizes = new StringBuilder();
    StringBuilder archiveVisibilities = new StringBuilder();

    for (Entry<String, LocalResource> resource : rescs.entrySet()) {
      try {
        Path destPath = ConverterUtils.getPathFromYarnURL(resource.getValue().
                getResource());
        URI sparkUri = destPath.toUri();
        URI pathURI = new URI(sparkUri.getScheme(), sparkUri.getAuthority(),
                sparkUri.getPath(), null, resource.getKey());
        if (resource.getValue().getType() == LocalResourceType.FILE) {
          fileUris.append(pathURI.toString()).append(",");
          fileTimestamps.append(resource.getValue().getTimestamp()).append(",");
          fileSizes.append(resource.getValue().getSize()).append(",");
          fileVisibilities.append(resource.getValue().getVisibility()).append(
                  ",");
        } else {
          archiveUris.append(pathURI.toString()).append(",");
          archiveTimestamps.append(resource.getValue().getTimestamp()).append(
                  ",");
          archiveSizes.append(resource.getValue().getSize()).append(",");
          archiveVisibilities.append(resource.getValue().getVisibility()).
                  append(",");
        }
      } catch (URISyntaxException e) {
        logger.log(Level.SEVERE, "Failed to add LocalResource " + resource
                + " to Spark Environment.", e);
      }
    }

    if (fileUris.length() > 1) {
      env.put("SPARK_YARN_CACHE_FILES", fileUris.substring(0, fileUris.length()
              - 1));
      env.put("SPARK_YARN_CACHE_FILES_TIME_STAMPS", fileTimestamps.substring(0,
              fileTimestamps.length() - 1));
      env.put("SPARK_YARN_CACHE_FILES_FILE_SIZES", fileSizes.substring(0,
              fileSizes.length() - 1));
      env.put("SPARK_YARN_CACHE_FILES_VISIBILITIES", fileVisibilities.substring(
              0,
              fileVisibilities.length() - 1));
    }
    if (archiveUris.length() > 1) {
      env.put("SPARK_YARN_CACHE_ARCHIVES", archiveUris.substring(0, archiveUris.
              length()
              - 1));
      env.put("SPARK_YARN_CACHE_ARCHIVES_TIME_STAMPS", archiveTimestamps.
              substring(0,
                      archiveTimestamps.length() - 1));
      env.put("SPARK_YARN_CACHE_ARCHIVES_FILE_SIZES", archiveSizes.substring(0,
              archiveSizes.length() - 1));
      env.put("SPARK_YARN_CACHE_ARCHIVES_VISIBILITIES", archiveVisibilities.
              substring(0,
                      archiveVisibilities.length() - 1));
    }
    ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
            launchContext.getLocalResources(), env, launchContext.getCommands(),
            null, null, null);
    appContext.setAMContainerSpec(amContainer);
  }

}
