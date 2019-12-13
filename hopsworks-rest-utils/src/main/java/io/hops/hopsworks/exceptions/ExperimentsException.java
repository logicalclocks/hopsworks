package io.hops.hopsworks.exceptions;

import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;

import java.util.logging.Level;

public class ExperimentsException extends RESTException {

  public ExperimentsException(RESTCodes.ExperimentsErrorCode code, Level level) {
    super(code, level);
  }

  public ExperimentsException(RESTCodes.ExperimentsErrorCode code, Level level, String usrMsg) {
    super(code, level, usrMsg);
  }

  public ExperimentsException(RESTCodes.ExperimentsErrorCode code, Level level, String usrMsg, String devMsg) {
    super(code, level, usrMsg, devMsg);
  }

  public ExperimentsException(RESTCodes.ExperimentsErrorCode code, Level level, String usrMsg, String devMsg,
                         Throwable throwable) {
    super(code, level, usrMsg, devMsg, throwable);
  }

}
