/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import javax.ejb.Stateless;
import javax.ws.rs.core.Response;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.rest.AppException;

/**
 *
 * @author Ermias
 */
@Stateless
public class ProjectNameValidator {

  public boolean isValidName(String projectName) throws AppException {
    if (projectName == null || projectName.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NAME_NOT_SET);
    }
    if (projectName.length() > 24) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NAME_TOO_LONG);
    }
    if (projectName.endsWith(".")) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NAME_ENDS_WITH_DOT);
    }
    for (char c : Constants.FILENAME_DISALLOWED_CHARS.toCharArray()) {
      if (projectName.contains("" + c)) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_NAME_CONTAIN_DISALLOWED_CHARS
                + Constants.FILENAME_DISALLOWED_CHARS);
      }
    }
    return true;
  }
}
