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
public class FolderNameValidator {

  public boolean isValidName(String name) throws AppException {
    if (name == null || name.isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.FOLDER_NAME_NOT_SET);
    }
    if (name.length() > 24) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.FOLDER_NAME_TOO_LONG);
    }
    if (name.endsWith(".")) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.FOLDER_NAME_ENDS_WITH_DOT);
    }
    for (char c : Constants.FILENAME_DISALLOWED_CHARS.toCharArray()) {
      if (name.contains("" + c)) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.FOLDER_NAME_CONTAIN_DISALLOWED_CHARS
                + Constants.FILENAME_DISALLOWED_CHARS);
      }
    }
    return true;
  }
}
