/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.hdfs;

import org.apache.hadoop.fs.permission.FsPermission;

public final class FsPermissions {
  public static final FsPermission rwxrwxrwx  = FsPermission.createImmutable((short)00777);
  public static final FsPermission rwxrwx___  = FsPermission.createImmutable((short)00770);
  public static final FsPermission rwxrwx___T = FsPermission.createImmutable((short)01770);
  public static final FsPermission rwx______  = FsPermission.createImmutable((short)00700);
  public static final FsPermission rwxr_x___  = FsPermission.createImmutable((short)00750);
  public static final FsPermission r_xr_xr_x  = FsPermission.createImmutable((short)00555);
  public static final FsPermission rwxrwxr_x  = FsPermission.createImmutable((short)00775);
}
