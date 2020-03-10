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
package io.hops.hopsworks.api.python.library;

import io.hops.hopsworks.api.python.command.CommandDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;
import io.hops.hopsworks.persistence.entity.python.MachineType;
import io.hops.hopsworks.persistence.entity.python.PythonDep;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LibraryDTO extends RestDTO<LibraryDTO> {

  private String channel;
  private PackageManager packageManager;
  private MachineType machine;
  private String library;
  private String version;
  private CondaStatus status;
  private String preinstalled;
  private CommandDTO commands;

  public LibraryDTO() {
  }
  
  public String getChannel() {
    return channel;
  }
  
  public void setChannel(String channel) {
    this.channel = channel;
  }
  
  public PackageManager getPackageManager() {
    return packageManager;
  }
  
  public void setPackageManager(PackageManager packageManager) {
    this.packageManager = packageManager;
  }
  
  public MachineType getMachine() {
    return machine;
  }
  
  public void setMachine(MachineType machine) {
    this.machine = machine;
  }
  
  public String getLibrary() {
    return library;
  }
  
  public void setLibrary(String library) {
    this.library = library;
  }
  
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
  
  public CondaStatus getStatus() {
    return status;
  }
  
  public void setStatus(CondaStatus status) {
    this.status = status;
  }
  
  public String getPreinstalled() {
    return preinstalled;
  }
  
  public void setPreinstalled(String preinstalled) {
    this.preinstalled = preinstalled;
  }
  
  public CommandDTO getCommands() {
    return commands;
  }
  
  public void setCommands(CommandDTO commands) {
    this.commands = commands;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof LibraryDTO) {
      LibraryDTO pd = (LibraryDTO) o;
      if (pd.getChannel().compareToIgnoreCase(this.channel) == 0
          && pd.getPackageManager().equals(this.packageManager)
          && pd.getLibrary().compareToIgnoreCase(this.library) == 0
          && pd.getVersion().compareToIgnoreCase(this.version) == 0
          && pd.getMachine().equals(this.machine)
          && pd.getPreinstalled().compareToIgnoreCase(this.preinstalled) == 0) {
        return true;
      }
    }
    if (o instanceof PythonDep) {
      PythonDep pd = (PythonDep) o;
      if (pd.getRepoUrl().getUrl().compareToIgnoreCase(this.channel) == 0
          && pd.getInstallType().name().equalsIgnoreCase(this.packageManager.name())
          && pd.getDependency().compareToIgnoreCase(this.library) == 0
          && pd.getVersion().compareToIgnoreCase(this.version) == 0
          && pd.getMachineType().equals(this.machine)
          && Boolean.toString(pd.isPreinstalled()).compareToIgnoreCase(this.preinstalled) == 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (this.channel.hashCode() / 3 + this.library.hashCode()
        + this.version.hashCode()) / 2;
  }

  public enum PackageManager {
    CONDA,
    PIP;
    
    public static PackageManager fromString(String param) {
      return valueOf(param.toUpperCase());
    }
    
  }
  
  @Override
  public String toString() {
    return "LibraryDTO{" +
      "channel='" + channel + '\'' +
      ", packageManager=" + packageManager +
      ", machine=" + machine +
      ", library='" + library + '\'' +
      ", version='" + version + '\'' +
      ", status=" + status +
      ", preinstalled='" + preinstalled + '\'' +
      ", commands=" + commands +
      '}';
  }
}
