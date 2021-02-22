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
package io.hops.hopsworks.api.python.environment;

import io.hops.hopsworks.api.python.command.CommandDTO;
import io.hops.hopsworks.api.python.conflicts.ConflictDTO;
import io.hops.hopsworks.api.python.library.LibraryDTO;
import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EnvironmentDTO extends RestDTO<EnvironmentDTO> {

  private String pythonVersion;
  private Boolean pythonConflicts;
  private Boolean pipSearchEnabled;
  private ConflictDTO conflicts;
  private String condaChannel;
  private LibraryDTO libraries;
  private CommandDTO commands;

  public String getPythonVersion() {
    return pythonVersion;
  }

  public void setPythonVersion(String pythonVersion) {
    this.pythonVersion = pythonVersion;
  }

  public LibraryDTO getLibraries() {
    return libraries;
  }

  public void setLibraries(LibraryDTO libraries) {
    this.libraries = libraries;
  }

  public String getCondaChannel() {
    return condaChannel;
  }

  public void setCondaChannel(String condaChannel) {
    this.condaChannel = condaChannel;
  }

  public CommandDTO getCommands() {
    return commands;
  }

  public void setCommands(CommandDTO commands) {
    this.commands = commands;
  }

  public Boolean getPythonConflicts() {
    return pythonConflicts;
  }

  public void setPythonConflicts(Boolean pythonConflicts) {
    this.pythonConflicts = pythonConflicts;
  }

  public ConflictDTO getConflicts() {
    return conflicts;
  }

  public void setConflicts(ConflictDTO conflicts) {
    this.conflicts = conflicts;
  }

  public Boolean getPipSearchEnabled() {
    return pipSearchEnabled;
  }

  public void setPipSearchEnabled(Boolean pipSearchEnabled) {
    this.pipSearchEnabled = pipSearchEnabled;
  }

  public enum Operation {
    CREATE,
    EXPORT;
  
    public static Operation fromString(String param) {
      return valueOf(param.toUpperCase());
    }
  }
}
