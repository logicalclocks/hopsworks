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
package io.hops.hopsworks.admin.maintenance;

import io.hops.hopsworks.common.security.CertificatesMgmService;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LoggedMaintenanceHelper {
  private final Logger LOGGER = Logger.getLogger(HopsworksVariablesBean.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private CertificatesMgmService certificatesMgmService;
  
  
  public void updateVariable(String varName, String varValue, HttpServletRequest request) {
    settings.updateVariable(varName, varValue);
  }
  
  public void changeMasterEncryptionPassword(String currentPassword, String newPassword, HttpServletRequest request)
    throws IOException, EncryptionMasterPasswordException {
    String userEmail = request.getUserPrincipal().getName();
    certificatesMgmService.checkPassword(currentPassword, userEmail);
    Integer opId = certificatesMgmService.initUpdateOperation();
    certificatesMgmService.resetMasterEncryptionPassword(opId, newPassword, userEmail);
  }
}
