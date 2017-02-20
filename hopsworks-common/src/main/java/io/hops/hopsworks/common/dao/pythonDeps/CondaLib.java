package io.hops.hopsworks.common.dao.pythonDeps;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CondaLib {

  private String library;
  private List<String> versions;

  public CondaLib() {
  }

  public String getLibrary() {
    return library;
  }

  public void setLibrary(String library) {
    this.library = library;
  }

  public List<String> getVersions() {
    return versions;
  }

  public void setVersions(List<String> versions) {
    this.versions = versions;
  }


  @Override
  public boolean equals(Object o) {
    if (o instanceof CondaLib) {
      CondaLib pd = (CondaLib) o;
      if (pd.getLibrary().compareToIgnoreCase(this.library) == 0) {
        return true;
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    return (this.library.hashCode() * 7) / 2;
  }
}
