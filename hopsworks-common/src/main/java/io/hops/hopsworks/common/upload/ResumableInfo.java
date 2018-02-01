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

import java.io.File;
import java.util.HashSet;

public class ResumableInfo {

  private int resumableChunkSize;
  private long resumableTotalSize;
  private String resumableIdentifier;
  private String resumableFilename;
  private String resumableRelativePath;
  private int resumableTemplateId;
  private long uploadedContentLength = 0;
  private String resumableFilePath;

  //Chunks uploaded. Private to enable atomically add and check if finished
  private HashSet<ResumableChunkNumber> uploadedChunks = new HashSet<>();

  public static class ResumableChunkNumber {

    public ResumableChunkNumber(int number) {
      this.number = number;
    }

    public int number;

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ResumableChunkNumber
              ? ((ResumableChunkNumber) obj).number == this.number : false;
    }

    @Override
    public int hashCode() {
      return number;
    }
  }

  public boolean valid() {
    if (resumableChunkSize < 0 || resumableTotalSize < 0
            || HttpUtils.isEmpty(resumableIdentifier)
            || HttpUtils.isEmpty(resumableFilename)
            || HttpUtils.isEmpty(resumableRelativePath)) {
      return false;
    } else {
      return true;
    }
  }

  private boolean checkIfUploadFinished() {
    if (uploadedContentLength != resumableTotalSize) {
      return false;
    }

    //Upload finished, change filename.
    File file = new File(resumableFilePath);
    String new_path = file.getAbsolutePath().substring(0,
            file.getAbsolutePath().length() - ".temp".length());
    file.renameTo(new File(new_path));
    return true;
  }

  /**
   * Add the chunk <i>rcn</i> to the uploaded chunks and check if upload
   * has finished. Upon upload, change file name. Synchronized method to enable
   * atomic checking.
   * <p/>
   * @return true if finished.
   */
  public synchronized boolean addChunkAndCheckIfFinished(
          ResumableChunkNumber rcn, long contentLength) {
    if (!uploadedChunks.contains(rcn)) {
      uploadedContentLength += contentLength;
    }
    uploadedChunks.add(rcn);
    return checkIfUploadFinished();
  }

  /**
   * Check if the resumable chunk has been uploaded.
   * <p/>
   * @param rcn
   * @return
   */
  public boolean isUploaded(ResumableChunkNumber rcn) {
    return uploadedChunks.contains(rcn);
  }

  /*
   * getters
   */
  public int getResumableChunkSize() {
    return this.resumableChunkSize;
  }

  public long getResumableTotalSize() {
    return this.resumableTotalSize;
  }

  public String getResumableIdentifier() {
    return this.resumableIdentifier;
  }

  public String getResumableFilename() {
    return this.resumableFilename;
  }

  public String getResumableRelativePath() {
    return this.resumableRelativePath;
  }

  public int getResumableTemplateId() {
    return this.resumableTemplateId;
  }

  public long getUploadedContentLength() {
    return this.uploadedContentLength;
  }

  public String getResumableFilePath() {
    return this.resumableFilePath;
  }

  /*
   * setters
   */
  public void setResumableChunkSize(int resumableChunkSize) {
    this.resumableChunkSize = resumableChunkSize;
  }

  public void setResumableTotalSize(long resumableTotalSize) {
    this.resumableTotalSize = resumableTotalSize;
  }

  public void setResumableIdentifier(String resumableIdentifier) {
    this.resumableIdentifier = resumableIdentifier;
  }

  public void setResumableFilename(String resumableFilename) {
    this.resumableFilename = resumableFilename;
  }

  public void setResumableRelativePath(String resumableRelativePath) {
    this.resumableRelativePath = resumableRelativePath;
  }

  public void setResumableTemplateId(int resumableTemplateId) {
    this.resumableTemplateId = resumableTemplateId;
  }

  public void setUploadedContentLength(long uploadedContentLength) {
    this.uploadedContentLength = uploadedContentLength;
  }

  public void setResumableFilePath(String resumableFilePath) {
    this.resumableFilePath = resumableFilePath;
  }

}
