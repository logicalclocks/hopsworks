/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.pythonresources;

import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class DockerCgroupsMonitor {
  private static final Logger LOGGER = Logger.getLogger(DockerCgroupsMonitor.class.getName());
  private static Long memoryHardLimit = 9223372036854771712L;
  private static Long memorySoftLimit = 9223372036854771712L;
  private static Double cpuQuota = 100.0;
  private static Integer cpuPeriod = 100000;

  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @Resource
  private TimerService timerService;

  @PostConstruct
  public void init() {
    String rawInterval = settings.getDockerCgroupIntervalMonitor();
    Long intervalValue = settings.getConfTimeValue(rawInterval);
    TimeUnit intervalTimeunit = settings.getConfTimeTimeUnit(rawInterval);
    intervalValue = intervalTimeunit.toMillis(intervalValue);
    timerService.createIntervalTimer(0, intervalValue,
        new TimerConfig("Docker Cgroup Monitor", false));
  }

  @Timeout
  public void rotate(Timer timer) {
    if (settings.isDockerCgroupEnabled()) {
      LOGGER.log(Level.INFO, "Running DockerCgroupsMonitor");
      try {
        final Long dbDockerMemoryLimit = getLimitsLong(settings.getDockerCgroupMemoryLimit());
        final Long dbDockerCgroupSoftLimit = getLimitsLong(settings.getDockerCgroupSoftLimit());
        final Double dbDockerCgroupCpuQuota = settings.getDockerCgroupCpuQuota();
        final Integer dbDockerCgroupCpuPeriod = settings.getDockerCgroupCpuPeriod();
        if (dbDockerMemoryLimit != memoryHardLimit || dbDockerCgroupSoftLimit != memorySoftLimit
            || dbDockerCgroupCpuQuota != cpuQuota || dbDockerCgroupCpuPeriod != cpuPeriod) {
          memoryHardLimit = dbDockerMemoryLimit;
          memorySoftLimit = dbDockerCgroupSoftLimit;
          cpuQuota = dbDockerCgroupCpuQuota;
          cpuPeriod = dbDockerCgroupCpuPeriod;
          Long cpuQuotaValue =  Math.round((dbDockerCgroupCpuQuota/100) * cpuPeriod *
            Runtime.getRuntime().availableProcessors());
          //update the cgroups
          String prog = settings.getSudoersDir() + "/docker-cgroup-rewrite.sh";
          ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
              .addCommand("/usr/bin/sudo")
              .addCommand(prog)
              .addCommand(dbDockerMemoryLimit.toString())
              .addCommand(dbDockerCgroupSoftLimit.toString())
              .addCommand(cpuQuotaValue.toString())
              .addCommand(cpuPeriod.toString())
              .redirectErrorStream(true)
              .setWaitTimeout(60L, TimeUnit.SECONDS)
              .build();

          ProcessResult processResult = null;
          try {
            processResult = osProcessExecutor.execute(processDescriptor);
            if (processResult.getExitCode() != 0) {
              String errorMsg =
                  "Failed to update docker cgroup values. Exit code: " +
                      processResult.getExitCode() + " Error: stdout: " + processResult.getStdout() + " stderr: " +
                      processResult.getStderr();
              LOGGER.log(Level.SEVERE, errorMsg);
            } else {
              LOGGER.log(Level.INFO, "Successfully updated cgroup values");
            }
          } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to update docker cgroup values", e);
          }
        }
      } catch (NumberFormatException e) {
        LOGGER.log(Level.SEVERE, "Failed to update docker cgroup values. Could not convert one of the " +
            "provided values to number", e);
      }
    }
  }

  private Long getLimitsLong(String limit) throws NumberFormatException {
    return Long.parseLong(limit.replaceAll("[^0-9.]", "")) * 1073741824L;
  }
}
