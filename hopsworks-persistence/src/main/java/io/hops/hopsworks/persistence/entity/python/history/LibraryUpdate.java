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
package io.hops.hopsworks.persistence.entity.python.history;

public class LibraryUpdate {
  private String library;
  private String oldVersion;
  private String currentVersion;

  public LibraryUpdate() {}

  public String getLibrary() { return library; }

  public void setLibrary(String library) { this.library = library; }

  public LibraryUpdate(String library, String old, String current) {
    this.library = library;
    this.oldVersion = old;
    this.currentVersion = current;
  }

  public String getOldVersion() { return oldVersion; }

  public void setOldVersion(String oldVersion) { this.oldVersion = oldVersion; }

  public String getCurrentVersion() { return currentVersion; }

  public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }
}
