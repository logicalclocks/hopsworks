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
package io.hops.hopsworks.persistence.entity.dataset;

public enum PermissionTransition {
  EDITABLE_TO_EDITABLE_BY_OWNERS("EDITABLE_TO_EDITABLE_BY_OWNERS", DatasetAccessPermission.EDITABLE,
    DatasetAccessPermission.EDITABLE_BY_OWNERS),
  EDITABLE_TO_READ_ONLY("EDITABLE_TO_READ_ONLY", DatasetAccessPermission.EDITABLE, DatasetAccessPermission.READ_ONLY),
  EDITABLE_TO_EDITABLE("EDITABLE_TO_EDITABLE", DatasetAccessPermission.EDITABLE, DatasetAccessPermission.EDITABLE),
  EDITABLE_BY_OWNERS_TO_EDITABLE("EDITABLE_BY_OWNERS_TO_EDITABLE", DatasetAccessPermission.EDITABLE_BY_OWNERS,
    DatasetAccessPermission.EDITABLE),
  EDITABLE_BY_OWNERS_TO_READ_ONLY("EDITABLE_BY_OWNERS_TO_READ_ONLY", DatasetAccessPermission.EDITABLE_BY_OWNERS,
    DatasetAccessPermission.READ_ONLY),
  EDITABLE_BY_OWNERS_TO_EDITABLE_BY_OWNERS("EDITABLE_BY_OWNERS_TO_EDITABLE_BY_OWNERS",
    DatasetAccessPermission.EDITABLE_BY_OWNERS, DatasetAccessPermission.EDITABLE_BY_OWNERS),
  READ_ONLY_TO_EDITABLE("READ_ONLY_TO_EDITABLE", DatasetAccessPermission.READ_ONLY, DatasetAccessPermission.EDITABLE),
  READ_ONLY_TO_EDITABLE_BY_OWNERS("READ_ONLY_TO_EDITABLE_BY_OWNERS", DatasetAccessPermission.READ_ONLY,
    DatasetAccessPermission.EDITABLE_BY_OWNERS),
  READ_ONLY_TO_READ_ONLY("READ_ONLY_TO_READ_ONLY", DatasetAccessPermission.READ_ONLY,
    DatasetAccessPermission.READ_ONLY);

  private final String value;
  private final DatasetAccessPermission from;
  private final DatasetAccessPermission to;
  PermissionTransition(String value, DatasetAccessPermission from, DatasetAccessPermission to) {
    this.value = value;
    this.from = from;
    this.to = to;
  }

  public static PermissionTransition valueOf(DatasetAccessPermission from, DatasetAccessPermission to) {
    return PermissionTransition.valueOf(from.name() + "_TO_" + to.name());
  }

  public boolean noop() {
    return PermissionTransition.EDITABLE_TO_EDITABLE.equals(this) ||
      PermissionTransition.EDITABLE_BY_OWNERS_TO_EDITABLE_BY_OWNERS.equals(this) ||
      PermissionTransition.READ_ONLY_TO_READ_ONLY.equals(this);
  }

  public DatasetAccessPermission getFrom() {
    return from;
  }

  public DatasetAccessPermission getTo() {
    return to;
  }
}
