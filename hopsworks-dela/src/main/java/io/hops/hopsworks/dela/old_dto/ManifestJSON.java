/*
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
 *
 */

package io.hops.hopsworks.dela.old_dto;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ManifestJSON {

  private String datasetName;

  private String datasetDescription;

  private String creatorEmail;

  private String creatorDate;

  private boolean kafkaSupport;

  private List<FileInfo> fileInfos;

  private List<String> metaDataJsons;

  public ManifestJSON() {
  }

  public ManifestJSON(String datasetName, String datasetDescription,
          String creatorEmail, String creatorDate, boolean kafkaSupport,
          List<FileInfo> fileInfos, List<String> metaDataJsons) {
    this.datasetName = datasetName;
    this.datasetDescription = datasetDescription;
    this.creatorEmail = creatorEmail;
    this.creatorDate = creatorDate;
    this.kafkaSupport = kafkaSupport;
    this.fileInfos = fileInfos;
    this.metaDataJsons = metaDataJsons;
  }

  public List<FileInfo> getFileInfos() {
    return fileInfos;
  }

  public void setFileInfos(List<FileInfo> fileInfos) {
    this.fileInfos = fileInfos;
  }

  public List<String> getMetaDataJsons() {
    return metaDataJsons;
  }

  public void setMetaDataJsons(List<String> metaDataJsons) {
    this.metaDataJsons = metaDataJsons;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getDatasetDescription() {
    return datasetDescription;
  }

  public void setDatasetDescription(String datasetDescription) {
    this.datasetDescription = datasetDescription;
  }

  public String getCreatorEmail() {
    return creatorEmail;
  }

  public void setCreatorEmail(String creatorEmail) {
    this.creatorEmail = creatorEmail;
  }

  public String getCreatorDate() {
    return creatorDate;
  }

  public void setCreatorDate(String creatorDate) {
    this.creatorDate = creatorDate;
  }

  public boolean isKafkaSupport() {
    return kafkaSupport;
  }

  public void setKafkaSupport(boolean kafkaSupport) {
    this.kafkaSupport = kafkaSupport;
  }

}
