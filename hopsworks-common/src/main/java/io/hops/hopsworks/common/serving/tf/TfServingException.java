/*
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
 */

package io.hops.hopsworks.common.serving.tf;

import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.RESTException;

public class TfServingException extends RESTException {
  
  public TfServingException(RESTCodes.TfServingErrorCode code) {
    super(code);
  }
  
  public TfServingException(RESTCodes.TfServingErrorCode code, String usrMsg) {
    super(code, usrMsg);
  }
  
  public TfServingException(RESTCodes.TfServingErrorCode code, String usrMsg, String devMsg) {
    super(code, usrMsg, devMsg);
  }
  
  public TfServingException(RESTCodes.TfServingErrorCode code, String usrMsg, String devMsg, Throwable throwable) {
    super(code, usrMsg, devMsg, throwable);
  }
}
