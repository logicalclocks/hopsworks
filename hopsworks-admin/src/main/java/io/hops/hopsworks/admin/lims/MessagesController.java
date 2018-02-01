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

package io.hops.hopsworks.admin.lims;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import org.primefaces.context.RequestContext;

public class MessagesController {

  public static final String ERROR = "Error.";
  public static final String SUCCESS = "Success.";

  public static void addInfoMessage(String message) {
    addInfoMessage(message, message, null);
  }

  public static void addInfoMessage(String summary, String mess) {
    addInfoMessage(summary, mess, null);
  }

  public static void addInfoMessage(String summary, String mess, String anchor) {
    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary,
            mess);
    FacesContext.getCurrentInstance().addMessage(anchor, message);
  }

  public static void addErrorMessage(String message) {
    addErrorMessage("Error", message, null);
  }

  public static void addErrorMessage(String summary, String message) {
    addErrorMessage(summary, message, null);
  }

  public static void addErrorMessage(String summary, String message,
          String anchor) {
    FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR,
            summary, message);
    FacesContext.getCurrentInstance().addMessage(anchor, errorMessage);
  }

  public static void addMessage(FacesMessage message) {
    FacesContext.getCurrentInstance().addMessage(null, message);
  }

  public static void addWarnMessage(String summary, String mess) {
    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
            FacesMessage.SEVERITY_WARN, summary, mess));
  }

  public static void addSecurityErrorMessage(String message) {
    FacesContext context = FacesContext.getCurrentInstance();
    context.addMessage("messages", new FacesMessage(FacesMessage.SEVERITY_ERROR,
        message, "null"));

  }

  public static void addMessageToGrowl(String message) {

    RequestContext.getCurrentInstance().update("growl");
    FacesContext context = FacesContext.getCurrentInstance();
    context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
            message, null));
  }

}
