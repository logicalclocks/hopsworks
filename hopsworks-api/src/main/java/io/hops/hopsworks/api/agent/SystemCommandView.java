/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.agent;

import io.hops.hopsworks.common.dao.command.SystemCommand;
import io.hops.hopsworks.common.dao.command.SystemCommandFacade;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;

@ApiModel(value = "System commands for kagent to execute or report")
@XmlRootElement
public class SystemCommandView {
  private SystemCommandFacade.OP op;
  private SystemCommandFacade.STATUS status;
  private Integer id;
  private String arguments;
  private Integer priority;
  private String execUser;
  
  public SystemCommandView() {
  }
  
  private SystemCommandView(final Builder builder) {
    this.op = builder.op;
    this.status = builder.status;
    this.id = builder.id;
    this.arguments = builder.arguments;
    this.priority = builder.priority;
    this.execUser = builder.execUser;
  }
  
  @ApiModelProperty(value = "Operation to be performed", required = true)
  public SystemCommandFacade.OP getOp() {
    return op;
  }
  
  public void setOp(SystemCommandFacade.OP op) {
    this.op = op;
  }
  
  @ApiModelProperty(value = "Status of command", required = true)
  public SystemCommandFacade.STATUS getStatus() {
    return status;
  }
  
  public void setStatus(SystemCommandFacade.STATUS status) {
    this.status = status;
  }
  
  @ApiModelProperty(value = "ID of command", required = true)
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  @ApiModelProperty(value = "Arguments passed to command")
  public String getArguments() {
    return arguments;
  }
  
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }
  
  @ApiModelProperty(value = "Priority the command will run, 0 is the lowest priority")
  public Integer getPriority() {
    return priority;
  }
  
  public void setPriority(Integer priority) {
    this.priority = priority;
  }
  
  @ApiModelProperty(value = "The user command will be executed")
  public String getExecUser() {
    return execUser;
  }
  
  public void setExecUser(String execUser) {
    this.execUser = execUser;
  }
  
  public SystemCommand toSystemCommand() {
    final SystemCommand sc = new SystemCommand();
    sc.setOp(op);
    sc.setStatus(status);
    sc.setId(id);
    sc.setCommandArgumentsAsString(arguments);
    sc.setPriority(priority);
    sc.setExecUser(execUser);
    
    return sc;
  }
  
  public static class Builder {
    private SystemCommandFacade.OP op;
    private SystemCommandFacade.STATUS status;
    private Integer id;
    private String arguments;
    private Integer priority;
    private String execUser;
    
    public Builder() {}
    
    public Builder setOp(SystemCommandFacade.OP op) {
      this.op = op;
      return this;
    }
    
    public Builder setStatus(SystemCommandFacade.STATUS status) {
      this.status = status;
      return this;
    }
    
    public Builder setCommandId(Integer id) {
      this.id = id;
      return this;
    }
    
    public Builder setArguments(String arguments) {
      this.arguments = arguments;
      return this;
    }
    
    public Builder setPriority(Integer priority) {
      this.priority = priority;
      return this;
    }
    
    public Builder setUser(String user) {
      this.execUser = user;
      return this;
    }
    
    public SystemCommandView build() {
      return new SystemCommandView(this);
    }
    
    public Builder reset() {
      this.op = null;
      this.status = null;
      this.id = null;
      this.arguments = null;
      this.priority = null;
      this.execUser = null;
      return this;
    }
  }
}
