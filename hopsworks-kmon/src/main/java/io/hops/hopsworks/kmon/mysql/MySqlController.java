/*
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
 *
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
