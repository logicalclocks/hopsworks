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

package io.hops.hopsworks.admin.maintenance;

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
