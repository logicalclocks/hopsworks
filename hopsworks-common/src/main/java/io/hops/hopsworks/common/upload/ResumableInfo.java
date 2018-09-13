/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
