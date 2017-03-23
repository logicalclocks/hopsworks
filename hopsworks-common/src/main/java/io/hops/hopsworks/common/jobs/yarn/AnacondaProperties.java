package io.hops.hopsworks.common.jobs.yarn;

/**
 *
 * <p>
 */
public class AnacondaProperties {

  private String envPath;

  public AnacondaProperties(String envPath) {
    this.envPath = envPath;
  }

  public String getEnvPath() {
    return envPath;
  }

  public void setEnvPath(String envPath) {
    this.envPath = envPath;
  }

}
