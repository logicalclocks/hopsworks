/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.python.updates.analyzer;

import io.hops.hopsworks.common.python.library.LibraryVersionDTO;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public abstract class LatestVersionAnalyzer {

  protected String latestVersion;

  public String getLatestVersion() {
    return this.latestVersion;
  }

  public abstract String getLibrary();

  public abstract void setLatestVersion(String currentHopsworksVersion,
                                           HashMap<String, List<LibraryVersionDTO>> versions);

  protected class SortByVersionComparator implements Comparator<LibraryVersionDTO> {
    public int compare(LibraryVersionDTO a, LibraryVersionDTO b) {
      Integer aMinorVersion = new Integer(a.getVersion().substring(a.getVersion().lastIndexOf(".") + 1));
      Integer bMinorVersion = new Integer(b.getVersion().substring(b.getVersion().lastIndexOf(".") + 1));
      return aMinorVersion.compareTo(bMinorVersion);
    }
  }
}
