/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.python;

public enum CondaOp {
  CLONE,
  CREATE,
  BACKUP,
  REMOVE,
  LIST,
  INSTALL,
  UNINSTALL,
  UPGRADE,
  CLEAN,
  YML,
  EXPORT;
  
  public boolean isEnvOp() {
    return CondaOp.isEnvOp(this);
  }
  
  public static boolean isEnvOp(CondaOp arg) {
    if (arg.compareTo(CondaOp.CLONE) == 0 || arg.compareTo(CondaOp.CREATE) == 0 || arg.compareTo(CondaOp.YML) == 0 ||
      arg.compareTo(CondaOp.REMOVE) == 0 || arg.compareTo(CondaOp.BACKUP) == 0 || arg.compareTo(CondaOp.CLEAN) == 0
      || arg.compareTo(CondaOp.EXPORT) == 0) {
      return true;
    }
    return false;
  }
}
