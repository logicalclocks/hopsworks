/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.dataset;

import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * User settable permissions for Datasets.
 * 
 */
@XmlType
@XmlEnum
public enum DatasetPermissions {
  GROUP_WRITABLE_SB("GROUP_WRITABLE_SB", "rwxrwx--T", "1770"),
  GROUP_WRITABLE("GROUP_WRITABLE", "rwxrwx---", "770"),
  OWNER_ONLY("OWNER_ONLY", "rwxr-x---", "750");
  
  @XmlEnumValue(value = "displayName")
  private final String displayName;
  @XmlEnumValue(value = "permissionStr")
  private final String permissionStr;
  @XmlEnumValue(value = "permission")
  private final String permission;
  
  DatasetPermissions(String displayName, String permissionStr, String permission) {
    this.displayName = displayName;
    this.permissionStr = permissionStr;
    this.permission = permission;
  }
  
  public String getDisplayName() {
    return displayName;
  }
  
  public String getPermissionStr() {
    return permissionStr;
  }
  
  public String getPermission() {
    return permission;
  }
  
  public static DatasetPermissions fromPermissionString(String permissionStr) {
    switch (permissionStr) {
      case "rwxrwx--T":
        return DatasetPermissions.GROUP_WRITABLE_SB;
      case "rwxrwx---":
        return DatasetPermissions.GROUP_WRITABLE;
      case "rwxr-x---":
        return DatasetPermissions.OWNER_ONLY;
      default:
        throw new IllegalArgumentException("Dataset permission has no value with the specified permission string.");
    }
  }
  
  public static DatasetPermissions fromPermission(String permission) {
    switch (permission) {
      case "1770":
        return DatasetPermissions.GROUP_WRITABLE_SB;
      case "770":
        return DatasetPermissions.GROUP_WRITABLE;
      case "750":
        return DatasetPermissions.OWNER_ONLY;
      default:
        throw new IllegalArgumentException("Dataset permission has no value with the specified permission.");
    }
  }
  
  public static DatasetPermissions fromString(String displayName) {
    return valueOf(displayName.toUpperCase());
  }
  
  public static DatasetPermissions fromFilePermissions(FsPermission fsPermission) {
    if (fsPermission.equals(new FsPermission(FsAction.ALL, FsAction.READ_EXECUTE, FsAction.NONE, false))) {
      return DatasetPermissions.OWNER_ONLY;
    } else if (fsPermission.equals(new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.NONE, true))) {
      return DatasetPermissions.GROUP_WRITABLE_SB;
    } else if (fsPermission.equals(new FsPermission(FsAction.ALL, FsAction.ALL, FsAction.NONE, false))) {
      return DatasetPermissions.GROUP_WRITABLE;
    } else {
      throw new IllegalArgumentException("Dataset permission has no value with the specified FsPermission.");
    }
  }
  
  public FsPermission toFsPermission() {
    return new FsPermission(this.permission);
  }
  
}
