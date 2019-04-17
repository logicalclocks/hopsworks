package io.hops.hopsworks.common.dao.iot;

import io.hops.hopsworks.common.dao.project.Project;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "hopsworks.gateways")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "IotGateways.findAll",
    query = "SELECT i FROM IotGateways i"),
  @NamedQuery(name = "IotGateways.findByProject",
    query = "SELECT i FROM IotGateways i WHERE i.project = :project"),
  @NamedQuery(name = "IotGateways.findByProjectAndId",
    query = "SELECT i FROM IotGateways i WHERE i.project = :project AND i.id = :id"),
  @NamedQuery(name = "IotGateways.updateState",
    query = "UPDATE IotGateways i SET i.state = :state WHERE i.id = :id")
  })
public class IotGateways implements Serializable {

  @Id
  @Basic(optional = false)
  @Column(name = "gateway_id")
  private Integer id;

  @Column(name = "hostname")
  @Basic(optional = false)
  @Size(max = 128)
  private String hostname;

  @Column(name = "port")
  @Basic(optional = false)
  private Integer port;
  
  @JoinColumn(name = "project_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private IotGatewayState state;
  
  public IotGateways() {
  }
  
  public IotGateways(Integer id, String hostname, Integer port, IotGatewayState state) {
    this.id = id;
    this.hostname = hostname;
    this.port = port;
    this.state = state;
  }
  
  public IotGateways(Integer id, String hostname, Integer port, Project project, IotGatewayState state) {
    this.id = id;
    this.hostname = hostname;
    this.port = port;
    this.project = project;
    this.state = state;
  }
  
  public IotGateways(IotGatewayConfiguration config, Project project, IotGatewayState state) {
    this.id = config.getGatewayId();
    this.hostname = config.getHostname();
    this.port = config.getPort();
    this.project = project;
    this.state = state;
  }
  
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
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
  
  public IotGatewayState getState() {
    return state;
  }
  
  public void setState(IotGatewayState state) {
    this.state = state;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IotGateways that = (IotGateways) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(hostname, that.hostname) &&
      Objects.equals(port, that.port) &&
      Objects.equals(project, that.project);
  }
  
  @Override
  public int hashCode() {
    
    return Objects.hash(id, hostname, port, project);
  }
}
