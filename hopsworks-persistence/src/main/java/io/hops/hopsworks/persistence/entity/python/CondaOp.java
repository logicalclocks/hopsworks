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
  CREATE,
  REMOVE,
  INSTALL,
  UNINSTALL,
  EXPORT,
  IMPORT,
  SYNC_BASE_ENV;

  public static boolean isEnvOp(CondaOp op) {
    return op.compareTo(CondaOp.CREATE) == 0 ||
           op.compareTo(CondaOp.REMOVE) == 0 ||
           op.compareTo(CondaOp.EXPORT) == 0 ||
           op.compareTo(CondaOp.IMPORT) == 0 ||
           op.compareTo(CondaOp.SYNC_BASE_ENV) == 0;
  }

  public static boolean isLibraryOp(CondaOp op) {
    return op.compareTo(CondaOp.INSTALL) == 0 ||
           op.compareTo(CondaOp.UNINSTALL) == 0;
  }
}
