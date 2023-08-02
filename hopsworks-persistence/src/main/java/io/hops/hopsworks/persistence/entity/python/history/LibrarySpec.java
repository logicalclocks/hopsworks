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

public class LibrarySpec implements Comparable<LibrarySpec> {
  private String library;
  private String version;
  private String installType;

  public LibrarySpec() {}

  public LibrarySpec(String library, String version, String installType) {
    this.library = library;
    this.version = version;
    this.installType = installType;
  }

  public String getLibrary() { return library; }

  public void setLibrary(String library) { this.library = library; }

  public String getVersion() { return version; }

  public void setVersion(String version) { this.version = version; }

  public String getInstallType() { return installType; }

  public void setInstallType(String installType) { this.installType = installType; }

  @Override
  public boolean equals(Object o) {
    if (o instanceof LibrarySpec == false) {
      return false;
    }
    LibrarySpec lib = (LibrarySpec) o;
    if (lib.getLibrary().equals(library)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return library.hashCode();
  }

  @Override
  public int compareTo(LibrarySpec lib) {
    return this.getLibrary().compareTo(lib.getLibrary());
  }
}
