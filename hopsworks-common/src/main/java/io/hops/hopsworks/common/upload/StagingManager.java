/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.upload;

import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.flink.shaded.com.google.common.io.Files;

/**
 * Basically provides a temporary folder in which to stage uploaded files.
 */
@Singleton
@DependsOn("Settings")
public class StagingManager {

  private File stagingFolder;

  @EJB
  private Settings settings;

  @PostConstruct
  public void init() {
    String path = RandomStringUtils.randomAlphanumeric(8);
    stagingFolder = new File(settings.getStagingDir() + "/" + path);
    stagingFolder.mkdirs();
  }

  public String getStagingPath() {
    if (stagingFolder == null) {
      stagingFolder = Files.createTempDir();
    }
    return stagingFolder.getAbsolutePath();
  }

  @PreDestroy
  public void removeTmpDir() {
    if (stagingFolder != null) {
      stagingFolder.delete();
    }
  }
}
