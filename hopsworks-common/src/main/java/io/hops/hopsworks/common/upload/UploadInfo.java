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
package io.hops.hopsworks.common.upload;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

public class UploadInfo implements Serializable {
  private static final long serialVersionUID = 1313991374949049442L;

  private long resumableTotalSize;
  private long uploadedContentLength;
  private Date lastWrite;
  private HashSet<Integer> uploadedChunks = new HashSet<>();

  public UploadInfo() {
  }

  public UploadInfo(long resumableTotalSize) {
    this.resumableTotalSize = resumableTotalSize;
    this.lastWrite = new Date();
  }

  public long getResumableTotalSize() {
    return resumableTotalSize;
  }

  public void setResumableTotalSize(long resumableTotalSize) {
    this.resumableTotalSize = resumableTotalSize;
  }

  public long getUploadedContentLength() {
    return uploadedContentLength;
  }

  public void setUploadedContentLength(long uploadedContentLength) {
    this.uploadedContentLength = uploadedContentLength;
  }

  public HashSet<Integer> getUploadedChunks() {
    return uploadedChunks;
  }

  public void setUploadedChunks(HashSet<Integer> uploadedChunks) {
    this.uploadedChunks = uploadedChunks;
  }

  public boolean checkIfUploadFinished() {
    return uploadedContentLength == resumableTotalSize;
  }

  public Date getLastWrite() {
    return lastWrite;
  }

  public void setLastWrite(Date lastWrite) {
    this.lastWrite = lastWrite;
  }

  /**
   * Add the chunk <i>rcn</i> to the uploaded chunks and check if upload has finished.
   * <p/>
   * @return true if finished.
   */
  public boolean addChunkAndCheckIfFinished(int rcn, long contentLength) {
    if (uploadedChunks == null) {
      uploadedChunks = new HashSet<>();
    }
    if (!uploadedChunks.contains(rcn)) {
      uploadedContentLength += contentLength;
    }
    uploadedChunks.add(rcn);
    lastWrite = new Date();
    return checkIfUploadFinished();
  }

  @Override
  public String toString() {
    return "UploadInfo{" +
      "serialVersionUID=" + serialVersionUID +
      ", resumableTotalSize=" + resumableTotalSize +
      ", uploadedContentLength=" + uploadedContentLength +
      ", uploadedChunks=" + uploadedChunks +
      '}';
  }
}