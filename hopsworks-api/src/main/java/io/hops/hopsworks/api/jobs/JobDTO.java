package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class JobDTO extends RestDTO<Jobs, JobDTO> {
  
  private Integer id;
  private String name;
  private Date creationTime;
  private JobConfiguration config;
  private JobType type;
  private UserDTO creator;
  private ExecutionDTO executions;
  
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
  
  public Date getCreationTime() {
    return creationTime;
  }
  
  public void setCreationTime(Date creationTime) {
    this.creationTime = creationTime;
  }
  
  public JobConfiguration getConfig() {
    return config;
  }
  
  public void setConfig(JobConfiguration config) {
    this.config = config;
  }
  
  public JobType getType() {
    return type;
  }
  
  public void setType(JobType type) {
    this.type = type;
  }
  
  public UserDTO getCreator() {
    return creator;
  }
  
  public void setCreator(UserDTO creator) {
    this.creator = creator;
  }
  
  public ExecutionDTO getExecutions() {
    return executions;
  }
  
  public void setExecutions(ExecutionDTO executions) {
    this.executions = executions;
  }
  
}