/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.json.datavalidation;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DataValidationSettingsDTO {
  private String validationRulesPath;
  private String executablePath;
  private String executableMainClass;
  
  public DataValidationSettingsDTO() {}
  
  public String getValidationRulesPath() {
    return validationRulesPath;
  }
  
  public void setValidationRulesPath(String validationRulesPath) {
    this.validationRulesPath = validationRulesPath;
  }
  
  public String getExecutablePath() {
    return executablePath;
  }
  
  public void setExecutablePath(String executablePath) {
    this.executablePath = executablePath;
  }
  
  public String getExecutableMainClass() {
    return executableMainClass;
  }
  
  public void setExecutableMainClass(String executableMainClass) {
    this.executableMainClass = executableMainClass;
  }
  
  @Override
  public String toString() {
    return "Rules path: " + validationRulesPath + " - exec path: " + executablePath
        + " - main class: " + executableMainClass;
  }
}
