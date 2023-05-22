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

import java.util.HashSet;
import java.util.Objects;

public class FlowInfo {
  private int chunkNumber;
  private int chunkSize;
  private int currentChunkSize;
  private String filename;
  private String identifier;
  private String relativePath;
  private int totalChunks;
  private long totalSize;
  private String filePath;
  private long uploadedContentLength;
  private HashSet<Integer> uploadedChunks = new HashSet<>();
  
  public FlowInfo(int chunkNumber, int chunkSize, int currentChunkSize, String filename, String identifier,
    String relativePath, int totalChunks, long totalSize) {
    this.chunkNumber = chunkNumber;
    this.chunkSize = chunkSize;
    this.currentChunkSize = currentChunkSize;
    this.filename = filename;
    this.identifier = identifier;
    this.relativePath = relativePath;
    this.totalChunks = totalChunks;
    this.totalSize = totalSize;
  }
  
  public FlowInfo(String chunkNumber, String chunkSize, String currentChunkSize, String filename, String identifier,
    String relativePath, String totalChunks, String totalSize) {
    this.chunkNumber = UploadUtils.toInt(chunkNumber, -1);
    this.chunkSize = UploadUtils.toInt(chunkSize, -1);
    this.currentChunkSize = UploadUtils.toInt(currentChunkSize, -1);
    this.filename = filename;
    this.identifier = identifier;
    this.relativePath = relativePath;
    this.totalChunks = UploadUtils.toInt(totalChunks, -1);
    this.totalSize = UploadUtils.toLong(totalSize, -1);
  }
  
  public boolean valid() {
    return chunkNumber >= 0 && totalSize >= 0 && !UploadUtils.isEmpty(identifier) && !UploadUtils.isEmpty(filename) &&
      !UploadUtils.isEmpty(relativePath);
  }
  
  public int getChunkNumber() {
    return chunkNumber;
  }
  
  public void setChunkNumber(int chunkNumber) {
    this.chunkNumber = chunkNumber;
  }
  
  public int getChunkSize() {
    return chunkSize;
  }
  
  public void setChunkSize(int chunkSize) {
    this.chunkSize = chunkSize;
  }
  
  public int getCurrentChunkSize() {
    return currentChunkSize;
  }
  
  public void setCurrentChunkSize(int currentChunkSize) {
    this.currentChunkSize = currentChunkSize;
  }
  
  public String getFilename() {
    return filename;
  }
  
  public void setFilename(String filename) {
    this.filename = filename;
  }
  
  public String getIdentifier() {
    return identifier;
  }
  
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
  
  public String getRelativePath() {
    return relativePath;
  }
  
  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }
  
  public int getTotalChunks() {
    return totalChunks;
  }
  
  public void setTotalChunks(int totalChunks) {
    this.totalChunks = totalChunks;
  }
  
  public long getTotalSize() {
    return totalSize;
  }
  
  public void setTotalSize(long totalSize) {
    this.totalSize = totalSize;
  }
  
  public String getFilePath() {
    return filePath;
  }
  
  public void setFilePath(String filePath) {
    this.filePath = filePath;
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
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FlowInfo flowInfo = (FlowInfo) o;
    return Objects.equals(identifier, flowInfo.identifier) &&
      Objects.equals(filePath, flowInfo.filePath);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(identifier, filePath);
  }
  
  @Override
  public String toString() {
    return "FlowInfo{" +
      "chunkNumber=" + chunkNumber +
      ", chunkSize=" + chunkSize +
      ", currentChunkSize=" + currentChunkSize +
      ", filename='" + filename + '\'' +
      ", identifier='" + identifier + '\'' +
      ", relativePath='" + relativePath + '\'' +
      ", totalChunks=" + totalChunks +
      ", totalSize=" + totalSize +
      '}';
  }
}
