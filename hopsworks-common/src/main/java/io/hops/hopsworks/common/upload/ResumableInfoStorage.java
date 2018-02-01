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

import java.util.HashMap;

public class ResumableInfoStorage {

  //Single instance
  private ResumableInfoStorage() {
  }
  private static ResumableInfoStorage sInstance;

  public static synchronized ResumableInfoStorage getInstance() {
    if (sInstance == null) {
      sInstance = new ResumableInfoStorage();
    }
    return sInstance;
  }

  //resumableIdentifier --  ResumableInfo
  private HashMap<String, ResumableInfo> mMap
          = new HashMap<String, ResumableInfo>();

  /**
   * Get ResumableInfo from mMap or Create a new one.
   * <p/>
   * @param resumableChunkSize
   * @param resumableTotalSize
   * @param resumableIdentifier
   * @param resumableFilename
   * @param resumableRelativePath
   * @param resumableFilePath
   * @param resumableTemplateId
   * @return
   */
  public synchronized ResumableInfo get(int resumableChunkSize,
          long resumableTotalSize,
          String resumableIdentifier, String resumableFilename,
          String resumableRelativePath, String resumableFilePath,
          int resumableTemplateId) {

    ResumableInfo info = mMap.get(resumableIdentifier);

    if (info == null) {
      info = new ResumableInfo();

      info.setResumableChunkSize(resumableChunkSize);
      info.setResumableTotalSize(resumableTotalSize);
      info.setResumableIdentifier(resumableIdentifier);
      info.setResumableFilename(resumableFilename);
      info.setResumableRelativePath(resumableRelativePath);
      info.setResumableFilePath(resumableFilePath);
      info.setResumableTemplateId(resumableTemplateId);

      mMap.put(resumableIdentifier, info);
    }
    return info;
  }

  /**
   * ResumableInfo
   * <p/>
   * @param info
   */
  public void remove(ResumableInfo info) {
    mMap.remove(info.getResumableIdentifier());
  }
}
