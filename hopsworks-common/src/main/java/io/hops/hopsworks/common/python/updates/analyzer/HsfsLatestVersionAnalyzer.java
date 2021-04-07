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

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/*
  hsfs version format:

  2.1.0 where the Hopsworks installation number is the first two digits separated by a dot.
  In this case the installation version is 2.1.0 for Hopsworks.
  The ending .0 indicates the bugfix version which in this case is 0.
  For each bugfix release the bugfix version is bumped by + 1, such as 2.1.0 -> 2.1.1 -> 2.1.2.
 */
public class HsfsLatestVersionAnalyzer extends LatestVersionAnalyzer {

  private static final Logger LOGGER = Logger.getLogger(
    HsfsLatestVersionAnalyzer.class.getName());

  @Override
  public String getLibrary() {
    return "hsfs";
  }


  @Override
  public void setLatestVersion(String currentHopsworksVersion, HashMap<String, List<LibraryVersionDTO>> versions) {

    if(versions.containsKey(this.getLibrary()) && !versions.get(this.getLibrary()).isEmpty()) {

      List<LibraryVersionDTO> searchVersionHits = versions.get(this.getLibrary());

      String [] hopsworksVersionSplit = currentHopsworksVersion.split("\\.");

      String hsfsVersionPrefix = hopsworksVersionSplit[0] + "." + hopsworksVersionSplit[1];

      List<LibraryVersionDTO> currentVersionSearchHits = searchVersionHits
        .stream().filter(version -> version.getVersion().startsWith(hsfsVersionPrefix))
        .collect(Collectors.toList());

      currentVersionSearchHits = currentVersionSearchHits
        .stream().sorted(new SortByVersionComparator()).collect(Collectors.toList());

      if(!currentVersionSearchHits.isEmpty()) {
        this.latestVersion = currentVersionSearchHits.get(currentVersionSearchHits.size() - 1).getVersion();
      } else {
        LOGGER.log(Level.SEVERE, "Could not find library version for " + this.getLibrary() +
          " for hopsworks version " + currentHopsworksVersion);
      }
    } else {
      LOGGER.log(Level.SEVERE, "Could not find library " + this.getLibrary() + " in PyPi search");
    }
  }
}
