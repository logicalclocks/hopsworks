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

package io.hops.hopsworks.common.python.updates;

import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.python.library.LibraryVersionDTO;
import io.hops.hopsworks.common.python.updates.analyzer.HopsLatestVersionAnalyzer;
import io.hops.hopsworks.common.python.updates.analyzer.HsfsLatestVersionAnalyzer;
import io.hops.hopsworks.common.python.updates.analyzer.LatestVersionAnalyzer;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import com.google.common.base.Strings;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LibraryUpdatesMonitor {

  @EJB
  private Settings settings;
  @Resource
  private TimerService timerService;
  @EJB
  private LibraryController libraryController;

  private final Map<String, LatestVersionAnalyzer> latestVersionAnalyzerMap = new ConcurrentHashMap<>();

  private static final Logger LOGGER = Logger.getLogger(
    LibraryUpdatesMonitor.class.getName());

  @PostConstruct
  public void init() {
    HopsLatestVersionAnalyzer hopsAnalyzer = new HopsLatestVersionAnalyzer();
    latestVersionAnalyzerMap.put(hopsAnalyzer.getLibrary(), hopsAnalyzer);

    HsfsLatestVersionAnalyzer hsfsAnalyzer = new HsfsLatestVersionAnalyzer();
    latestVersionAnalyzerMap.put(hsfsAnalyzer.getLibrary(), hsfsAnalyzer);

    String rawInterval = settings.getPyPiIndexerTimerInterval();
    Long intervalValue = settings.getConfTimeValue(rawInterval);
    TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);

    //600000 millis should be enough for PyPi search to get indexed as it runs on startup
    timerService.createIntervalTimer(600000, intervalTimeunit.toMillis(intervalValue),
      new TimerConfig("Python Library Updates Monitor", false));
  }

  @Timeout
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void execute(Timer timer) throws ServiceException {
    LOGGER.log(Level.INFO, "Running Python Library Updates Monitor");

    if(!libraryController.isPyPiIndexed()) {
      LOGGER.log(Level.WARNING, "PyPi search not indexed, skipping timer for now");
      return;
    }

    String hopsworksVersion = settings.getHopsworksVersion().replace("-SNAPSHOT", "");
    for(String library: latestVersionAnalyzerMap.keySet()) {
      HashMap<String, List<LibraryVersionDTO>> versions = libraryController.pipSearch(library);
      latestVersionAnalyzerMap.get(library).setLatestVersion(hopsworksVersion, versions);
    }
  }

  public String getLatestVersion(String libraryName) {
    String latestVersion = this.latestVersionAnalyzerMap.get(libraryName).getLatestVersion();
    if(!Strings.isNullOrEmpty(latestVersion)) {
      return latestVersion;
    } else {
      return "Latest version for this library is not available right now.";
    }
  }

  public Set<String> getMonitoredLibraries() {
    return this.latestVersionAnalyzerMap.keySet();
  }
}
