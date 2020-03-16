/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.security.util;

import io.hops.hopsworks.persistence.entity.util.Variables;
import io.hops.hopsworks.security.dao.VariablesFacade;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.File;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SecuritySettings {
  
  private static final String VARIABLE_CERTS_DIRS = "certs_dir";
  private static final String VARIABLE_ADMIN_EMAIL = "admin_email";
  
  private String CERTS_DIR = "/srv/hops/certs-dir";
  private String ADMIN_EMAIL = "admin@hopsworks.ai";
  
  private boolean cached = false;
  
  @EJB
  private VariablesFacade variablesFacade;
  
  private String setDirVar(String varName, String defaultValue) {
    Variables dirName = variablesFacade.find(varName);
    if (dirName != null && dirName.getValue() != null && (new File(dirName.getValue()).isDirectory())) {
      String val = dirName.getValue();
      if (val != null && !val.isEmpty()) {
        return val;
      }
    }
    return defaultValue;
  }
  
  private String setVar(String varName, String defaultValue) {
    Variables var = variablesFacade.find(varName);
    if (var != null && var.getValue() != null && (!var.getValue().isEmpty())) {
      String val = var.getValue();
      if (val != null && !val.isEmpty()) {
        return val;
      }
    }
    return defaultValue;
  }
  
  private void populateCache() {
    if (!cached) {
      ADMIN_EMAIL = setVar(VARIABLE_ADMIN_EMAIL, ADMIN_EMAIL);
      CERTS_DIR = setDirVar(VARIABLE_CERTS_DIRS, CERTS_DIR);
    }
  }
  
  private void checkCache() {
    if (!cached) {
      populateCache();
    }
  }
  
  public synchronized void refreshCache() {
    cached = false;
    populateCache();
  }
  
  public synchronized String getCertsDir() {
    checkCache();
    return CERTS_DIR;
  }
  
  public synchronized String getHopsworksMasterEncPasswordFile() {
    return getCertsDir() + File.separator + "encryption_master_password";
  }
  
  public synchronized String getAdminEmail() {
    checkCache();
    return ADMIN_EMAIL;
  }
}
