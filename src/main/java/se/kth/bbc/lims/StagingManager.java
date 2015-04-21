package se.kth.bbc.lims;

import com.google.common.io.Files;
import java.io.File;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;

/**
 * Basically provides a temporary folder in which to stage uploaded files.
 * <p>
 * @author stig
 */
@Singleton
public class StagingManager {

  private File stagingFolder;

  @PostConstruct
  public void init() {
    stagingFolder = Files.createTempDir();
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
