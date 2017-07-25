package io.hops.hopsworks.common.upload;

import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.flink.shaded.com.google.common.io.Files;

/**
 * Basically provides a temporary folder in which to stage uploaded files.
 */
@Singleton
public class StagingManager {

  private File stagingFolder;

  @EJB
  private Settings settings;

  @PostConstruct
  public void init() {
    String path = RandomStringUtils.randomAlphanumeric(8);
    stagingFolder = new File(settings.getStagingDir() + "/" + path);
    stagingFolder.mkdirs();
  }

  public String getStagingPath() {
    if (stagingFolder == null) {
      stagingFolder = Files.createTempDir();
    }
    return stagingFolder.getAbsolutePath();
  }

  @PreDestroy
  public void removeTmpDir() {
    if (stagingFolder != null) {
      stagingFolder.delete();
    }
  }
}
