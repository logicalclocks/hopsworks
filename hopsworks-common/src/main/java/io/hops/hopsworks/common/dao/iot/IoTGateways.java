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
  @NamedQuery(name = "IoTGateways.findAll",
    query = "SELECT i FROM IoTGateways i"),
  @NamedQuery(name = "IoTGateways.findByProject",
    query = "SELECT i FROM IoTGateways i WHERE i.project = :project"),
  @NamedQuery(name = "IoTGateways.findByProjectAndId",
    query = "SELECT i FROM IoTGateways i WHERE i.project = :project AND i.id = :id"),
  @NamedQuery(name = "IoTGateways.updateState",
    query = "UPDATE IoTGateways i SET i.state = :state WHERE i.id = :id")
  })
public class IoTGateways implements Serializable {

  @Id
  @Basic(optional = false)
  @Column(name = "gateway_id")
  private Integer id;

  @Column(name = "ip_address")
  @Basic(optional = false)
  @Size(max = 128)
  private String ipAddress;

  @Column(name = "port")
  @Basic(optional = false)
  private Integer port;
  
  @JoinColumn(name = "project_id",
    referencedColumnName = "id",
    insertable = false,
    updatable = false)
  @ManyToOne(optional = false)
  private Project project;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private GatewayState state;
  
  public IoTGateways() {
  }
  
  public IoTGateways(Integer id, String ipAddress, Integer port, GatewayState state) {
    this.id = id;
    this.ipAddress = ipAddress;
    this.port = port;
    this.state = state;
  }
  
  public IoTGateways(Integer id, String ipAddress, Integer port, Project project, GatewayState state) {
    this.id = id;
    this.ipAddress = ipAddress;
    this.port = port;
    this.project = project;
    this.state = state;
  }
  
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
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
  
  public GatewayState getState() {
    return state;
  }
  
  public void setState(GatewayState state) {
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
    IoTGateways that = (IoTGateways) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(ipAddress, that.ipAddress) &&
      Objects.equals(port, that.port) &&
      Objects.equals(project, that.project);
  }
  
  @Override
  public int hashCode() {
    
    return Objects.hash(id, ipAddress, port, project);
  }
}
