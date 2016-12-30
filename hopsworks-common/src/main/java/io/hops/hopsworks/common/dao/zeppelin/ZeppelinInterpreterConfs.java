
package io.hops.hopsworks.common.dao.zeppelin;

import io.hops.hopsworks.common.dao.project.Project;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "hopsworks.zeppelin_interpreter_confs")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ZeppelinInterpreterConfs.findAll",
          query
          = "SELECT z FROM ZeppelinInterpreterConfs z"),
  @NamedQuery(name = "ZeppelinInterpreterConfs.findByProjectName",
          query
          = "SELECT z FROM ZeppelinInterpreterConfs z WHERE z.projectName = :projectName"),
  @NamedQuery(name = "ZeppelinInterpreterConfs.findByLastUpdate",
          query
          = "SELECT z FROM ZeppelinInterpreterConfs z WHERE z.lastUpdate = :lastUpdate"),
  @NamedQuery(name = "ZeppelinInterpreterConfs.findByIntrepeterConf",
          query
          = "SELECT z FROM ZeppelinInterpreterConfs z WHERE z.intrepeterConf = :intrepeterConf")})
public class ZeppelinInterpreterConfs implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 100)
  @Column(name = "project_name")
  private String projectName;
  @Basic(optional = false)
  @NotNull
  @Column(name = "lastUpdate")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastUpdate;
  @Column(name = "interpreter_conf")
  private String intrepeterConf;
  @JoinColumn(name = "project_name",
          referencedColumnName = "projectname",
          insertable
          = false,
          updatable = false)
  @OneToOne(optional = false)
  private Project project;

  public ZeppelinInterpreterConfs() {
  }

  public ZeppelinInterpreterConfs(String projectName) {
    this.projectName = projectName;
  }

  public ZeppelinInterpreterConfs(String projectName, Date lastUpdate) {
    this.projectName = projectName;
    this.lastUpdate = lastUpdate;
  }

  public ZeppelinInterpreterConfs(String projectName, Date lastUpdate,
          String intrepeterConf) {
    this.projectName = projectName;
    this.lastUpdate = lastUpdate;
    this.intrepeterConf = intrepeterConf;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(Date lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  public String getIntrepeterConf() {
    return new String(intrepeterConf);
  }

  public void setIntrepeterConf(String intrepeterConf) {
    this.intrepeterConf = intrepeterConf;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (projectName != null ? projectName.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ZeppelinInterpreterConfs)) {
      return false;
    }
    ZeppelinInterpreterConfs other = (ZeppelinInterpreterConfs) object;
    if ((this.projectName == null && other.projectName != null)
            || (this.projectName != null && !this.projectName.equals(
                    other.projectName))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "se.kth.hopsworks.zeppelin.util.ZeppelinInterpreterConfs[ projectName="
            + projectName + " ]";
  }

}
