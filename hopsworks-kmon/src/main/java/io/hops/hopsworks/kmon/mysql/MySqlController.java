/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.kmon.mysql;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;

@ManagedBean
@RequestScoped
public class MySqlController implements Serializable {

  private static final Logger logger = Logger.getLogger(MySqlController.class.
          getName());
  MySQLAccess mysql = new MySQLAccess();

  public MySqlController() {
    logger.info("MysqlController");
  }

  public StreamedContent getBackup() throws IOException, InterruptedException {
    StreamedContent content = mysql.getBackup();
    if (content == null) {
      FacesContext context = FacesContext.getCurrentInstance();
      context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
              "Backup Failed", "Backup failed. Tray again later."));
    }
    return content;
  }

  public void handleRestoreFileUpload(FileUploadEvent event) {
    FacesMessage msg;
    boolean result = mysql.restore(event.getFile());
    if (result) {
      msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
              "Data restored sucessfully.");
    } else {
      msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Failure",
              "Restore failed. Try again.");
    }
    FacesContext.getCurrentInstance().addMessage(null, msg);
  }

}
