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
