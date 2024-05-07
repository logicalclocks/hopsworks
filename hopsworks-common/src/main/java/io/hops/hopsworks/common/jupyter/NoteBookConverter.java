/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.jupyter;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.system.job.SystemJobStatus;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

public interface NoteBookConverter {

  default String getHtmlFromLog(String log) {
    if(!Strings.isNullOrEmpty(log)) {
      StringBuilder renderedNotebookSB = new StringBuilder(log);
      int startIndex = renderedNotebookSB.indexOf("<html");
      int stopIndex = renderedNotebookSB.length();
      return renderedNotebookSB.substring(startIndex, stopIndex);
    }
    return "";
  }
  SystemJobStatus convertIPythonNotebook(Project project, Users user, String notebookPath, String pyPath,
    NotebookConversion notebookConversion)  throws ServiceException;
}
