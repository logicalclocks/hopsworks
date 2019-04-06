package io.hops.hopsworks.common.dao.iot;

import io.hops.hopsworks.common.dao.project.Project;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "hopsworks.iot_gateways")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "IoTGateways.findAll",
    query = "SELECT i FROM IoTGateways i")
  })
public class IoTGateways implements Serializable {

  @Id
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Column(name = "name")
  @Basic(optional = false)
  @Size(max = 128)
  private String name;

  @Column(name = "ip_address")
  @Basic(optional = false)
  @Size(max = 128)
  private String ipAddress;

  @Column(name = "port")
  @Basic(optional = false)
  private Integer port;

  @JoinColumn(name = "project_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IoTGateways that = (IoTGateways) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {

    return Objects.hash(name);
  }
}
