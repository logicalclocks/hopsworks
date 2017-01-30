package io.hops.hopsworks.common.dao.pythonDeps;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PythonDepJson {

  private String channel;
  private String dependency;
  private String version;

  public PythonDepJson() {
  }

  public PythonDepJson(PythonDep pd) {
    this.channel = pd.getRepoUrl().getUrl();
    this.dependency = pd.getDependency();
    this.version = pd.getVersion();
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getDependency() {
    return dependency;
  }

  public void setDependency(String library) {
    this.dependency = library;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PythonDepJson) {
      PythonDepJson pd = (PythonDepJson) o;
      if (pd.getChannel().compareToIgnoreCase(this.channel) == 0
              && pd.getDependency().compareToIgnoreCase(this.dependency) == 0
              && pd.getVersion().compareToIgnoreCase(this.version) == 0) {
        return true;
      }
    }
    if (o instanceof PythonDep) {
      PythonDep pd = (PythonDep) o;
      if (pd.getRepoUrl().getUrl().compareToIgnoreCase(this.channel) == 0
              && pd.getDependency().compareToIgnoreCase(this.dependency) == 0
              && pd.getVersion().compareToIgnoreCase(this.version) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (this.channel.hashCode() / 3 + this.dependency.hashCode()
            + this.version.hashCode()) / 2;
  }
}
