/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.commands;

import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;

import java.util.logging.Level;

public class CommandException extends RESTException {
  public CommandException(RESTCodes.CommandErrorCode code, Level level) {
    super(code, level);
  }
  
  public CommandException(RESTCodes.CommandErrorCode code, Level level, String usrMsg) {
    super(code, level, usrMsg);
  }
  
  public CommandException(RESTCodes.CommandErrorCode code, Level level, String usrMsg, String devMsg) {
    super(code, level, usrMsg, devMsg);
  }
  
  public CommandException(RESTCodes.CommandErrorCode code, Level level, String usrMsg, String devMsg,
                             Throwable throwable) {
    super(code, level, usrMsg, devMsg, throwable);
  }
}
