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
package io.hops.hopsworks.api.dataset.inode;

import io.hops.hopsworks.persistence.entity.command.CommandStatus;
import io.hops.hopsworks.persistence.entity.hdfs.command.Command;

/**
 * Created for backward compatibility
 */
public enum ZipState {
  STAGING,
  ZIPPING,
  UNZIPPING,
  FAILED,
  NONE;

  public static ZipState fromCommandStatus(CommandStatus status, Command command) {
    // Command can be used for other commands than zip/unzip
    if (!Command.COMPRESS.equals(command) && !Command.EXTRACT.equals(command)) {
      return NONE;
    }
    switch (status) {
      case ONGOING:
        return Command.COMPRESS.equals(command) ? ZIPPING : UNZIPPING;
      case FAILED:
        return FAILED;
      case NEW:
        return STAGING;
      default:
        return NONE;
    }
  }
}
