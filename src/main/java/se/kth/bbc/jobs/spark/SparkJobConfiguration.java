package se.kth.bbc.jobs.spark;

import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;

/**
 * Contains Spark-specific run information for a Spark job, on top of Yarn
 * configuration.
 * <p>
 * @author stig
 */
@XmlRootElement
public class SparkJobConfiguration extends YarnJobConfiguration {

  private String jarPath;
  private String mainClass;
  private String args;

  public String getJarPath() {
    return jarPath;
  }

  /**
   * Set the path to the main executable jar. No default value.
   * <p>
   * @param jarPath
   */
  public void setJarPath(String jarPath) {
    this.jarPath = jarPath;
  }

  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set the name of the main class to be executed. No default value.
   * <p>
   * @param mainClass
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArgs() {
    return args;
  }

  /**
   * Set the arguments to be passed to the job. No default value.
   * <p>
   * @param args
   */
  public void setArgs(String args) {
    this.args = args;
  }

}
