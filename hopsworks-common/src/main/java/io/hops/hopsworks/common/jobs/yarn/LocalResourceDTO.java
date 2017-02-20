package io.hops.hopsworks.common.jobs.yarn;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class containing LocalResource properties required by YarnJobs.
 * <p>
 */
@XmlRootElement
public class LocalResourceDTO {

  private String name;
  private String path;
  private String visibility;
  private String type;
  //User provided pattern is used if the LocalResource is of type Pattern
  private String pattern;

  public LocalResourceDTO() {
  }

  public LocalResourceDTO(String name, String path, String visibility,
          String type, String pattern) {
    this.name = name;
    this.path = path;
    this.visibility = visibility;
    this.type = type;
    this.pattern = pattern;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public String toString() {
    return "LocalResourceDTO{" + "name=" + name + ", path=" + path
            + ", visibility=" + visibility + ", type=" + type + ", pattern="
            + pattern + '}';
  }

}
