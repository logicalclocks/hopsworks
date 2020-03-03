/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.admin.ndb;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.dao.ndb.NdbBackup;
import io.hops.hopsworks.common.dao.ndb.NdbBackupFacade;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.event.RowEditEvent;

@ManagedBean(name = "ndbBackupBean")
@SessionScoped
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class NdbBackupBean implements Serializable {

  private static final Logger logger = Logger.getLogger(
          NdbBackupBean.class.getName());

  @EJB
  private NdbBackupFacade ndbBackupFacade;
  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  public List<NdbBackup> allBackups = new ArrayList<>();

  public void setAllBackups(List<NdbBackup> allBackups) {
    this.allBackups = allBackups;
  }

  public List<NdbBackup> getAllBackups() {
    List<NdbBackup> all = ndbBackupFacade.findAll();
    if (all != null) {
      allBackups = all;
    }
    return allBackups;
  }

  public void onRowEdit(RowEditEvent event)
          throws IOException {

  }

  public void onRowCancel(RowEditEvent event) {
  }

  public String remove(Integer backupId) {

    String prog = settings.getSudoersDir() + "/bin/ndb_backup.sh";
    int exitValue;

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand("-u")
        .addCommand(settings.getMysqlUser())
        .addCommand(prog)
        .addCommand("remove")
        .addCommand(backupId.toString())
        .setWaitTimeout(2L, TimeUnit.HOURS)
        .ignoreOutErrStreams(true)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (!processResult.processExited()) {
        logger.log(Level.SEVERE, "Removing NDB backup time-out");
        return "RESTORE_FAILED";
      }
      
      exitValue = processResult.getExitCode();
      if (exitValue == 0) {
        ndbBackupFacade.removeBackup(backupId);        
      }
    } catch (IOException ex) {

      logger.log(Level.SEVERE, "Problem removing the backup: {0}", ex.toString());
      return "RESTORE_FAILED";
    }
    return exitValue == 0 ? "RESTORE_OK" : "RESTORE_FAILED";
  }

  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public String startBackup() {

    String prog = settings.getSudoersDir() + "/bin/ndb_backup.sh";
    int exitValue = -1;

    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
        .addCommand("/usr/bin/sudo")
        .addCommand("-u")
        .addCommand(settings.getMysqlUser())
        .addCommand(prog)
        .addCommand("backup")
        .setWaitTimeout(2L, TimeUnit.HOURS)
        .ignoreOutErrStreams(true)
        .build();

    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (!processResult.processExited()) {
        logger.log(Level.SEVERE, "Removing NDB backup time-out");
        return "BACKUP_FAILED";
      }

      exitValue = processResult.getExitCode();
      Pattern r = Pattern.compile("(.*)Backup (\\d+)(.*)");
      if (exitValue == 0) {
        Matcher m = r.matcher(processResult.getStdout());
        if (m.find()) {
          int id = Integer.parseInt(m.group(2));
          NdbBackup newBackup = new NdbBackup(id);
          ndbBackupFacade.persistBackup(newBackup);
          return "BACKUP_OK";
        }
      }

      return "BACKUP_FAILED";
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Problem starting a backup: {0}", e.toString());
      MessagesController.addSecurityErrorMessage("Backup failed: " + exitValue);
      return "BACKUP_FAILED";
    }
  }
}
