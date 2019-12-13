package io.hops.hopsworks.exceptions;

import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;

import java.util.logging.Level;

public class ModelsException extends RESTException {

  public ModelsException(RESTCodes.ModelsErrorCode code, Level level) {
    super(code, level);
  }

  public ModelsException(RESTCodes.ModelsErrorCode code, Level level, String usrMsg) {
    super(code, level, usrMsg);
  }

  public ModelsException(RESTCodes.ModelsErrorCode code, Level level, String usrMsg, String devMsg) {
    super(code, level, usrMsg, devMsg);
  }

  public ModelsException(RESTCodes.ModelsErrorCode code, Level level, String usrMsg, String devMsg,
                         Throwable throwable) {
    super(code, level, usrMsg, devMsg, throwable);
  }

}
