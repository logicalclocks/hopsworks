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

import io.hops.hopsworks.common.dao.pythonDeps.CondaCommands;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@ApiModel(value = "Conda commands for kagent to execute or report")
@XmlRootElement
public class CondaCommandView {
  
  private PythonDepsFacade.CondaOp op;
  private String user;
  @XmlElement(name = "proj")
  private String project;
  private Integer id;
  private String arg;
  private PythonDepsFacade.CondaStatus status;
  private String version;
  private String channelUrl;
  private PythonDepsFacade.CondaInstallType installType;
  private String lib;
  private String environmentYml;
  
  public CondaCommandView() {
  }
  
  private CondaCommandView(final Builder builder) {
    this.op = builder.op;
    this.user = builder.user;
    this.project = builder.project;
    this.id = builder.id;
    this.arg = builder.arg;
    this.status = builder.status;
    this.version = builder.version;
    this.channelUrl = builder.channelUrl;
    this.installType = builder.installType;
    this.lib = builder.lib;
    this.environmentYml = builder.environmentYml;
  }
  
  @ApiModelProperty(value = "Operation to be performed", required = true)
  public PythonDepsFacade.CondaOp getOp() {
    return op;
  }
  
  public void setOp(PythonDepsFacade.CondaOp op) {
    this.op = op;
  }
  
  @ApiModelProperty(value = "The user command will be executed")
  public String getUser() {
    return user;
  }
  
  public void setUser(String user) {
    this.user = user;
  }
  
  @ApiModelProperty(value = "Name of the project the command is associated with", required = true)
  public String getProject() {
    return project;
  }
  
  public void setProject(String project) {
    this.project = project;
  }
  
  @ApiModelProperty(value = "ID of command", required = true)
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  @ApiModelProperty(value = "Arguments passed to command")
  public String getArg() {
    return arg;
  }
  
  public void setArg(String arg) {
    this.arg = arg;
  }
  
  @ApiModelProperty(value = "Status of comamnd", required = true)
  public PythonDepsFacade.CondaStatus getStatus() {
    return status;
  }
  
  public void setStatus(PythonDepsFacade.CondaStatus status) {
    this.status = status;
  }
  
  @ApiModelProperty(value = "Python version to be enabled")
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
  
  @ApiModelProperty(value = "Pip channel to download a library if not th default")
  public String getChannelUrl() {
    return channelUrl;
  }
  
  public void setChannelUrl(String channelUrl) {
    this.channelUrl = channelUrl;
  }
  
  @ApiModelProperty(value = "Type of Conda installation")
  public PythonDepsFacade.CondaInstallType getInstallType() {
    return installType;
  }
  
  public void setInstallType(PythonDepsFacade.CondaInstallType installType) {
    this.installType = installType;
  }
  
  @ApiModelProperty(value = "Name of the library to install")
  public String getLib() {
    return lib;
  }
  
  public void setLib(String lib) {
    this.lib = lib;
  }
  
  @ApiModelProperty(value = "Environment exported as a YML file")
  public String getEnvironmentYml() {
    return environmentYml;
  }
  
  public void setEnvironmentYml(String environmentYml) {
    this.environmentYml = environmentYml;
  }
  
  public CondaCommands toCondaCommands() {
    final CondaCommands cc = new CondaCommands();
    cc.setOp(op);
    cc.setUser(user);
    cc.setProj(project);
    cc.setId(id);
    cc.setArg(arg);
    cc.setStatus(status);
    cc.setVersion(version);
    cc.setChannelUrl(channelUrl);
    cc.setInstallType(installType);
    cc.setLib(lib);
    cc.setEnvironmentYml(environmentYml);
    return cc;
  }
  
  public static class Builder {
    private PythonDepsFacade.CondaOp op;
    private String user;
    private String project;
    private Integer id;
    private String arg;
    private PythonDepsFacade.CondaStatus status;
    private String version;
    private String channelUrl;
    private PythonDepsFacade.CondaInstallType installType;
    private String lib;
    private String environmentYml;
    
    public Builder() {}
    
    public Builder setCondaOp(PythonDepsFacade.CondaOp op) {
      this.op = op;
      return this;
    }
    
    public Builder setUser(String user) {
      this.user = user;
      return this;
    }
    
    public Builder setProject(String project) {
      this.project = project;
      return this;
    }
    
    public Builder setCommandId(Integer id) {
      this.id = id;
      return this;
    }
    
    public Builder setArguments(String arguments) {
      this.arg = arguments;
      return this;
    }
    
    public Builder setStatus(PythonDepsFacade.CondaStatus status) {
      this.status = status;
      return this;
    }
    
    public Builder setVersion(String version) {
      this.version = version;
      return this;
    }
    
    public Builder setChannelUrl(String channelUrl) {
      this.channelUrl = channelUrl;
      return this;
    }
    
    public Builder setInstallType(PythonDepsFacade.CondaInstallType installType) {
      this.installType = installType;
      return this;
    }
    
    public Builder setLib(String lib) {
      this.lib = lib;
      return this;
    }
    
    public Builder setEnvironmentYml(String yml) {
      this.environmentYml = yml;
      return this;
    }
    
    public CondaCommandView build() {
      return new CondaCommandView(this);
    }
  
    public Builder reset() {
      this.op = null;
      this.user = null;
      this.project = null;
      this.id = null;
      this.arg = null;
      this.status = null;
      this.version = null;
      this.channelUrl = null;
      this.installType = null;
      this.lib = null;
      this.environmentYml = null;
    
      return this;
    }
  }
}
